package kr.voicemate.malitda.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kr.voicemate.malitda.data.db.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val taken = repo.addExpression(1, walkId, "아람")
        assertEquals(MeaningRepository.AddExpressionResult.TakenBy("바람이 차가워요"), taken)
    }

    @Test
    fun deleteRemovesMeaningAndItsExpressions() = runBlocking {
        val id = repo.save(1, "산책하고 싶어요", null, null, listOf("바람"), confirmed = true)
        repo.delete(id)
        assertEquals(null, repo.byId(id))
        assertTrue(repo.expressionsFor(id).isEmpty())
        assertTrue(repo.allImageKeys().isEmpty())
    }
}
