package kr.voicemate.malitda.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kr.voicemate.malitda.data.db.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClearMeaningsRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var files: File
    private lateinit var repo: MeaningRepository
    private lateinit var management: DataManagementRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        files = File(context.cacheDir, "clear-meanings-${UUID.randomUUID()}").apply { mkdirs() }
        repo = MeaningRepository(db.meaningDao())
        management = DataManagementRepository(db, files)
    }

    @After fun close() { db.close(); files.deleteRecursively() }

    @Test fun dictionaryDeletionPreservesProfilesTrialsAndOtherPersonPictures() = runBlocking {
        db.profileDao().insert(ProfileEntity(1, "합성A", 1))
        db.profileDao().insert(ProfileEntity(2, "합성B", 1))
        val sharedName = "a".repeat(64) + ".png"
        val ownName = "b".repeat(64) + ".png"
        val sharedKey = "file:meaning_media/$sharedName"
        val dir = File(files, "meaning_media").apply { mkdirs() }
        File(dir, sharedName).writeText("synthetic")
        File(dir, ownName).writeText("synthetic")
        val mine = repo.save(1, "A뜻", "file:meaning_media/$ownName", null, listOf("A표현"), false)
        repo.save(1, "A공유그림", sharedKey, null, listOf("A별칭"), false)
        val other = repo.save(2, "B뜻", sharedKey, null, listOf("B표현"), false)
        repo.activateConfirmed(mine, 1, repo.byId(mine)!!.updatedAt, "gaze")
        val legacy = db.expressionDao().insert(ExpressionEntity(profileId = 1, category = "often", label = "A", text = "A문장", createdAt = 1, updatedAt = 1))
        db.correctionDao().upsert(CorrectionEntity(profileId = 1, sourceKey = "a", sourceRaw = "a", approvedText = "b", createdAt = 1, lastUsedAt = 1))
        db.trialLogDao().insert(TrialLogEntity(
            profileId = 1, startedAt = 1, endedAt = 2, origin = "CARD", voiceFirstHit = false,
            usedCardFallback = false, otherMeaningCount = 0, delivered = true, understood = 1,
            supporterHelped = false, meaningId = mine, meaningText = null, targetMeaningId = mine,
        ))

        management.clearMeanings(1)

        assertNotNull(db.profileDao().byId(1))
        assertNotNull(db.profileDao().byId(2))
        assertTrue(db.meaningDao().allFor(1).isEmpty())
        assertTrue(repo.expressionsFor(mine).isEmpty())
        assertTrue(repo.confirmationHistory(mine).isEmpty())
        assertNull(db.expressionDao().byId(legacy))
        assertTrue(db.correctionDao().all(1).isEmpty())
        assertEquals(mine, db.trialLogDao().allFor(1).single().meaningId)
        assertEquals(mine, db.trialLogDao().allFor(1).single().targetMeaningId)
        assertNotNull(repo.byId(other))
        assertEquals(1, repo.expressionsFor(other).size)
        assertTrue(File(dir, sharedName).exists())
        assertFalse(File(dir, ownName).exists())
    }

    @Test fun dictionaryDeletionRejectsInvalidProfileWithoutDeletingOtherData() = runBlocking {
        val id = repo.save(1, "합성 뜻", null, null, listOf("합성 표현"), false)
        try { management.clearMeanings(0); fail("invalid profile must be rejected") }
        catch (_: IllegalArgumentException) { }
        assertNotNull(repo.byId(id))
    }
}
