package kr.voicemate.malitda.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kr.voicemate.malitda.data.db.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 실사용 보완 3건의 데이터 동작 검증: 복수 표현 보존·중복 검사·수정 후 재확인(PR #1 접근 이식). */
@RunWith(AndroidJUnit4::class)
class MeaningRepositoryInstrumentedTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: MeaningRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = MeaningRepository(db.meaningDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveKeepsEveryExpression() = runBlocking {
        val id = repo.save(1, "산책하고 싶어요", "walk", "쉬는 시간", listOf("바람", "산책 가자", "걷고 싶어"), confirmed = true)
        assertEquals(3, repo.expressionsFor(id).size)
    }

    @Test
    fun saveKeepsOneExpressionPerNormalizedKey() = runBlocking {
        val id = repo.save(1, "산책", null, null, listOf(" 산책 ", "산책!", "...", " "), confirmed = false)
        assertEquals(listOf("산책"), repo.expressionsFor(id).map { it.key })
    }

    @Test
    fun findConflictsReportsExpressionOwnedByOtherMeaning() = runBlocking {
        val walkId = repo.save(1, "산책하고 싶어요", "walk", null, listOf("바람"), confirmed = true)
        val conflicts = repo.findConflicts(1, listOf("바람", "새표현"))
        assertEquals(1, conflicts.size)
        assertEquals("바람", conflicts.first().expression)
        assertEquals("산책하고 싶어요", conflicts.first().meaningText)
        // 자기 자신은 제외한다(수정 시 자기 표현은 중복이 아님).
        assertTrue(repo.findConflicts(1, listOf("바람"), excludeMeaningId = walkId).isEmpty())
        // 다른 프로필의 같은 표현은 충돌이 아니다.
        assertTrue(repo.findConflicts(2, listOf("바람")).isEmpty())
    }

    @Test
    fun updatePreservesExpressionsAndRequiresReconfirmation() = runBlocking {
        val id = repo.save(1, "산책하고 싶어요", "walk", null, listOf("바람", "산책 가자"), confirmed = true)
        val before = repo.byId(id)!!

        repo.update(id, "산책 가고 싶어요", "walk", "오후", listOf("바람", "산책 가자", "걷고 싶어"))

        val after = repo.byId(id)!!
        assertEquals("산책 가고 싶어요", after.text)
        assertEquals(3, repo.expressionsFor(id).size)
        // 수정은 학생과 다시 확인해야 쓸 수 있다(U09-E → U10 게이트).
        assertFalse(after.confirmed)
        assertTrue(after.updatedAt >= before.updatedAt)
    }

    @Test
    fun addExpressionLearnsMisrecognitionWithoutChangingMeaning() = runBlocking {
        val coldId = repo.save(1, "바람이 차가워요", "cold", null, listOf("바람"), confirmed = true)
        val walkId = repo.save(1, "산책하고 싶어요", "walk", null, listOf("산책"), confirmed = true)
        val before = repo.byId(coldId)!!

        // 음성 시험에서 "바람"이 "아람"으로 들렸다 — 학생이 카드로 고친 뒤 지원자가 연결.
        assertEquals(MeaningRepository.AddExpressionResult.Added, repo.addExpression(1, coldId, "아람"))
        assertEquals(listOf("바람", "아람"), repo.expressionsFor(coldId).map { it.text }.sorted())
        // 뜻은 바뀌지 않았으므로 확인 상태·updatedAt이 그대로다.
        assertEquals(before.updatedAt, repo.byId(coldId)!!.updatedAt)
        assertTrue(repo.byId(coldId)!!.confirmed)

        assertEquals(MeaningRepository.AddExpressionResult.AlreadyThere, repo.addExpression(1, coldId, "아람"))
        // 같은 표현의 다른 뜻도 저장해 상황에 따라 학생이 고를 수 있게 한다.
        assertEquals(MeaningRepository.AddExpressionResult.Added, repo.addExpression(1, walkId, "아람"))
    }

    @Test
    fun concurrentLearningAddsSameExpressionOnce() = runBlocking {
        val id = repo.save(1, "산책", null, null, emptyList(), confirmed = true)
        val before = repo.byId(id)
        val results = coroutineScope {
            List(10) { async(Dispatchers.Default) { repo.addExpression(1, id, "산책 표현") } }.awaitAll()
        }
        assertEquals(1, results.count { it == MeaningRepository.AddExpressionResult.Added })
        assertEquals(9, results.count { it == MeaningRepository.AddExpressionResult.AlreadyThere })
        assertEquals(listOf("산책 표현"), repo.expressionsFor(id).map { it.text })
        assertEquals(before, repo.byId(id))
    }

    @Test
    fun concurrentLearningForDifferentMeaningsPreservesBothChoices() = runBlocking {
        val firstId = repo.save(1, "첫 뜻", null, null, emptyList(), confirmed = true)
        val secondId = repo.save(1, "둘째 뜻", null, null, emptyList(), confirmed = true)
        val results = coroutineScope {
            listOf(firstId, secondId).map { id ->
                async(Dispatchers.Default) { repo.addExpression(1, id, "공통 표현") }
            }.awaitAll()
        }
        assertEquals(2, results.count { it == MeaningRepository.AddExpressionResult.Added })
        assertEquals(2, repo.expressionsFor(firstId).size + repo.expressionsFor(secondId).size)
    }

    @Test
    fun learningRejectsMissingMeaningAndDifferentProfileWithoutWriting() = runBlocking {
        val id = repo.save(1, "내 뜻", null, null, listOf("기존 표현"), confirmed = true)
        val before = repo.expressionsFor(id)
        // 이미 있는 표현도 프로필 소유권을 먼저 확인한다.
        assertTrue(runCatching { repo.addExpression(2, id, "기존 표현") }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { repo.addExpression(2, id, "새 표현") }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { repo.addExpression(1, id + 100, "없는 뜻 표현") }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(before, repo.expressionsFor(id))
        assertTrue(repo.findConflicts(2, listOf("기존 표현", "새 표현")).isEmpty())
        assertTrue(repo.expressionsFor(id + 100).isEmpty())
    }

    @Test
    fun deleteRemovesMeaningAndItsExpressions() = runBlocking {
        val id = repo.save(1, "산책하고 싶어요", null, null, listOf("바람"), confirmed = true)
        repo.delete(id)
        assertEquals(null, repo.byId(id))
        assertTrue(repo.expressionsFor(id).isEmpty())
        assertTrue(repo.allImageKeys().isEmpty())
    }

    @Test
    fun failedSaveRollsBackMeaningAndEveryNewExpression() = runBlocking {
        val ownerId = repo.save(1, "기존 뜻", null, null, listOf("이미 연결됨"), confirmed = true)
        rejectExpression("이미 연결됨")
        val result = runCatching {
            repo.save(1, "새 뜻", null, null, listOf("새 표현", "이미 연결됨"), confirmed = false)
        }
        assertTrue(result.isFailure)
        assertEquals(listOf(ownerId), db.meaningDao().allFor(1).map { it.id })
        assertEquals(listOf("이미 연결됨"), repo.expressionsFor(ownerId).map { it.text })
        assertTrue(repo.findConflicts(1, listOf("새 표현")).isEmpty())
    }

    @Test
    fun failedUpdateRestoresOriginalContentEvidenceAndExpressions() = runBlocking {
        val ownerId = repo.save(1, "다른 뜻", null, null, listOf("이미 연결됨"), confirmed = true)
        val editedId = repo.save(1, "기존 내용", "walk", "기존 상황", listOf("기존 표현"), confirmed = false)
        val initial = repo.byId(editedId)!!
        assertTrue(repo.activateConfirmed(editedId, 1, initial.updatedAt, "pointing"))
        val before = repo.byId(editedId)!!
        val beforeExpressions = repo.expressionsFor(editedId)
        rejectExpression("이미 연결됨")
        val result = runCatching {
            repo.update(editedId, "바꾼 내용", "water", "바꾼 상황", listOf("새 표현", "이미 연결됨"))
        }
        assertTrue(result.isFailure)
        assertEquals(before, repo.byId(editedId))
        assertEquals(beforeExpressions, repo.expressionsFor(editedId))
        assertEquals(listOf("이미 연결됨"), repo.expressionsFor(ownerId).map { it.text })
        assertTrue(repo.findConflicts(1, listOf("새 표현")).isEmpty())
    }

    @Test
    fun sampleCleanupKeepsPersonalMeaningWithSameTextAndImage() = runBlocking {
        repo.seedDefaults(1)
        val personalId = repo.save(1, "산책하고 싶어요", "walk", null, listOf("내 산책 표현"), confirmed = true)
        repo.seedDefaults(2)
        assertEquals(4, repo.removeSamples(1))
        assertEquals(listOf(personalId), db.meaningDao().allFor(1).map { it.id })
        assertFalse(repo.byId(personalId)!!.isSample)
        assertEquals(listOf("내 산책 표현"), repo.expressionsFor(personalId).map { it.text })
        assertEquals(4, db.meaningDao().allFor(2).size)
    }

    @Test
    fun seededExamplesRemainPracticeItemsWithoutStudentConfirmation() = runBlocking {
        repo.seedDefaults(1)
        repo.seedDefaults(1)
        val samples = db.meaningDao().allFor(1)
        assertEquals(4, samples.size)
        assertTrue(samples.all { it.isSample && !it.confirmed && it.confirmedAt == null })
        assertEquals(4, repo.usableWithExpressions(1).size)
    }

    @Test
    fun failedSeedDoesNotLeavePartialExamples() = runBlocking {
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_second_sample BEFORE INSERT ON meanings " +
                "WHEN NEW.text = '물이 필요해요' BEGIN SELECT RAISE(ABORT, 'simulated storage failure'); END"
        )
        assertTrue(runCatching { repo.seedDefaults(1) }.isFailure)
        assertTrue(db.meaningDao().allFor(1).isEmpty())
        assertTrue(repo.findConflicts(1, listOf("산책")).isEmpty())
    }

    @Test
    fun confirmationRejectsMissingWrongProfileStaleVersionAndUnknownMethod() = runBlocking {
        val id = repo.save(1, "확인할 뜻", null, null, listOf("확인 표현"), confirmed = false)
        val before = repo.byId(id)!!
        assertFalse(repo.activateConfirmed(id + 100, 1, before.updatedAt, "speech"))
        assertFalse(repo.activateConfirmed(id, 2, before.updatedAt, "speech"))
        assertTrue(runCatching { repo.activateConfirmed(id, 1, before.updatedAt, "guess") }.isFailure)
        assertEquals(before, repo.byId(id))
        repo.update(id, "바뀐 뜻", null, null, listOf("바뀐 표현"))
        assertTrue(repo.byId(id)!!.updatedAt > before.updatedAt)
        assertFalse(repo.activateConfirmed(id, 1, before.updatedAt, "speech"))
        assertFalse(repo.byId(id)!!.confirmed)
    }

    @Test
    fun currentStudentConfirmationRecordsEvidenceAndContentEditClearsIt() = runBlocking {
        repo.seedDefaults(1)
        val sample = db.meaningDao().allFor(1).first()
        repo.setActive(sample.id, false)
        assertTrue(repo.activateConfirmed(sample.id, 1, sample.updatedAt, "gaze"))
        val confirmed = repo.byId(sample.id)!!
        assertTrue(confirmed.confirmed && confirmed.active)
        assertFalse(confirmed.isSample)
        assertEquals("gaze", confirmed.confirmationMethod)
        assertEquals("matched", confirmed.confirmationResponse)
        assertTrue(confirmed.confirmedAt!! > 0)
        repo.update(sample.id, "수정한 나의 뜻", "walk", null, listOf("개인 표현"))
        val edited = repo.byId(sample.id)!!
        assertFalse(edited.confirmed)
        assertFalse(edited.isSample)
        assertNull(edited.confirmationMethod)
        assertNull(edited.confirmationResponse)
        assertNull(edited.confirmedAt)
        assertTrue(edited.updatedAt > confirmed.updatedAt)
        repo.removeSamples(1)
        assertEquals(edited, repo.byId(sample.id))
    }

    @Test
    fun unconfirmClearsStudentConfirmationEvidence() = runBlocking {
        val id = repo.save(1, "확인한 뜻", null, null, emptyList(), confirmed = false)
        assertTrue(repo.activateConfirmed(id, 1, repo.byId(id)!!.updatedAt, "other"))
        repo.setConfirmed(id, false)
        val unconfirmed = repo.byId(id)!!
        assertFalse(unconfirmed.confirmed)
        assertNull(unconfirmed.confirmationMethod)
        assertNull(unconfirmed.confirmationResponse)
        assertNull(unconfirmed.confirmedAt)
    }

    @Test
    fun fiftyMeaningLimitIsAtomicForConcurrentSaves() = runBlocking {
        repeat(49) { repo.save(1, "뜻 $it", null, null, listOf("표현 $it"), confirmed = false) }
        val results = coroutineScope {
            List(8) { index -> async(Dispatchers.Default) {
                runCatching { repo.save(1, "마지막 뜻 $index", null, null, listOf("마지막 표현 $index"), false) }
            } }.awaitAll()
        }
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(7, results.count { it.exceptionOrNull() is kr.voicemate.malitda.data.db.MeaningLimitReachedException })
        assertEquals(50, db.meaningDao().count(1))
        // 한도는 프로필별이며 수정은 추가 등록이 아니다.
        val existing = db.meaningDao().allFor(1).first()
        repo.update(existing.id, "수정한 뜻", null, null, listOf("수정 표현"))
        assertTrue(repo.save(2, "다른 학생", null, null, emptyList(), false) > 0)
    }

    @Test
    fun confirmationHistoryKeepsRefusalAndOperatorWithoutActivating() = runBlocking {
        val id = repo.save(1, "산책", null, null, listOf("바람"), false)
        val version = repo.byId(id)!!.updatedAt
        assertTrue(repo.recordConfirmationResponse(id, 1, version, "unsure", "gaze", "guardian-1"))
        assertFalse(repo.byId(id)!!.confirmed)
        assertTrue(repo.activateConfirmed(id, 1, version, "pointing", "guardian-2"))
        val history = repo.confirmationHistory(id)
        assertEquals(2, history.size)
        assertEquals(listOf("matched", "unsure"), history.map { it.response })
        assertEquals(listOf("guardian-2", "guardian-1"), history.map { it.confirmedBy })
        assertEquals(1, repo.usableWithExpressions(1).size)
        repo.delete(id)
        assertTrue(repo.confirmationHistory(id).isEmpty())
    }

    @Test
    fun categoryAndFavoriteDoNotAlterApprovedContentVersion() = runBlocking {
        val id = repo.save(1, "학교에 가요", null, null, listOf("학교"), false, category = "place")
        val before = repo.byId(id)!!
        repo.activateConfirmed(id, 1, before.updatedAt, "speech")
        repo.toggleFavorite(id)
        repo.setCategory(id, "message")
        val after = repo.byId(id)!!
        assertEquals("daily", after.category)
        assertTrue(after.favorite)
        assertEquals(before.updatedAt, after.updatedAt)
        assertTrue(after.confirmed)
    }

    @Test
    fun duplicateMeaningIsRejectedButDifferentSituationIsAllowed() = runBlocking {
        val id = repo.save(1, "산책", "walk", "점심 후", listOf("바람"), false)
        val duplicate = runCatching { repo.save(1, "산책", "walk", "점심 후", listOf("다른 표현"), false) }.exceptionOrNull()
        assertTrue(duplicate is kr.voicemate.malitda.data.db.MeaningAlreadyExistsException)
        assertEquals(id, (duplicate as kr.voicemate.malitda.data.db.MeaningAlreadyExistsException).meaningId)
        val another = repo.save(1, "산책", "walk", "오후 수업 후", listOf("바람"), false)
        assertTrue(id != another)
        assertEquals(2, db.meaningDao().count(1))
    }

    @Test
    fun trainingSamplesDoNotConsumeFiftyPersonalMeaningAllowance() = runBlocking {
        repo.seedDefaults(1)
        repeat(50) { repo.save(1, "개인 뜻 $it", null, null, listOf("표현 $it"), false) }
        assertEquals(54, db.meaningDao().count(1))
        assertEquals(50, db.meaningDao().countPersonal(1))
        assertTrue(runCatching { repo.save(1, "추가 뜻", null, null, emptyList(), false) }.exceptionOrNull() is kr.voicemate.malitda.data.db.MeaningLimitReachedException)
    }

    @Test
    fun everyVisibleMeaningCategorySurvivesSaveUpdateAndRead() = runBlocking {
        kr.voicemate.malitda.domain.MeaningCategory.keys.forEach { category ->
            val id = repo.save(1, "분류 $category", null, null, listOf("표현 $category"), false, category = category)
            assertEquals(category, repo.byId(id)!!.category)
            repo.update(id, "수정 $category", null, null, listOf("표현 $category"), category)
            assertEquals(category, repo.byId(id)!!.category)
        }
        val id = repo.save(2, "이전 분류", null, null, emptyList(), false, category = "name")
        assertEquals("people", repo.byId(id)!!.category)
        db.meaningDao().setCategory(id, "place")
        assertEquals("places", repo.byId(id)!!.category)
        assertEquals("place", db.meaningDao().byId(id)!!.category)
    }

    private fun rejectExpression(key: String) {
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_expression BEFORE INSERT ON meaning_expressions " +
                "WHEN NEW.key = '$key' BEGIN SELECT RAISE(ABORT, 'simulated storage failure'); END"
        )
    }
}
