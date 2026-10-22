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

    /** 첫 실행 데모용 기본 4종(디자인 42화면의 카드와 동일). 학생과 확인을 마친 상태로 넣는다. */
    suspend fun seedDefaults(profileId: Long) {
        if (dao.count(profileId) > 0) return
        save(profileId, "산책하고 싶어요", "walk", "쉬는 시간, 점심 후", listOf("산책", "산책 가자", "걷고 싶어"), confirmed = true)
        save(profileId, "물이 필요해요", "water", "식사·활동 중", listOf("물", "물 주세요", "목말라"), confirmed = true)
        save(profileId, "쉬고 싶어요", "rest", "활동이 길어질 때", listOf("쉬고 싶어", "쉬자", "휴식"), confirmed = true)
        save(profileId, "바람이 차가워요", "cold", "야외 활동", listOf("바람", "추워", "차가워"), confirmed = true)
    }
}
