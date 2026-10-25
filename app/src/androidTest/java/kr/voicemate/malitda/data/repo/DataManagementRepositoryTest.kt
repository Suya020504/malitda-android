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
class DataManagementRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var files: File
    private lateinit var cache: File
    private lateinit var repo: MeaningRepository
    private lateinit var management: DataManagementRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val root = File(context.cacheDir, "delete-test-${UUID.randomUUID()}").apply { mkdirs() }
        files = File(root, "files").apply { mkdirs() }
        cache = File(root, "cache").apply { mkdirs() }
        repo = MeaningRepository(db.meaningDao())
        management = DataManagementRepository(db, files, cache)
    }

    @After fun close() { db.close(); files.parentFile!!.deleteRecursively() }

    @Test fun deletingOneProfilePreservesOtherProfilesAndSharedPicture() = runBlocking {
        db.profileDao().insert(ProfileEntity(1, "합성A", 1))
        db.profileDao().insert(ProfileEntity(2, "합성B", 1))
        val sharedName = "a".repeat(64) + ".png"
        val privateName = "b".repeat(64) + ".png"
        val sharedKey = "file:meaning_media/$sharedName"
        val dir = File(files, "meaning_media").apply { mkdirs() }
        File(dir, sharedName).writeText("synthetic")
        File(dir, privateName).writeText("synthetic")
        val legacyDir = File(files, "means_media").apply { mkdirs() }
        val legacyShared = "c".repeat(64) + ".png"
        val legacyPrivate = "d".repeat(64) + ".png"
        File(legacyDir, legacyShared).writeText("synthetic legacy shared")
        File(legacyDir, legacyPrivate).writeText("synthetic legacy private")
        val first = repo.save(1, "A뜻", sharedKey, null, listOf("A표현"), false)
        repo.save(1, "A개인그림", "file:meaning_media/$privateName", null, emptyList(), false)
        val second = repo.save(2, "B뜻", sharedKey, null, listOf("B표현"), false)
        repo.save(2, "B이전그림", "file:means_media/$legacyShared", null, emptyList(), false)
        repo.save(1, "A이전그림", "file:means_media/$legacyPrivate", null, emptyList(), false)
        repo.activateConfirmed(first, 1, repo.byId(first)!!.updatedAt, "gaze", "operator-A")
        db.trialLogDao().insert(trial(1))
        db.trialLogDao().insert(trial(2))
        File(cache, "exports").apply { mkdirs() }.resolve("old.csv").writeText("synthetic")
        management.wipeProfile(1)
        assertNull(db.profileDao().byId(1))
        assertNotNull(db.profileDao().byId(2))
        assertNull(repo.byId(first))
        assertNotNull(repo.byId(second))
        assertTrue(repo.confirmationHistory(first).isEmpty())
        assertTrue(db.trialLogDao().allFor(1).isEmpty())
        assertEquals(1, db.trialLogDao().allFor(2).size)
        assertTrue(File(dir, sharedName).exists())
        assertFalse(File(dir, privateName).exists())
        assertTrue(File(legacyDir, legacyShared).exists())
        assertFalse(File(legacyDir, legacyPrivate).exists())
        assertFalse(File(cache, "exports").exists())
    }

    @Test fun deletingAllRemovesCurrentAndLegacyDataAndUserFilesButKeepsModels() = runBlocking {
        db.profileDao().insert(ProfileEntity(1, "합성A", 1))
        val id = repo.save(1, "A뜻", null, null, listOf("A표현"), false)
        repo.activateConfirmed(id, 1, repo.byId(id)!!.updatedAt, "speech", "operator-A")
        db.expressionDao().insert(ExpressionEntity(profileId = 1, category = "often", label = "A", text = "A", createdAt = 1, updatedAt = 1))
        db.correctionDao().upsert(CorrectionEntity(profileId = 1, sourceKey = "a", sourceRaw = "a", approvedText = "b", createdAt = 1, lastUsedAt = 1))
        db.counterDao().upsert(CounterEntity(profileId = 1, approvals = 1))
        db.trialLogDao().insert(trial(1))
        listOf("meaning_media", "means_media", "eval", "testaudio", "whisper").forEach { name ->
            File(files, name).apply { mkdirs() }.resolve("synthetic.bin").writeText("synthetic")
        }
        management.wipeAll()
        assertTrue(db.profileDao().allOnce().isEmpty())
        assertTrue(db.meaningDao().allFor(1).isEmpty())
        assertTrue(db.meaningDao().expressionsFor(id).isEmpty())
        assertTrue(db.meaningDao().confirmationHistory(id).isEmpty())
        assertTrue(db.trialLogDao().allProfiles().isEmpty())
        assertNull(db.expressionDao().byId(1))
        assertTrue(db.correctionDao().all(1).isEmpty())
        assertNull(db.counterDao().get(1))
        listOf("meaning_media", "means_media", "eval", "testaudio").forEach { assertFalse(File(files, it).exists()) }
        assertTrue(File(files, "whisper/synthetic.bin").exists())
    }

    @Test fun rankingResetPreservesExpressionsAndStudentConfirmation() = runBlocking {
        val id = repo.save(1, "개인 뜻", null, null, listOf("개인 표현"), false)
        repo.activateConfirmed(id, 1, repo.byId(id)!!.updatedAt, "pointing")
        repo.recordUse(id)
        db.correctionDao().upsert(CorrectionEntity(profileId = 1, sourceKey = "a", sourceRaw = "a", approvedText = "b", createdAt = 1, lastUsedAt = 1))
        management.resetRanking(1)
        assertEquals(0, repo.byId(id)!!.useCount)
        assertTrue(repo.byId(id)!!.confirmed)
        assertEquals(1, repo.expressionsFor(id).size)
        assertTrue(db.correctionDao().all(1).isEmpty())
    }

    @Test fun imageLookupRejectsTraversalAndGarbageCollectionProtectsDraftPicture() = runBlocking {
        val name = "a".repeat(64) + ".png"
        val dir = File(files, "meaning_media").apply { mkdirs() }
        File(dir, name).writeText("synthetic")
        File(files, "outside.png").writeText("synthetic")
        assertNull(MeaningImageStore.fileFor(files, "file:meaning_media/../outside.png"))
        assertNull(MeaningImageStore.fileFor(files, "file:means_media/../outside.png"))
        val key = "file:meaning_media/$name"
        val legacyKey = "file:means_media/$name"
        File(files, "means_media").apply { mkdirs() }.resolve(name).writeText("synthetic legacy")
        assertNotNull(MeaningImageStore.fileFor(files, key))
        assertNotNull(MeaningImageStore.fileFor(files, legacyKey))
        MeaningImageStore.removeUnreferenced(files, setOf(legacyKey), protectedKeys = setOf(key))
        assertTrue(File(dir, name).exists())
        assertTrue(File(files, "means_media/$name").exists())
        MeaningImageStore.removeUnreferenced(files, emptySet())
        assertFalse(File(dir, name).exists())
        assertFalse(File(files, "means_media/$name").exists())
        assertTrue(File(files, "outside.png").exists())
    }

    private fun trial(profileId: Long) = TrialLogEntity(
        profileId = profileId, startedAt = 1, endedAt = 2, origin = "CARD", voiceFirstHit = false,
        usedCardFallback = false, otherMeaningCount = 0, delivered = false, understood = -1,
        supporterHelped = false, meaningId = null, meaningText = null,
    )
}
