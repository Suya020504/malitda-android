package kr.voicemate.malitda.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        ProfileEntity::class, ExpressionEntity::class, CorrectionEntity::class, CounterEntity::class,
        MeaningEntity::class, MeaningExpressionEntity::class, TrialLogEntity::class, ConfirmationEventEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun expressionDao(): ExpressionDao
    abstract fun correctionDao(): CorrectionDao
    abstract fun counterDao(): CounterDao
    abstract fun meaningDao(): MeaningDao
    abstract fun trialLogDao(): TrialLogDao
    abstract fun dataManagementDao(): DataManagementDao

    companion object {
        const val NAME = "malitda.db"

        /** v1 → v2: 개인 의미 사전(meanings·meaning_expressions)만 추가. 기존 데이터는 그대로 보존한다. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `meanings` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, " +
                        "`text` TEXT NOT NULL, `imageKey` TEXT, `situation` TEXT, " +
                        "`confirmed` INTEGER NOT NULL, `active` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `useCount` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meanings_profileId` ON `meanings` (`profileId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `meaning_expressions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, " +
                        "`meaningId` INTEGER NOT NULL, `text` TEXT NOT NULL, `key` TEXT NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_meaning_expressions_profileId_key` ON `meaning_expressions` (`profileId`, `key`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meaning_expressions_meaningId` ON `meaning_expressions` (`meaningId`)")
            }
        }

        /** v2 → v3: 실증 기록(trial_logs)만 추가. 기존 데이터는 그대로 보존한다. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                convertKnownLegacyMeaningsIfNeeded(db)
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trial_logs` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, " +
                        "`startedAt` INTEGER NOT NULL, `endedAt` INTEGER NOT NULL, `origin` TEXT NOT NULL, " +
                        "`voiceFirstHit` INTEGER NOT NULL, `usedCardFallback` INTEGER NOT NULL, " +
                        "`otherMeaningCount` INTEGER NOT NULL, `delivered` INTEGER NOT NULL, " +
                        "`understood` INTEGER NOT NULL, `supporterHelped` INTEGER NOT NULL, " +
                        "`meaningId` INTEGER, `meaningText` TEXT)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trial_logs_profileId` ON `trial_logs` (`profileId`)")
            }
        }

        /**
         * v3 → v4: 예시 출처와 학생 확인 근거를 추가한다.
         * 기존 문장·그림만으로 예시를 추정하지 않는다. 출처를 알 수 없는 기록은 모두 보존한다.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                convertKnownLegacyMeaningsIfNeeded(db)
                addColumnIfMissing(db, "meanings", "isSample INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "meanings", "confirmationMethod TEXT")
                addColumnIfMissing(db, "meanings", "confirmationResponse TEXT")
                addColumnIfMissing(db, "meanings", "confirmedAt INTEGER")
            }
        }

        /** v4 → v5: 복수 뜻·확인 이력·익명 실증 조건. 이전 실증은 동의/연습을 추정하지 않는다. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                convertKnownLegacyMeaningsIfNeeded(db)
                db.execSQL("DROP INDEX IF EXISTS `index_meaning_expressions_profileId_key`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_meaning_expressions_profileId_key_meaningId` ON `meaning_expressions` (`profileId`, `key`, `meaningId`)")
                addColumnIfMissing(db, "meanings", "category TEXT NOT NULL DEFAULT 'often'")
                addColumnIfMissing(db, "meanings", "favorite INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "meanings", "lastUsedAt INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "meanings", "legacyVersion INTEGER DEFAULT NULL")
                addColumnIfMissing(db, "meanings", "legacyConfirmedBy TEXT DEFAULT NULL")
                // 과거 실증 문장은 새 익명 기록 정책에 맞춰 지우고 결과 수치는 보존한다.
                db.execSQL("UPDATE `trial_logs` SET `meaningText` = NULL")
                val fields = listOf(
                    "participantCode TEXT NOT NULL DEFAULT ''", "condition TEXT NOT NULL DEFAULT 'C'",
                    "taskCode TEXT NOT NULL DEFAULT ''", "targetMeaningId INTEGER DEFAULT NULL",
                    "outcome TEXT NOT NULL DEFAULT 'COMPLETED'", "practice INTEGER NOT NULL DEFAULT 1",
                    "consented INTEGER NOT NULL DEFAULT 0", "deviceContext TEXT NOT NULL DEFAULT ''",
                    "qualitativeNote TEXT NOT NULL DEFAULT ''", "judgedCorrect INTEGER NOT NULL DEFAULT -1",
                    "retryCount INTEGER NOT NULL DEFAULT 0", "editCount INTEGER NOT NULL DEFAULT 0",
                    "stopReason TEXT NOT NULL DEFAULT ''", "accidentalSendCount INTEGER NOT NULL DEFAULT 0",
                    "taskSetVersion TEXT NOT NULL DEFAULT 'local-v1'",
                )
                fields.forEach { field ->
                    addColumnIfMissing(db, "trial_logs", field)
                }
                db.execSQL("CREATE TABLE IF NOT EXISTS `confirmation_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `meaningId` INTEGER NOT NULL, `meaningVersion` INTEGER NOT NULL, `response` TEXT NOT NULL, `method` TEXT, `confirmedBy` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_confirmation_events_profileId` ON `confirmation_events` (`profileId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_confirmation_events_meaningId` ON `confirmation_events` (`meaningId`)")
            }
        }

        private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
            db.query("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table)).use { it.moveToFirst() }

        private fun columns(db: SupportSQLiteDatabase, table: String): Map<String, String> =
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                buildMap {
                    while (cursor.moveToNext()) {
                        put(cursor.getString(cursor.getColumnIndexOrThrow("name")), cursor.getString(cursor.getColumnIndexOrThrow("type")).uppercase())
                    }
                }
            }

        private fun addColumnIfMissing(db: SupportSQLiteDatabase, table: String, field: String) {
            val name = field.substringBefore(' ')
            if (name !in columns(db, table)) db.execSQL("ALTER TABLE `$table` ADD COLUMN `$name` ${field.substringAfter(' ')}")
        }

        private fun count(db: SupportSQLiteDatabase, query: String): Long =
            db.query(query).use { cursor -> check(cursor.moveToFirst()); cursor.getLong(0) }

        /**
         * 같은 DB 버전을 썼던 10월 개인 의미 사전 분기만 비파괴 이관한다.
         * 알려진 두 표와 열 형태를 모두 확인한다. 다른 형태를 발견하면 원본을 지우지 않고 실패한다.
         */
        private fun convertKnownLegacyMeaningsIfNeeded(db: SupportSQLiteDatabase) {
            val canonical = tableExists(db, "meanings")
            val legacyMeaning = tableExists(db, "meaning_entries")
            val legacyAlias = tableExists(db, "expression_aliases")
            if (canonical) {
                check(!legacyMeaning && !legacyAlias) { "서로 다른 의미 사전 형식이 함께 있습니다. 원본을 보존한 채 확인해야 합니다." }
                return
            }
            check(legacyMeaning && legacyAlias && !tableExists(db, "meaning_expressions")) {
                "알 수 없는 의미 사전 형식입니다. 원본을 보존한 채 확인해야 합니다."
            }
            val allowedTables = setOf("profiles", "expressions", "corrections", "counters", "meaning_entries", "expression_aliases", "trial_logs", "room_master_table", "android_metadata")
            val knownTablesOnly = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").use { cursor ->
                var known = true
                while (cursor.moveToNext()) if (cursor.getString(0) !in allowedTables) known = false
                known
            }
            check(knownTablesOnly) { "알 수 없는 이전 자료 표가 있습니다. 원본을 보존한 채 확인해야 합니다." }
            val expectedMeanings = mapOf(
                "id" to "INTEGER", "profileId" to "INTEGER", "displayText" to "TEXT", "imageRef" to "TEXT",
                "contextLabel" to "TEXT", "status" to "TEXT", "version" to "INTEGER", "confirmationMethod" to "TEXT",
                "confirmedBy" to "TEXT", "confirmedAt" to "INTEGER", "createdAt" to "INTEGER", "updatedAt" to "INTEGER",
            )
            val expectedAliases = mapOf("id" to "INTEGER", "profileId" to "INTEGER", "meaningId" to "INTEGER", "rawAlias" to "TEXT", "normalizedAlias" to "TEXT")
            check(columns(db, "meaning_entries") == expectedMeanings && columns(db, "expression_aliases") == expectedAliases) {
                "알려진 이전 의미 사전과 열 구성이 다릅니다. 원본을 보존한 채 확인해야 합니다."
            }
            check(count(db, "SELECT COUNT(*) FROM meaning_entries WHERE status IS NULL OR status NOT IN ('DRAFT', 'CONFIRMED', 'INACTIVE')") == 0L) {
                "이전 의미 사전에 알 수 없는 상태가 있습니다. 원본을 보존한 채 확인해야 합니다."
            }
            check(count(db, "SELECT COUNT(*) FROM meaning_entries e LEFT JOIN profiles p ON p.id=e.profileId WHERE p.id IS NULL") == 0L &&
                count(db, "SELECT COUNT(*) FROM expression_aliases a LEFT JOIN meaning_entries e ON e.id=a.meaningId AND e.profileId=a.profileId WHERE e.id IS NULL") == 0L) {
                "이전 의미 사전의 사용자 연결을 확인해야 합니다. 원본은 보존됩니다."
            }
            val meaningCount = count(db, "SELECT COUNT(*) FROM meaning_entries")
            val aliasCount = count(db, "SELECT COUNT(*) FROM expression_aliases")
            MIGRATION_1_2.migrate(db)
            // 이전 분기는 같은 별칭의 복수 뜻을 허용했다. 임시 구형 단일 뜻 인덱스를 먼저 제거한다.
            db.execSQL("DROP INDEX IF EXISTS `index_meaning_expressions_profileId_key`")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_meaning_expressions_profileId_key_meaningId` ON `meaning_expressions` (`profileId`, `key`, `meaningId`)")
            listOf(
                "isSample INTEGER NOT NULL DEFAULT 0", "confirmationMethod TEXT", "confirmationResponse TEXT", "confirmedAt INTEGER",
                "category TEXT NOT NULL DEFAULT 'often'", "favorite INTEGER NOT NULL DEFAULT 0", "lastUsedAt INTEGER NOT NULL DEFAULT 0",
                "legacyVersion INTEGER DEFAULT NULL", "legacyConfirmedBy TEXT DEFAULT NULL",
            ).forEach { addColumnIfMissing(db, "meanings", it) }
            db.execSQL("INSERT INTO meanings (id, profileId, text, imageKey, situation, confirmed, active, createdAt, updatedAt, useCount, isSample, confirmationMethod, confirmationResponse, confirmedAt, category, favorite, lastUsedAt, legacyVersion, legacyConfirmedBy) SELECT id, profileId, displayText, imageRef, contextLabel, CASE WHEN status='CONFIRMED' THEN 1 ELSE 0 END, CASE WHEN status='INACTIVE' THEN 0 ELSE 1 END, createdAt, updatedAt, 0, 0, confirmationMethod, CASE WHEN status='CONFIRMED' THEN 'matched' ELSE NULL END, confirmedAt, 'often', 0, 0, version, confirmedBy FROM meaning_entries")
            db.execSQL("INSERT INTO meaning_expressions (id, profileId, meaningId, text, key) SELECT id, profileId, meaningId, rawAlias, normalizedAlias FROM expression_aliases")
            db.execSQL("CREATE TABLE IF NOT EXISTS `confirmation_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `meaningId` INTEGER NOT NULL, `meaningVersion` INTEGER NOT NULL, `response` TEXT NOT NULL, `method` TEXT, `confirmedBy` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_confirmation_events_profileId` ON `confirmation_events` (`profileId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_confirmation_events_meaningId` ON `confirmation_events` (`meaningId`)")
            db.execSQL("INSERT INTO confirmation_events (profileId, meaningId, meaningVersion, response, method, confirmedBy, createdAt) SELECT profileId, id, version, 'matched', confirmationMethod, COALESCE(confirmedBy, 'legacy_unknown'), COALESCE(confirmedAt, updatedAt) FROM meaning_entries WHERE status='CONFIRMED'")
            check(count(db, "SELECT COUNT(*) FROM meanings") == meaningCount && count(db, "SELECT COUNT(*) FROM meaning_expressions") == aliasCount)
            // NULL과 빈 문자열도 구분해서 검사한다. 확인 기록이 없던 DRAFT는 확인됨으로 바꾸지 않는다.
            check(count(db, "SELECT COUNT(*) FROM meaning_entries e JOIN meanings m ON m.id=e.id WHERE NOT (m.profileId IS e.profileId AND m.text IS e.displayText AND m.imageKey IS e.imageRef AND m.situation IS e.contextLabel AND m.createdAt IS e.createdAt AND m.updatedAt IS e.updatedAt AND m.legacyVersion IS e.version AND m.legacyConfirmedBy IS e.confirmedBy AND m.confirmationMethod IS e.confirmationMethod AND m.confirmedAt IS e.confirmedAt)") == 0L)
            check(count(db, "SELECT COUNT(*) FROM expression_aliases a JOIN meaning_expressions e ON e.id=a.id WHERE NOT (e.profileId IS a.profileId AND e.meaningId IS a.meaningId AND e.text IS a.rawAlias AND e.key IS a.normalizedAlias)") == 0L)
            db.execSQL("DROP TABLE expression_aliases")
            db.execSQL("DROP TABLE meaning_entries")
            // 이 분기의 version=3은 실증 테이블을 뜻하지 않았다. 알려진 의미 자료 이관 뒤 빈 실증 표만 준비한다.
            db.execSQL("CREATE TABLE IF NOT EXISTS `trial_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER NOT NULL, `origin` TEXT NOT NULL, `voiceFirstHit` INTEGER NOT NULL, `usedCardFallback` INTEGER NOT NULL, `otherMeaningCount` INTEGER NOT NULL, `delivered` INTEGER NOT NULL, `understood` INTEGER NOT NULL, `supporterHelped` INTEGER NOT NULL, `meaningId` INTEGER, `meaningText` TEXT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_trial_logs_profileId` ON `trial_logs` (`profileId`)")
        }

        /** SQLCipher로 암호화된 로컬 DB. 키는 Android Keystore가 감싼 무작위 32바이트. */
        fun build(context: Context, passphrase: ByteArray): AppDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
        }
    }
}
