package kr.voicemate.malitda.data.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kr.voicemate.malitda.data.db.AppDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The historical personal-dictionary branch reused database versions 2 and 3 with different table names. */
@RunWith(AndroidJUnit4::class)
class LegacyMeaningBranchMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test fun knownLegacyVersion2UpgradesWithoutLosingPersonalData() = verifyLegacyUpgrade(2)
    @Test fun knownLegacyVersion3UpgradesWithoutLosingPersonalData() = verifyLegacyUpgrade(3)

    @Test fun canonicalVersion3StillUpgradesToCurrentRoomSchema() {
        val name = "canonical-v3-current"
        helper.createDatabase(name, 3).apply {
            execSQL("INSERT INTO profiles (id,name,createdAt) VALUES (11,'synthetic',1)")
            execSQL("INSERT INTO meanings (id,profileId,text,imageKey,situation,confirmed,active,createdAt,updatedAt,useCount) VALUES (101,11,'preserved','file:means_media/${"a".repeat(64)}.png','context',1,1,10,20,9)")
            execSQL("INSERT INTO meaning_expressions (id,profileId,meaningId,text,key) VALUES (201,11,101,'alias','alias')")
            close()
        }
        helper.runMigrationsAndValidate(name, 5, true, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5).use { db ->
            db.query("SELECT text,imageKey,useCount,isSample,legacyVersion,legacyConfirmedBy FROM meanings WHERE id=101").use { c ->
                assertTrue(c.moveToFirst()); assertEquals("preserved", c.getString(0))
                assertEquals("file:means_media/${"a".repeat(64)}.png", c.getString(1))
                assertEquals(9, c.getInt(2)); assertEquals(0, c.getInt(3)); assertTrue(c.isNull(4)); assertTrue(c.isNull(5))
            }
            assertEquals(1L, count(db, "meaning_expressions"))
            assertEquals(0L, count(db, "confirmation_events"))
        }
    }

    @Test fun unknownLegacyColumnFailsBeforeChangingOrDroppingTheSource() {
        helper.createDatabase("unknown-legacy-column", 1).use { db ->
            createKnownLegacy(db)
            db.execSQL("ALTER TABLE meaning_entries ADD COLUMN unknown_private_metadata TEXT")
            assertTrue(runCatching { AppDatabase.MIGRATION_3_4.migrate(db) }.isFailure)
            assertEquals(3L, count(db, "meaning_entries"))
            assertEquals(4L, count(db, "expression_aliases"))
            assertFalse(tableExists(db, "meanings"))
        }
    }

    @Test fun unknownLegacyStatusFailsBeforeChangingOrDroppingTheSource() {
        helper.createDatabase("unknown-legacy-status", 1).use { db ->
            createKnownLegacy(db)
            db.execSQL("UPDATE meaning_entries SET status='UNKNOWN' WHERE id=101")
            assertTrue(runCatching { AppDatabase.MIGRATION_2_3.migrate(db) }.isFailure)
            assertEquals(3L, count(db, "meaning_entries"))
            assertFalse(tableExists(db, "meanings"))
        }
    }

    @Test fun unknownExtraPrivateTableFailsBeforeConvertingTheKnownTables() {
        helper.createDatabase("unknown-legacy-table", 1).use { db ->
            createKnownLegacy(db)
            db.execSQL("CREATE TABLE unknown_private_archive (id INTEGER PRIMARY KEY, payload TEXT)")
            db.execSQL("INSERT INTO unknown_private_archive VALUES (1,'synthetic')")
            assertTrue(runCatching { AppDatabase.MIGRATION_3_4.migrate(db) }.isFailure)
            assertEquals(3L, count(db, "meaning_entries"))
            assertEquals(1L, count(db, "unknown_private_archive"))
            assertFalse(tableExists(db, "meanings"))
        }
    }

    private fun verifyLegacyUpgrade(version: Int) {
        val name = "legacy-branch-v$version-current"
        helper.createDatabase(name, 1).apply { createKnownLegacy(this); this.version = version; close() }
        val migrations = if (version == 2) arrayOf(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            else arrayOf(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
        helper.runMigrationsAndValidate(name, 5, true, *migrations).use { db ->
            assertEquals(2L, count(db, "profiles"))
            assertEquals(3L, count(db, "meanings"))
            assertEquals(4L, count(db, "meaning_expressions"))
            assertEquals(1L, count(db, "expressions"))
            assertEquals(0L, count(db, "trial_logs"))
            assertFalse(tableExists(db, "meaning_entries")); assertFalse(tableExists(db, "expression_aliases"))
            db.query("SELECT id,profileId,text,imageKey,situation,confirmed,active,createdAt,updatedAt,isSample,legacyVersion,legacyConfirmedBy,confirmationMethod,confirmedAt FROM meanings ORDER BY id").use { c ->
                assertTrue(c.moveToNext())
                assertEquals(101L, c.getLong(0)); assertEquals(11L, c.getLong(1)); assertEquals("draft text", c.getString(2))
                assertTrue(c.isNull(3)); assertTrue(c.isNull(4)); assertEquals(0, c.getInt(5)); assertEquals(1, c.getInt(6))
                assertEquals(1L, c.getLong(7)); assertEquals(10L, c.getLong(8)); assertEquals(0, c.getInt(9))
                assertEquals(3L, c.getLong(10)); assertTrue(c.isNull(11)); assertTrue(c.isNull(12)); assertTrue(c.isNull(13))
                assertTrue(c.moveToNext())
                assertEquals(102L, c.getLong(0)); assertEquals(11L, c.getLong(1)); assertEquals("confirmed text", c.getString(2))
                assertEquals("file:means_media/${"a".repeat(64)}.png", c.getString(3)); assertEquals("after lunch", c.getString(4))
                assertEquals(1, c.getInt(5)); assertEquals(1, c.getInt(6)); assertEquals(2L, c.getLong(7)); assertEquals(20L, c.getLong(8))
                assertEquals(0, c.getInt(9)); assertEquals(4L, c.getLong(10)); assertEquals("S01", c.getString(11)); assertEquals("pointing", c.getString(12)); assertEquals(15L, c.getLong(13))
                assertTrue(c.moveToNext())
                assertEquals(103L, c.getLong(0)); assertEquals(12L, c.getLong(1)); assertEquals("inactive text", c.getString(2))
                assertEquals("file:meaning_media/${"b".repeat(64)}.png", c.getString(3)); assertEquals("after activity", c.getString(4))
                assertEquals(0, c.getInt(5)); assertEquals(0, c.getInt(6)); assertEquals(3L, c.getLong(7)); assertEquals(30L, c.getLong(8))
                assertEquals(0, c.getInt(9)); assertEquals(7L, c.getLong(10)); assertEquals("", c.getString(11)); assertEquals("gaze", c.getString(12)); assertEquals(25L, c.getLong(13))
            }
            db.query("SELECT id,profileId,meaningId,text,key FROM meaning_expressions ORDER BY id").use { c ->
                assertTrue(c.moveToNext()); assertEquals(201L, c.getLong(0)); assertEquals(11L, c.getLong(1)); assertEquals(101L, c.getLong(2)); assertEquals("wind?", c.getString(3)); assertEquals("wind", c.getString(4))
                assertTrue(c.moveToNext()); assertEquals(202L, c.getLong(0)); assertEquals(102L, c.getLong(2)); assertEquals("wind", c.getString(4))
                assertTrue(c.moveToNext()); assertEquals(203L, c.getLong(0)); assertEquals(12L, c.getLong(1)); assertEquals(103L, c.getLong(2))
                assertTrue(c.moveToNext()); assertEquals(204L, c.getLong(0)); assertEquals("walk", c.getString(4))
            }
            assertEquals(1L, count(db, "confirmation_events"))
            db.query("SELECT profileId,meaningId,meaningVersion,response,method,confirmedBy,createdAt FROM confirmation_events").use { c ->
                assertTrue(c.moveToFirst()); assertEquals(11L, c.getLong(0)); assertEquals(102L, c.getLong(1)); assertEquals(4L, c.getLong(2))
                assertEquals("matched", c.getString(3)); assertEquals("pointing", c.getString(4)); assertEquals("S01", c.getString(5)); assertEquals(15L, c.getLong(6))
            }
            // Duplicate aliases for the same meaning are still blocked after allowing one alias's multiple meanings.
            assertTrue(runCatching { db.execSQL("INSERT INTO meaning_expressions (profileId,meaningId,text,key) VALUES (11,102,'wind','wind')") }.isFailure)
        }
    }

    private fun createKnownLegacy(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO profiles (id,name,createdAt) VALUES (11,'synthetic A',1),(12,'synthetic B',2)")
        db.execSQL("INSERT INTO expressions (id,profileId,category,label,text,favorite,createdAt,updatedAt) VALUES (301,11,'message','legacy','legacy expression',0,1,2)")
        db.execSQL("CREATE TABLE meaning_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, profileId INTEGER NOT NULL, displayText TEXT NOT NULL, imageRef TEXT, contextLabel TEXT, status TEXT NOT NULL, version INTEGER NOT NULL DEFAULT 1, confirmationMethod TEXT, confirmedBy TEXT, confirmedAt INTEGER, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(profileId) REFERENCES profiles(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE UNIQUE INDEX index_meaning_entries_profileId_id ON meaning_entries (profileId,id)")
        db.execSQL("CREATE TABLE expression_aliases (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, profileId INTEGER NOT NULL, meaningId INTEGER NOT NULL, rawAlias TEXT NOT NULL, normalizedAlias TEXT NOT NULL, FOREIGN KEY(profileId,meaningId) REFERENCES meaning_entries(profileId,id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE UNIQUE INDEX index_expression_aliases_profileId_normalizedAlias_meaningId ON expression_aliases (profileId,normalizedAlias,meaningId)")
        db.execSQL("CREATE INDEX index_expression_aliases_profileId_meaningId ON expression_aliases (profileId,meaningId)")
        db.execSQL("CREATE INDEX index_expression_aliases_meaningId ON expression_aliases (meaningId)")
        db.execSQL("INSERT INTO meaning_entries (id,profileId,displayText,imageRef,contextLabel,status,version,confirmationMethod,confirmedBy,confirmedAt,createdAt,updatedAt) VALUES (101,11,'draft text',NULL,NULL,'DRAFT',3,NULL,NULL,NULL,1,10)")
        db.execSQL("INSERT INTO meaning_entries (id,profileId,displayText,imageRef,contextLabel,status,version,confirmationMethod,confirmedBy,confirmedAt,createdAt,updatedAt) VALUES (102,11,'confirmed text','file:means_media/${"a".repeat(64)}.png','after lunch','CONFIRMED',4,'pointing','S01',15,2,20)")
        db.execSQL("INSERT INTO meaning_entries (id,profileId,displayText,imageRef,contextLabel,status,version,confirmationMethod,confirmedBy,confirmedAt,createdAt,updatedAt) VALUES (103,12,'inactive text','file:meaning_media/${"b".repeat(64)}.png','after activity','INACTIVE',7,'gaze','',25,3,30)")
        db.execSQL("INSERT INTO expression_aliases (id,profileId,meaningId,rawAlias,normalizedAlias) VALUES (201,11,101,'wind?','wind'),(202,11,102,'wind','wind'),(203,12,103,'rest','rest'),(204,11,102,'walk','walk')")
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean = db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { it.moveToFirst() }
    private fun count(db: SupportSQLiteDatabase, table: String): Long = db.query("SELECT COUNT(*) FROM `$table`").use { assertTrue(it.moveToFirst()); it.getLong(0) }
}
