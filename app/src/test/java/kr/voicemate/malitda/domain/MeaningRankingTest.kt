package kr.voicemate.malitda.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningRankingTest {
    private val request = MeaningRankingRequest(
        profileId = 7L,
        sessionId = 42L,
        rawText = "바람",
        candidates = listOf(
            MeaningRankingCandidate(11L, 1L, "산책하고 싶어요", "밖에 나가기", "바람"),
            MeaningRankingCandidate(12L, 3L, "바람이 불어요", "날씨", "바람"),
            MeaningRankingCandidate(13L, 2L, "선풍기를 켜 주세요", null, "바람"),
        ),
        context = "실내",
    )
    private val originalIds = listOf(11L, 12L, 13L)

    @Test fun rulesKeepEveryCandidateInOriginalOrder() = runBlocking {
        val result = RuleMeaningCandidateRanker().rank(request)
        assertEquals(originalIds, result.orderedMeaningIds)
        assertEquals("rules", result.provider)
        assertFalse(result.abstain)
    }

    @Test fun acceptsOnlyACompletePermutationOfInputIds() {
        val proposed = MeaningRankingResult(listOf(13L, 11L, 12L), provider = "local-test")
        assertEquals(proposed, validateMeaningRankingResult(request, proposed))
        assertEquals("바람", request.rawText)
        assertEquals("산책하고 싶어요", request.candidates.first().displayText)
    }

    @Test fun foreignIdRejectsEntireResultInsteadOfFilteringIt() {
        assertFallback(MeaningRankingResult(listOf(13L, 999L, 11L), provider = "local-test"))
    }

    @Test fun duplicateIdRejectsEntireResultInsteadOfDeduplicatingIt() {
        assertFallback(MeaningRankingResult(listOf(13L, 11L, 11L), provider = "local-test"))
    }

    @Test fun missingCandidateRejectsEntireResultInsteadOfDroppingIt() {
        assertFallback(MeaningRankingResult(listOf(13L, 11L), provider = "local-test"))
    }

    @Test fun extraCandidateRejectsEntireResult() {
        assertFallback(MeaningRankingResult(listOf(13L, 11L, 12L, 999L), provider = "local-test"))
    }

    @Test fun emptyResultCannotHideExistingCandidates() {
        assertFallback(MeaningRankingResult(emptyList(), provider = "local-test"))
    }

    @Test fun nullFromTimeoutOrErrorRestoresEveryCandidate() {
        assertFallback(null)
    }

    @Test fun abstentionKeepsOriginalCandidatesAndUserChoice() {
        val result = validateMeaningRankingResult(
            request,
            MeaningRankingResult(emptyList(), abstain = true, provider = "local-test"),
        )
        assertEquals(originalIds, result.orderedMeaningIds)
        assertTrue(result.abstain)
        assertEquals("local-test", result.provider)
    }

    @Test fun abstentionDoesNotApplyEvenAValidSuggestedOrder() {
        val result = validateMeaningRankingResult(
            request,
            MeaningRankingResult(listOf(13L, 12L, 11L), abstain = true, provider = "local-test"),
        )
        assertEquals(originalIds, result.orderedMeaningIds)
        assertTrue(result.abstain)
    }

    @Test fun emptyInputCannotAcquireANewMeaning() {
        val emptyRequest = request.copy(candidates = emptyList())
        val result = validateMeaningRankingResult(
            emptyRequest,
            MeaningRankingResult(listOf(999L), provider = "local-test"),
        )
        assertEquals(emptyList<Long>(), result.orderedMeaningIds)
        assertEquals("rules", result.provider)
    }

    @Test fun duplicateInputIdsAreNotSilentlyDropped() {
        val malformedRequest = request.copy(candidates = listOf(request.candidates[0], request.candidates[0]))
        val result = validateMeaningRankingResult(
            malformedRequest,
            MeaningRankingResult(listOf(11L), provider = "local-test"),
        )
        assertEquals(listOf(11L, 11L), result.orderedMeaningIds)
        assertEquals("rules", result.provider)
    }

    private fun assertFallback(proposed: MeaningRankingResult?) {
        val result = validateMeaningRankingResult(request, proposed)
        assertEquals(originalIds, result.orderedMeaningIds)
        assertEquals("rules", result.provider)
        assertFalse(result.abstain)
    }
}
