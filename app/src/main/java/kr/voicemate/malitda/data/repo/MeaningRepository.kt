package kr.voicemate.malitda.data.repo

import kotlinx.coroutines.flow.Flow
import kr.voicemate.malitda.data.db.MeaningDao
import kr.voicemate.malitda.data.db.MeaningEntity
import kr.voicemate.malitda.data.db.MeaningExpressionEntity
import kr.voicemate.malitda.data.db.MeaningWithExpressions
import kr.voicemate.malitda.domain.TextNormalizer

/** v2 개인 의미 사전. 뜻(그림·문장)과 표현→뜻 연결을 관리한다. */
class MeaningRepository(private val dao: MeaningDao) {

    fun observeUsable(profileId: Long): Flow<List<MeaningEntity>> = dao.observeUsable(profileId)
    fun observeAll(profileId: Long): Flow<List<MeaningEntity>> = dao.observeAll(profileId)
    fun observe(id: Long): Flow<MeaningEntity?> = dao.observe(id)
    suspend fun byId(id: Long): MeaningEntity? = dao.byId(id)
    suspend fun usableWithExpressions(profileId: Long): List<MeaningWithExpressions> = dao.usableWithExpressions(profileId)
    suspend fun recordUse(id: Long) = dao.incrementUse(id)
    /** 학생과 함께 확인(U10-C) 완료 → 전달 흐름에 활성화. */
    suspend fun setConfirmed(id: Long, confirmed: Boolean) = dao.setConfirmed(id, confirmed)
    suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active)
    fun observeArchived(profileId: Long): Flow<List<MeaningEntity>> = dao.observeArchived(profileId)
    suspend fun expressionsFor(meaningId: Long) = dao.expressionsFor(meaningId)

    /** 다른 뜻에 이미 연결된 표현이 있으면 돌려준다(등록·수정 전 중복 검사). */
    suspend fun findConflicts(profileId: Long, expressions: List<String>, excludeMeaningId: Long = 0L) =
        dao.findExpressionConflicts(
            profileId,
            expressions.map { TextNormalizer.normalize(it) }.filter { it.isNotBlank() },
            excludeMeaningId,
        )

    /** 수정(U09-E): 내용을 바꾸면 updatedAt을 올리고 미확인으로 되돌린다 — 학생과 다시 확인해야 쓸 수 있다. */
    suspend fun update(id: Long, text: String, imageKey: String?, situation: String?, expressions: List<String>) {
        val cur = dao.byId(id) ?: return
        // 표현을 먼저 바꾸고 뜻을 마지막에 갱신한다 — 뜻 변경을 관찰하는 화면이 새 표현 목록을 읽도록.
        dao.deleteExpressionsFor(id)
        expressions.map { it.trim() }.filter { it.isNotBlank() }.forEach { expr ->
            dao.insertExpression(
                MeaningExpressionEntity(profileId = cur.profileId, meaningId = id, text = expr, key = TextNormalizer.normalize(expr))
            )
        }
        dao.update(
            cur.copy(
                text = text.trim(), imageKey = imageKey,
                situation = situation?.trim()?.ifBlank { null },
                confirmed = false, updatedAt = System.currentTimeMillis(),
            )
        )
    }

    /** 삭제(U09-D): 연결한 표현도 함께 지운다. 되돌릴 수 없다. */
    suspend fun delete(id: Long) {
        dao.deleteExpressionsFor(id)
        dao.delete(id)
    }

    suspend fun allImageKeys(): Set<String> = dao.allImageKeys().toSet()

    sealed interface AddExpressionResult {
        data object Added : AddExpressionResult
        data object AlreadyThere : AddExpressionResult
        data class TakenBy(val meaningText: String) : AddExpressionResult
    }

    /**
     * 오인식 학습: 기존 뜻에 표현 하나를 덧붙인다. 뜻 자체(updatedAt)는 바꾸지 않는다 —
     * 뜻이 바뀐 것이 아니므로 진행 중인 선택을 U03-V로 무효화하지 않는다.
     */
    suspend fun addExpression(profileId: Long, meaningId: Long, expression: String): AddExpressionResult {
        val key = TextNormalizer.normalize(expression)
        require(key.isNotBlank())
        if (dao.expressionsFor(meaningId).any { it.key == key }) return AddExpressionResult.AlreadyThere
        dao.findExpressionConflicts(profileId, listOf(key), meaningId).firstOrNull()
            ?.let { return AddExpressionResult.TakenBy(it.meaningText) }
        dao.insertExpression(MeaningExpressionEntity(profileId = profileId, meaningId = meaningId, text = expression.trim(), key = key))
        return AddExpressionResult.Added
    }

    suspend fun save(
        profileId: Long,
        text: String,
        imageKey: String?,
        situation: String?,
        expressions: List<String>,
        confirmed: Boolean,
    ): Long {
        val now = System.currentTimeMillis()
        val id = dao.insert(
            MeaningEntity(
                profileId = profileId, text = text.trim(), imageKey = imageKey,
                situation = situation?.trim()?.ifBlank { null },
                confirmed = confirmed, active = true, createdAt = now, updatedAt = now, useCount = 0,
            )
        )
        expressions.map { it.trim() }.filter { it.isNotBlank() }.forEach { expr ->
            dao.insertExpression(
                MeaningExpressionEntity(profileId = profileId, meaningId = id, text = expr, key = TextNormalizer.normalize(expr))
            )
        }
        return id
    }

    /**
     * 예시 뜻 지우기: 첫 실행 때 넣은 기본 4종을 지운다(실증 전 정리용).
     * 지원자가 같은 문장으로 직접 등록한 뜻과 구별하기 위해 기본 그림 키까지 함께 맞춘다.
     */
    suspend fun removeSamples(profileId: Long): Int {
        val samples = mapOf(
            "산책하고 싶어요" to "walk", "물이 필요해요" to "water", "쉬고 싶어요" to "rest", "바람이 차가워요" to "cold",
        )
        var removed = 0
        dao.allFor(profileId).filter { samples[it.text] == it.imageKey }.forEach { delete(it.id); removed++ }
        return removed
    }

    /** 첫 실행 데모용 기본 4종(디자인 42화면의 카드와 동일). 학생과 확인을 마친 상태로 넣는다. */
    suspend fun seedDefaults(profileId: Long) {
        if (dao.count(profileId) > 0) return
        save(profileId, "산책하고 싶어요", "walk", "쉬는 시간, 점심 후", listOf("산책", "산책 가자", "걷고 싶어"), confirmed = true)
        save(profileId, "물이 필요해요", "water", "식사·활동 중", listOf("물", "물 주세요", "목말라"), confirmed = true)
        save(profileId, "쉬고 싶어요", "rest", "활동이 길어질 때", listOf("쉬고 싶어", "쉬자", "휴식"), confirmed = true)
        save(profileId, "바람이 차가워요", "cold", "야외 활동", listOf("바람", "추워", "차가워"), confirmed = true)
    }
}
