package kr.voicemate.malitda.domain

/** 같은 사용자에게 등록·확인된 의미 후보의 읽기 전용 스냅샷. */
data class MeaningRankingCandidate(
    val id: Long,
    val version: Long,
    val displayText: String,
    val contextLabel: String?,
    val matchedAlias: String,
)

/** rawText는 STT 인식 결과이며, displayText로 덮어쓰지 않는다. */
data class MeaningRankingRequest(
    val profileId: Long,
    val sessionId: Long,
    val rawText: String,
    val candidates: List<MeaningRankingCandidate>,
    val context: String?,
)

/** 입력에 있던 후보 ID의 순서만 반환한다. 문장 생성·등록·승인 권한은 없다. */
data class MeaningRankingResult(
    val orderedMeaningIds: List<Long>,
    val abstain: Boolean = false,
    val provider: String = "rules",
)

fun interface MeaningCandidateRanker {
    suspend fun rank(request: MeaningRankingRequest): MeaningRankingResult
}

/** 기본 구현은 사전 검색에서 정한 순서를 그대로 유지하며 기기 안에서 동작한다. */
class RuleMeaningCandidateRanker : MeaningCandidateRanker {
    override suspend fun rank(request: MeaningRankingRequest): MeaningRankingResult =
        MeaningRankingResult(orderedMeaningIds = request.candidates.map { it.id })
}

/**
 * 결과는 입력 ID 전체의 순열일 때만 적용한다. 후보를 일부만 반환하거나 새로운 ID를
 * 섞으면 결과 전체를 거절하고 원래 순서로 복원한다. null은 호출부의 실패·시간초과다.
 * 판단 보류 시 반환된 순서는 사용하지 않고 모든 원래 후보와 abstain 표시를 유지한다.
 * profile/session/version의 현재성 확인은 결과를 화면에 적용하는 호출부가 맡는다.
 */
fun validateMeaningRankingResult(
    request: MeaningRankingRequest,
    result: MeaningRankingResult?,
): MeaningRankingResult {
    val originalIds = request.candidates.map { it.id }
    val fallback = MeaningRankingResult(orderedMeaningIds = originalIds)
    if (result == null) return fallback
    if (result.abstain) return fallback.copy(abstain = true, provider = result.provider)

    val expectedIds = originalIds.toSet()
    val proposedIds = result.orderedMeaningIds.toList()
    val proposedSet = proposedIds.toSet()
    if (expectedIds.size != originalIds.size ||
        proposedIds.size != originalIds.size ||
        proposedSet.size != proposedIds.size ||
        proposedSet != expectedIds
    ) return fallback

    return result.copy(orderedMeaningIds = proposedIds)
}
