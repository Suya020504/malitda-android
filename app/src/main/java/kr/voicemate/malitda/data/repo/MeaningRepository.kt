package kr.voicemate.malitda.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.voicemate.malitda.data.db.ExpressionInsertResult
import kr.voicemate.malitda.data.db.MeaningDao
import kr.voicemate.malitda.data.db.MeaningEntity
import kr.voicemate.malitda.data.db.MeaningExpressionEntity
import kr.voicemate.malitda.data.db.MeaningWithExpressions
import kr.voicemate.malitda.domain.TextNormalizer
import kr.voicemate.malitda.domain.MeaningCategory
import kr.voicemate.malitda.domain.Limits

/** v2 개인 의미 사전. 뜻(그림·문장)과 표현→뜻 연결을 관리한다. */
class MeaningRepository(private val dao: MeaningDao) {

    fun observeUsable(profileId: Long): Flow<List<MeaningEntity>> = dao.observeUsable(profileId).map { list -> list.map(::canonical) }
    fun observeAll(profileId: Long): Flow<List<MeaningEntity>> = dao.observeAll(profileId).map { list -> list.map(::canonical) }
    fun observe(id: Long): Flow<MeaningEntity?> = dao.observe(id).map { it?.let(::canonical) }
    suspend fun byId(id: Long): MeaningEntity? = dao.byId(id)?.let(::canonical)
    suspend fun usableWithExpressions(profileId: Long): List<MeaningWithExpressions> = dao.usableWithExpressions(profileId)
        .map { it.copy(meaning = canonical(it.meaning)) }
    suspend fun recordUse(id: Long) = dao.incrementUse(id, System.currentTimeMillis())
    /** 구버전 호출 호환용 확인 상태 변경. 확인 근거를 기록할 때는 activateConfirmed를 쓴다. */
    suspend fun setConfirmed(id: Long, confirmed: Boolean) = dao.setConfirmed(id, confirmed)
    /** 학생 응답과 확인 방식이 갖춰진 현재 버전만 활성화한다. */
    suspend fun activateConfirmed(id: Long, profileId: Long, expectedUpdatedAt: Long, method: String, confirmedBy: String = "supporter"): Boolean =
        dao.activateConfirmed(id, profileId, expectedUpdatedAt, method, System.currentTimeMillis(), confirmedBy)
    suspend fun recordConfirmationResponse(
        id: Long, profileId: Long, expectedUpdatedAt: Long, response: String,
        method: String? = null, confirmedBy: String = "supporter",
    ): Boolean = dao.recordConfirmationResponse(id, profileId, expectedUpdatedAt, response, method, confirmedBy, System.currentTimeMillis())
    suspend fun confirmationHistory(id: Long) = dao.confirmationHistory(id)
    suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active)
    fun observeArchived(profileId: Long): Flow<List<MeaningEntity>> = dao.observeArchived(profileId).map { list -> list.map(::canonical) }
    suspend fun expressionsFor(meaningId: Long) = dao.expressionsFor(meaningId)
    suspend fun toggleFavorite(id: Long) = dao.toggleFavorite(id)
    suspend fun setCategory(id: Long, category: String) = dao.setCategory(id, MeaningCategory.normalize(category))
    suspend fun resetRanking(profileId: Long) = dao.resetUseCount(profileId)

    /** 같은 표현의 다른 뜻을 안내한다. 복수 뜻을 금지하는 저장 오류가 아니다. */
    suspend fun findConflicts(profileId: Long, expressions: List<String>, excludeMeaningId: Long = 0L) =
        dao.findExpressionConflicts(
            profileId,
            expressions.map { TextNormalizer.normalize(it) }.filter { it.isNotBlank() },
            excludeMeaningId,
        )

    /** 수정(U09-E): 내용을 바꾸면 updatedAt을 올리고 미확인으로 되돌린다 — 학생과 다시 확인해야 쓸 수 있다. */
    suspend fun update(id: Long, text: String, imageKey: String?, situation: String?, expressions: List<String>, category: String? = null) {
        validateText(text)
        dao.updateWithExpressions(
            id, text.trim(), imageKey, situation?.trim()?.ifBlank { null },
            prepareExpressions(expressions), System.currentTimeMillis(), category?.let(MeaningCategory::normalize),
        )
    }

    /** 삭제(U09-D): 연결한 표현도 함께 지운다. 되돌릴 수 없다. */
    suspend fun delete(id: Long) {
        dao.deleteWithExpressions(id)
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
        return when (val result = dao.addExpressionIfAvailable(
            MeaningExpressionEntity(profileId = profileId, meaningId = meaningId, text = expression.trim(), key = key)
        )) {
            ExpressionInsertResult.Added -> AddExpressionResult.Added
            ExpressionInsertResult.AlreadyThere -> AddExpressionResult.AlreadyThere
            is ExpressionInsertResult.TakenBy -> AddExpressionResult.TakenBy(result.meaningText)
        }
    }

    suspend fun save(
        profileId: Long,
        text: String,
        imageKey: String?,
        situation: String?,
        expressions: List<String>,
        confirmed: Boolean,
        isSample: Boolean = false,
        category: String = "often",
    ): Long {
        validateText(text)
        val now = System.currentTimeMillis()
        return dao.insertWithExpressions(
            MeaningEntity(
                profileId = profileId, text = text.trim(), imageKey = imageKey,
                situation = situation?.trim()?.ifBlank { null },
                confirmed = confirmed && !isSample, active = true,
                createdAt = now, updatedAt = now, useCount = 0, isSample = isSample,
                category = MeaningCategory.normalize(category),
            ),
            prepareExpressions(expressions),
        )
    }

    /**
     * 출처가 예시로 표시된 뜻만 지운다. 과거 출처가 불명확한 기록은 보존한다.
     */
    suspend fun removeSamples(profileId: Long): Int = dao.removeSamples(profileId)

    /** 첫 실행 연습용 4종. 학생 확인 기록과 구분하고 중간 실패 시 전부 되돌린다. */
    suspend fun seedDefaults(profileId: Long) {
        val now = System.currentTimeMillis()
        fun sample(text: String, imageKey: String, situation: String, expressions: List<String>) =
            MeaningWithExpressions(
                MeaningEntity(
                    profileId = profileId, text = text, imageKey = imageKey, situation = situation,
                    confirmed = false, active = true, createdAt = now, updatedAt = now,
                    useCount = 0, isSample = true,
                ),
                prepareExpressions(expressions),
            )
        dao.seedSamplesIfEmpty(
            profileId,
            listOf(
                sample("산책하고 싶어요", "walk", "쉬는 시간, 점심 후", listOf("산책", "산책 가자", "걷고 싶어")),
                sample("물이 필요해요", "water", "식사·활동 중", listOf("물", "물 주세요", "목말라")),
                sample("쉬고 싶어요", "rest", "활동이 길어질 때", listOf("쉬고 싶어", "쉬자", "휴식")),
                sample("바람이 차가워요", "cold", "야외 활동", listOf("바람", "추워", "차가워")),
            ),
        )
    }

    /** 저장 전에 정규화 키가 같은 입력을 합친다. 소유자는 DAO 트랜잭션 안에서 지정한다. */
    private fun prepareExpressions(expressions: List<String>): List<MeaningExpressionEntity> =
        expressions.map { it.trim() }
            .map { MeaningExpressionEntity(profileId = 0, meaningId = 0, text = it, key = TextNormalizer.normalize(it)) }
            .filter { it.key.isNotBlank() }
            .distinctBy { it.key }

    private fun validateText(text: String) {
        require(text.isNotBlank()) { "전할 뜻을 입력해야 합니다." }
        require(text.trim().length <= 100) { "전할 뜻은 100자까지 입력할 수 있습니다." }
    }

    private fun canonical(meaning: MeaningEntity): MeaningEntity =
        meaning.copy(category = MeaningCategory.normalize(meaning.category))
}
