package kr.voicemate.malitda.data.migration

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kr.voicemate.malitda.data.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeaningMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrationPreservesAllFourExistingTablesAndMatchesRoomSchema() {
        helper.createDatabase("meaning-migration-preservation", 1).apply {
            execSQL("INSERT INTO profiles (id, name, createdAt) VALUES (1, '기존 사용자', 100)")
            execSQL("INSERT INTO expressions (id, profileId, category, label, text, favorite, createdAt, updatedAt) VALUES (11, 1, 'daily', '물', '물을 주세요', 1, 101, 102)")
            execSQL("INSERT INTO corrections (id, profileId, sourceKey, sourceRaw, approvedText, createdAt, lastUsedAt, useCount) VALUES (21, 1, '물을 주새요', '물을 주새요!', '물을 주세요', 103, 104, 7)")
            execSQL("INSERT INTO counters (profileId, approvals, shares, expressionsAdded, lastActiveDay, streakDays, mainCharacter, friendsSeen) VALUES (1, 8, 3, 2, '2026-10-04', 4, 'friend', 5)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            "meaning-migration-preservation", 2, true, AppDatabase.MIGRATION_1_2,
        )
        migrated.query("SELECT id, name, createdAt FROM profiles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals("기존 사용자", cursor.getString(1))
            assertEquals(100L, cursor.getLong(2))
            assertEquals(1, cursor.count)
        }
        migrated.query("SELECT id, profileId, category, label, text, favorite, createdAt, updatedAt FROM expressions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(11L, cursor.getLong(0))
            assertEquals(1L, cursor.getLong(1))
            assertEquals("daily", cursor.getString(2))
            assertEquals("물", cursor.getString(3))
            assertEquals("물을 주세요", cursor.getString(4))
            assertEquals(1, cursor.getInt(5))
            assertEquals(101L, cursor.getLong(6))
            assertEquals(102L, cursor.getLong(7))
            assertEquals(1, cursor.count)
        }
        migrated.query("SELECT id, profileId, sourceKey, sourceRaw, approvedText, createdAt, lastUsedAt, useCount FROM corrections").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(21L, cursor.getLong(0))
            assertEquals(1L, cursor.getLong(1))
            assertEquals("물을 주새요", cursor.getString(2))
            assertEquals("물을 주새요!", cursor.getString(3))
            assertEquals("물을 주세요", cursor.getString(4))
            assertEquals(103L, cursor.getLong(5))
            assertEquals(104L, cursor.getLong(6))
            assertEquals(7, cursor.getInt(7))
            assertEquals(1, cursor.count)
        }
        migrated.query("SELECT profileId, approvals, shares, expressionsAdded, lastActiveDay, streakDays, mainCharacter, friendsSeen FROM counters").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals(8, cursor.getInt(1))
            assertEquals(3, cursor.getInt(2))
            assertEquals(2, cursor.getInt(3))
            assertEquals("2026-10-04", cursor.getString(4))
            assertEquals(4, cursor.getInt(5))
            assertEquals("friend", cursor.getString(6))
            assertEquals(5, cursor.getInt(7))
            assertEquals(1, cursor.count)
        }
        assertEquals(0L, count(migrated, "meaning_entries"))
        assertEquals(0L, count(migrated, "expression_aliases"))
    }

    @Test
    fun compositeForeignKeyRejectsOtherProfileAndDeletionCascadesAliases() {
        helper.createDatabase("meaning-migration-profile-key", 1).apply {
            execSQL("INSERT INTO profiles (id, name, createdAt) VALUES (1, '첫 사용자', 100), (2, '다른 사용자', 100)")
            close()
        }
        val migrated = helper.runMigrationsAndValidate(
            "meaning-migration-profile-key", 2, true, AppDatabase.MIGRATION_1_2,
        )
        migrated.setForeignKeyConstraintsEnabled(true)
        migrated.execSQL("INSERT INTO meaning_entries (id, profileId, displayText, status, createdAt, updatedAt) VALUES (1, 1, '물을 주세요', 'DRAFT', 101, 101)")
        migrated.execSQL("INSERT INTO expression_aliases (profileId, meaningId, rawAlias, normalizedAlias) VALUES (1, 1, '무', '무')")
        try {
            migrated.execSQL("INSERT INTO expression_aliases (profileId, meaningId, rawAlias, normalizedAlias) VALUES (2, 1, '무', '무')")
            fail("다른 프로필의 의미에 별칭을 연결할 수 없어야 한다.")
        } catch (_: SQLiteConstraintException) {
            // 복합 FK가 (profileId, meaningId) 두 값을 함께 검사한다.
        }
        try {
            migrated.execSQL("INSERT INTO meaning_entries (profileId, displayText, status, createdAt, updatedAt) VALUES (99, '잘못된 사용자', 'DRAFT', 101, 101)")
            fail("존재하지 않는 프로필에는 의미를 저장할 수 없어야 한다.")
        } catch (_: SQLiteConstraintException) {
            // 부모 프로필 FK도 적용되어야 한다.
        }
        migrated.query("SELECT version FROM meaning_entries WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
        }
        assertEquals(1L, count(migrated, "expression_aliases"))
        migrated.execSQL("DELETE FROM meaning_entries WHERE profileId = 1 AND id = 1")
        assertEquals(0L, count(migrated, "expression_aliases"))
    }

    private fun count(database: SupportSQLiteDatabase, table: String): Long =
        database.query("SELECT COUNT(*) FROM $table").use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }
}
