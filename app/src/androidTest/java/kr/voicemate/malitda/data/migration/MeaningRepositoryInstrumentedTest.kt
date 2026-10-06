package kr.voicemate.malitda.data.migration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.voicemate.malitda.data.db.AppDatabase
import kr.voicemate.malitda.data.db.MeaningEntry
import kr.voicemate.malitda.data.db.ProfileEntity
import kr.voicemate.malitda.data.repo.MeaningRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeaningRepositoryInstrumentedTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MeaningRepository
    private var profileId = 0L
    private var otherProfileId = 0L

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).build()
        repository = MeaningRepository(database)
        profileId = database.profileDao().insert(ProfileEntity(name = "첫 사용자", createdAt = 100))
        otherProfileId = database.profileDao().insert(ProfileEntity(name = "다른 사용자", createdAt = 101))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savePreservesRawAliasAndNormalizesOnlyNfcAndWhitespace() = runBlocking {
        val raw = "\t한  \u00A0 말!\n"
        val saved = save(rawAliases = listOf(raw, "한 말!", "  ")) as MeaningRepository.SaveResult.Ok
        val stored = repository.get(profileId, saved.id)!!
        assertEquals(1L, saved.version)
        assertEquals(MeaningEntry.DRAFT, stored.meaning.status)
        assertNull(stored.meaning.confirmedBy)
        assertNull(stored.meaning.confirmationMethod)
        assertNull(stored.meaning.confirmedAt)
        assertEquals(1, stored.aliases.size)
        assertEquals(raw, stored.aliases.single().rawAlias)
        assertEquals("한 말!", stored.aliases.single().normalizedAlias)
        assertEquals(saved.id, repository.observe(profileId).first().single().meaning.id)
        assertTrue(repository.observe(otherProfileId).first().isEmpty())
    }

    @Test
    fun everyStateChangeUsesVersionGuardAndEditsRequireConfirmationAgain() = runBlocking {
        val first = save() as MeaningRepository.SaveResult.Ok
        assertFalse(repository.confirm(profileId, first.id, first.version, " ", "본인"))
        assertTrue(repository.confirm(profileId, first.id, first.version, "DIRECT", "본인"))
        val confirmed = repository.get(profileId, first.id)!!.meaning
        assertEquals(MeaningEntry.CONFIRMED, confirmed.status)
        assertEquals(2L, confirmed.version)
        assertEquals("DIRECT", confirmed.confirmationMethod)
        assertEquals("본인", confirmed.confirmedBy)
        assertNotNull(confirmed.confirmedAt)
        assertEquals(MeaningRepository.SaveResult.Conflict, save(id = first.id, expectedVersion = first.version))

        val edit = save(id = first.id, expectedVersion = confirmed.version, rawAliases = listOf("수정 표현")) as MeaningRepository.SaveResult.Ok
        val draft = repository.get(profileId, first.id)!!
        assertEquals(3L, edit.version)
        assertEquals(MeaningEntry.DRAFT, draft.meaning.status)
        assertNull(draft.meaning.confirmationMethod)
        assertNull(draft.meaning.confirmedBy)
        assertNull(draft.meaning.confirmedAt)
        assertEquals(listOf("수정 표현"), draft.aliases.map { it.rawAlias })
        assertFalse(repository.deactivate(profileId, first.id, confirmed.version))
        assertTrue(repository.deactivate(profileId, first.id, edit.version))
        val inactive = repository.get(profileId, first.id)!!.meaning
        assertEquals(MeaningEntry.INACTIVE, inactive.status)
        assertEquals(4L, inactive.version)
        assertFalse(repository.confirm(profileId, first.id, inactive.version, "DIRECT", "본인"))

        val reviewed = save(id = first.id, expectedVersion = inactive.version) as MeaningRepository.SaveResult.Ok
        assertEquals(5L, reviewed.version)
        assertEquals(MeaningEntry.DRAFT, repository.get(profileId, first.id)!!.meaning.status)
        assertTrue(repository.confirm(profileId, first.id, reviewed.version, "DIRECT", "본인"))
    }

    @Test
    fun confirmationMetadataHasLengthLimitsAndSupporterIsOptional() = runBlocking {
        val saved = save() as MeaningRepository.SaveResult.Ok
        assertFalse(repository.confirm(profileId, saved.id, saved.version, "가".repeat(121), "본인"))
        assertFalse(repository.confirm(profileId, saved.id, saved.version, "DIRECT", "가".repeat(81)))
        val untouched = repository.get(profileId, saved.id)!!.meaning
        assertEquals(MeaningEntry.DRAFT, untouched.status)
        assertEquals(saved.version, untouched.version)
        assertNull(untouched.confirmationMethod)
        assertNull(untouched.confirmedBy)
        assertTrue(repository.confirm(profileId, saved.id, saved.version, "가".repeat(120), "가".repeat(80)))

        val optional = save() as MeaningRepository.SaveResult.Ok
        assertTrue(repository.confirm(profileId, optional.id, optional.version, " DIRECT ", " "))
        val confirmed = repository.get(profileId, optional.id)!!.meaning
        assertEquals("DIRECT", confirmed.confirmationMethod)
        assertNull(confirmed.confirmedBy)

        val noSupporter = save() as MeaningRepository.SaveResult.Ok
        assertTrue(repository.confirm(profileId, noSupporter.id, noSupporter.version, "DIRECT", null))
        assertNull(repository.get(profileId, noSupporter.id)!!.meaning.confirmedBy)
    }

    @Test
    fun profileAndStaleVersionGuardsDoNotChangeMeaningOrAliases() = runBlocking {
        val first = save() as MeaningRepository.SaveResult.Ok
        val original = repository.get(profileId, first.id)
        assertNull(repository.get(otherProfileId, first.id))
        assertEquals(MeaningRepository.SaveResult.Conflict, save(profile = otherProfileId, id = first.id, expectedVersion = first.version))
        assertEquals(MeaningRepository.SaveResult.Conflict, save(id = first.id, expectedVersion = null))
        assertEquals(MeaningRepository.SaveResult.Conflict, save(id = first.id, expectedVersion = first.version + 1, rawAliases = listOf("바뀌면 안 됨")))
        assertFalse(repository.confirm(otherProfileId, first.id, first.version, "DIRECT", "본인"))
        assertFalse(repository.deactivate(otherProfileId, first.id, first.version))
        assertFalse(repository.delete(otherProfileId, first.id))
        assertEquals(original, repository.get(profileId, first.id))
    }

    @Test
    fun concurrentEditsAcceptExactlyOneVersionAndKeepItsAliases() = runBlocking {
        val first = save() as MeaningRepository.SaveResult.Ok
        val results = coroutineScope {
            listOf(
                async { "첫 수정" to save(id = first.id, expectedVersion = first.version, rawAliases = listOf("첫 수정")) },
                async { "두 번째 수정" to save(id = first.id, expectedVersion = first.version, rawAliases = listOf("두 번째 수정")) },
            ).map { it.await() }
        }
        assertEquals(1, results.count { it.second is MeaningRepository.SaveResult.Ok })
        assertEquals(1, results.count { it.second == MeaningRepository.SaveResult.Conflict })
        val winner = results.single { it.second is MeaningRepository.SaveResult.Ok }
        val stored = repository.get(profileId, first.id)!!
        assertEquals(2L, stored.meaning.version)
        assertEquals(winner.first, stored.aliases.single().rawAlias)
    }

    @Test
    fun validatesLengthsAliasCountAndRequiredValuesBeforeWriting() = runBlocking {
        val invalid = listOf(
            save(rawAliases = listOf(" ")),
            save(rawAliases = List(6) { "표현 $it" }),
            save(rawAliases = listOf("가".repeat(81))),
            save(text = " "),
            save(text = "가".repeat(201)),
            save(context = "가".repeat(121)),
            save(profile = 999),
        )
        assertTrue(invalid.all { it is MeaningRepository.SaveResult.Invalid })
        assertEquals(0, database.meaningDao().count(profileId))
        assertTrue(save(rawAliases = listOf("가".repeat(80)), text = "가".repeat(200), context = "가".repeat(120)) is MeaningRepository.SaveResult.Ok)
    }

    @Test
    fun profileLimitIsAtomicAllowsEditingAndDoesNotLimitOtherProfile() = runBlocking {
        var first: MeaningRepository.SaveResult.Ok? = null
        repeat(49) { index ->
            val saved = save(rawAliases = listOf("표현 $index")) as MeaningRepository.SaveResult.Ok
            if (index == 0) first = saved
        }
        val results = coroutineScope {
            listOf(async { save(rawAliases = listOf("추가 A")) }, async { save(rawAliases = listOf("추가 B")) }).map { it.await() }
        }
        assertEquals(1, results.count { it is MeaningRepository.SaveResult.Ok })
        assertEquals(1, results.count { it == MeaningRepository.SaveResult.LimitReached })
        assertEquals(50, database.meaningDao().count(profileId))
        assertTrue(save(id = first!!.id, expectedVersion = first!!.version, text = "수정한 뜻") is MeaningRepository.SaveResult.Ok)
        assertTrue(save(profile = otherProfileId) is MeaningRepository.SaveResult.Ok)
        assertTrue(repository.delete(profileId, first!!.id))
        assertTrue(save(rawAliases = listOf("삭제 후 추가")) is MeaningRepository.SaveResult.Ok)
    }

    @Test
    fun deletingMeaningAndProfileRemovesTheirAliases() = runBlocking {
        val first = save() as MeaningRepository.SaveResult.Ok
        assertTrue(repository.delete(profileId, first.id))
        assertNull(repository.get(profileId, first.id))
        assertFalse(repository.delete(profileId, first.id))
        val second = save() as MeaningRepository.SaveResult.Ok
        database.profileDao().delete(profileId)
        assertNull(repository.get(profileId, second.id))
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM expression_aliases").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0L, cursor.getLong(0))
        }
    }

    private suspend fun save(
        profile: Long = profileId,
        id: Long? = null,
        expectedVersion: Long? = null,
        rawAliases: List<String> = listOf("무"),
        text: String = "물을 주세요",
        context: String? = "식사",
    ): MeaningRepository.SaveResult = repository.save(
        profileId = profile, id = id, expectedVersion = expectedVersion,
        rawAliases = rawAliases, displayText = text, imageRef = null,
        contextLabel = context, confirmedBy = "저장만으로 확인되지 않아야 함",
    )
}
