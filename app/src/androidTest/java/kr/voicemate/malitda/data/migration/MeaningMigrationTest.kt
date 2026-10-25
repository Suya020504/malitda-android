package kr.voicemate.malitda.data.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kr.voicemate.malitda.data.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 기존 버전과 v5의 비파괴 마이그레이션을 검증한다.
 */
@RunWith(AndroidJUnit4::class)
class MeaningMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrationPreservesAllFourExistingTablesAndMatchesRoomSchema() {
        helper.createDatabase("meaning-migration-preservation", 1).apply {
            execSQL("INSERT INTO profiles (id, name, createdAt) VALUES (1, '기존 사용자', 100)")
            execSQL(
                "INSERT INTO expressions (id, profileId, category, label, text, favorite, createdAt, updatedAt) " +
                    "VALUES (11, 1, 'daily', '물', '물을 주세요', 1, 101, 102)"
            )
            execSQL(
                "INSERT INTO corrections (id, profileId, sourceKey, sourceRaw, approvedText, createdAt, lastUsedAt, useCount) " +
                    "VALUES (21, 1, '물을 주새요', '물을 주새요!', '물을 주세요', 103, 104, 7)"
            )
            execSQL(
                "INSERT INTO counters (profileId, approvals, shares, expressionsAdded, lastActiveDay, streakDays, mainCharacter, friendsSeen) " +
                    "VALUES (1, 8, 3, 2, '2026-10-04', 4, 'friend', 5)"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            "meaning-migration-preservation", 4, true,
            AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4,
        )
        migrated.query("SELECT id, name, createdAt FROM profiles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals("기존 사용자", cursor.getString(1))
            assertEquals(100L, cursor.getLong(2))
            assertEquals(1, cursor.count)
        }
        migrated.query("SELECT text, favorite FROM expressions WHERE id = 11").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("물을 주세요", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
        }
        migrated.query("SELECT sourceKey, approvedText, useCount FROM corrections WHERE id = 21").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("물을 주새요", cursor.getString(0))
            assertEquals("물을 주세요", cursor.getString(1))
            assertEquals(7, cursor.getInt(2))
        }
        migrated.query("SELECT approvals, streakDays, mainCharacter FROM counters WHERE profileId = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(8, cursor.getInt(0))
            assertEquals(4, cursor.getInt(1))
            assertEquals("friend", cursor.getString(2))
        }
        assertEquals(0L, count(migrated, "meanings"))
        assertEquals(0L, count(migrated, "meaning_expressions"))
    }

    @Test
    fun migratedTablesAcceptMeaningAndExpressionRows() {
        helper.createDatabase("meaning-migration-insert", 1).apply {
            execSQL("INSERT INTO profiles (id, name, createdAt) VALUES (1, '학생', 100)")
            close()
        }
        val migrated = helper.runMigrationsAndValidate(
            "meaning-migration-insert", 2, true, AppDatabase.MIGRATION_1_2,
        )
        migrated.execSQL(
            "INSERT INTO meanings (id, profileId, text, imageKey, situation, confirmed, active, createdAt, updatedAt, useCount) " +
                "VALUES (1, 1, '산책하고 싶어요', 'walk', NULL, 0, 1, 101, 101, 0)"
        )
        migrated.execSQL(
            "INSERT INTO meaning_expressions (profileId, meaningId, text, key) VALUES (1, 1, '바람', '바람')"
        )
        assertEquals(1L, count(migrated, "meanings"))
        assertEquals(1L, count(migrated, "meaning_expressions"))
        // (profileId, key) 중복 등록은 유니크 인덱스가 막는다(REPLACE 전략으로 교체됨).
        migrated.execSQL(
            "INSERT OR REPLACE INTO meaning_expressions (profileId, meaningId, text, key) VALUES (1, 1, '바람', '바람')"
        )
        assertEquals(1L, count(migrated, "meaning_expressions"))
    }

    @Test
    fun migration2to3KeepsMeaningsAndAddsTrialLogs() {
        helper.createDatabase("trial-migration", 2).apply {
            execSQL("INSERT INTO profiles (id, name, createdAt) VALUES (1, '학생', 100)")
            execSQL(
                "INSERT INTO meanings (id, profileId, text, imageKey, situation, confirmed, active, createdAt, updatedAt, useCount) " +
                    "VALUES (1, 1, '산책하고 싶어요', 'walk', NULL, 1, 1, 101, 101, 3)"
            )
            execSQL("INSERT INTO meaning_expressions (profileId, meaningId, text, key) VALUES (1, 1, '바람', '바람')")
            close()
        }
        val migrated = helper.runMigrationsAndValidate("trial-migration", 3, true, AppDatabase.MIGRATION_2_3)
        assertEquals(1L, count(migrated, "meanings"))
        assertEquals(1L, count(migrated, "meaning_expressions"))
        assertEquals(0L, count(migrated, "trial_logs"))
        migrated.execSQL(
            "INSERT INTO trial_logs (profileId, startedAt, endedAt, origin, voiceFirstHit, usedCardFallback, " +
                "otherMeaningCount, delivered, understood, supporterHelped, meaningId, meaningText) " +
                "VALUES (1, 0, 5000, 'VOICE', 1, 0, 0, 1, 1, 0, 1, '산책하고 싶어요')"
        )
        assertEquals(1L, count(migrated, "trial_logs"))
    }

    @Test
    fun migration3to4PreservesAmbiguousOldMeaningAndTrialLogs() {
        helper.createDatabase("sample-provenance-migration", 3).apply {
            execSQL("INSERT INTO profiles (id, name, createdAt) VALUES (1, '학생', 100)")
            // 기존 예시와 문장·그림이 같아도 개인 기록일 수 있다. 출처를 추정해서 표시하거나 지우지 않는다.
            execSQL(
                "INSERT INTO meanings (id, profileId, text, imageKey, situation, confirmed, active, createdAt, updatedAt, useCount) " +
                    "VALUES (1, 1, '산책하고 싶어요', 'walk', '직접 등록', 1, 1, 101, 102, 3)"
            )
            execSQL("INSERT INTO meaning_expressions (profileId, meaningId, text, key) VALUES (1, 1, '나의 산책', '나의 산책')")
            execSQL(
                "INSERT INTO trial_logs (profileId, startedAt, endedAt, origin, voiceFirstHit, usedCardFallback, " +
                    "otherMeaningCount, delivered, understood, supporterHelped, meaningId, meaningText) " +
                    "VALUES (1, 200, 400, 'VOICE', 1, 0, 0, 1, 1, 0, 1, '산책하고 싶어요')"
            )
            close()
        }
        val migrated = helper.runMigrationsAndValidate("sample-provenance-migration", 4, true, AppDatabase.MIGRATION_3_4)
        migrated.query(
            "SELECT text, imageKey, situation, confirmed, useCount, isSample, confirmationMethod, confirmationResponse, confirmedAt " +
                "FROM meanings WHERE id = 1"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("산책하고 싶어요", cursor.getString(0))
            assertEquals("walk", cursor.getString(1))
            assertEquals("직접 등록", cursor.getString(2))
            assertEquals(1, cursor.getInt(3))
            assertEquals(3, cursor.getInt(4))
            assertEquals(0, cursor.getInt(5))
            assertTrue(cursor.isNull(6) && cursor.isNull(7) && cursor.isNull(8))
        }
        assertEquals(1L, count(migrated, "meaning_expressions"))
        assertEquals(1L, count(migrated, "trial_logs"))
    }

    @Test
    fun migration4to5PreservesDataAllowsContextMeaningsAndKeepsOldTrialsPrivate() {
        helper.createDatabase("personalization-migration", 4).apply {
            execSQL("INSERT INTO profiles (id, name, createdAt) VALUES (1, '기존 사용자', 100)")
            execSQL("INSERT INTO meanings (id, profileId, text, imageKey, situation, confirmed, active, createdAt, updatedAt, useCount, isSample) VALUES (1, 1, '산책', NULL, '점심 후', 1, 1, 101, 102, 3, 0)")
            execSQL("INSERT INTO meaning_expressions (id, profileId, meaningId, text, key) VALUES (1, 1, 1, '바람', '바람')")
            execSQL("INSERT INTO trial_logs (profileId, startedAt, endedAt, origin, voiceFirstHit, usedCardFallback, otherMeaningCount, delivered, understood, supporterHelped, meaningId, meaningText) VALUES (1, 200, 400, 'VOICE', 1, 0, 0, 1, 1, 0, 1, '이전 뜻 문장')")
            close()
        }
        val migrated = helper.runMigrationsAndValidate("personalization-migration", 5, true, AppDatabase.MIGRATION_4_5)
        assertEquals(1L, count(migrated, "meanings"))
        migrated.execSQL("INSERT INTO meanings (id, profileId, text, imageKey, situation, confirmed, active, createdAt, updatedAt, useCount, isSample) VALUES (2, 1, '추워요', NULL, '야외', 1, 1, 103, 103, 0, 0)")
        migrated.execSQL("INSERT INTO meaning_expressions (profileId, meaningId, text, key) VALUES (1, 2, '바람', '바람')")
        assertEquals(2L, count(migrated, "meaning_expressions"))
        migrated.query("SELECT practice, consented, participantCode, meaningText FROM trial_logs").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
            assertEquals(0, c.getInt(1))
            assertEquals("", c.getString(2))
            assertTrue(c.isNull(3))
        }
        assertEquals(0L, count(migrated, "confirmation_events"))
    }

    private fun count(database: SupportSQLiteDatabase, table: String): Long =
        database.query("SELECT COUNT(*) FROM $table").use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }
}
