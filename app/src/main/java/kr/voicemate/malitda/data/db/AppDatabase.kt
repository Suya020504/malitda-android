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
        MeaningEntity::class, MeaningExpressionEntity::class, TrialLogEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun expressionDao(): ExpressionDao
    abstract fun correctionDao(): CorrectionDao
    abstract fun counterDao(): CounterDao
    abstract fun meaningDao(): MeaningDao
    abstract fun trialLogDao(): TrialLogDao

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

        /** SQLCipher로 암호화된 로컬 DB. 키는 Android Keystore가 감싼 무작위 32바이트. */
        fun build(context: Context, passphrase: ByteArray): AppDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
