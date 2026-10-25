package kr.voicemate.malitda.domain

import kr.voicemate.malitda.data.db.MeaningEntity
import kr.voicemate.malitda.data.db.MeaningExpressionEntity
import kr.voicemate.malitda.data.db.MeaningWithExpressions
import org.junit.Assert.*
import org.junit.Test

/** 같은 표현의 여러 뜻은 후보로만 제시한다. 상황·이력으로 자동 승인하지 않는다. */
class MeaningPersonalizationTest {
    private fun item(id: Long, text: String, situation: String?, active: Boolean = true, confirmed: Boolean = true) =
        MeaningWithExpressions(
            MeaningEntity(id, 1, text, null, situation, confirmed, active, 1, 1, 0),
            listOf(MeaningExpressionEntity(profileId = 1, meaningId = id, text = "바람", key = "바람")),
        )

    @Test fun sameExpressionCanProduceTwoExplicitChoices() {
        val items = listOf(item(1, "산책하고 싶어요", "점심 후"), item(2, "추워요", "야외"))
        assertEquals(setOf(1L, 2L), MeaningResolver.resolve("바람", emptyList(), items).map { it.meaning.id }.toSet())
    }

    @Test fun contextRanksOnlyAlreadyMatchedMeanings() {
        val items = listOf(item(1, "산책하고 싶어요", "점심 후"), item(2, "추워요", "야외"))
        assertEquals(2L, MeaningResolver.resolve("바람", emptyList(), items, context = "야외").first().meaning.id)
        assertTrue(MeaningResolver.resolve("모르는 표현", emptyList(), items, context = "야외").isEmpty())
    }

    @Test fun inactiveAndUnconfirmedPersonalItemsAreNeverCandidates() {
        val items = listOf(item(1, "비활성", null, active = false), item(2, "미확인", null, confirmed = false))
        assertTrue(MeaningResolver.resolve("바람", emptyList(), items).isEmpty())
    }

    @Test fun displayCandidateLimitIsBoundedToOneThroughThree() {
        val items = (1L..5L).map { item(it, "뜻 $it", null) }
        assertEquals(3, MeaningResolver.resolve("바람", emptyList(), items, maxCandidates = 99).size)
        assertEquals(1, MeaningResolver.resolve("바람", emptyList(), items, maxCandidates = 0).size)
    }

    @Test fun uncertainFuzzyMatchIsMarkedAndCanBeDisabled() {
        val items = listOf(item(1, "산책", null))
        assertTrue(MeaningResolver.resolve("아람", emptyList(), items).single().lowConfidence)
        assertTrue(MeaningResolver.resolve("아람", emptyList(), items, fuzzyThreshold = 31).isEmpty())
    }

    @Test fun normalizationDoesNotRewriteRawTextOrRegisteredMeaning() {
        val raw = " 바람! "
        val meaning = item(1, "산책하고 싶어요", null)
        val matches = MeaningResolver.resolve(raw, emptyList(), listOf(meaning))
        assertEquals(" 바람! ", raw)
        assertEquals("산책하고 싶어요", matches.single().meaning.text)
    }

    @Test fun numericAndNegationChangesRequireExplicitReview() {
        assertTrue(MeaningSafetyReview.warnings("3시", "4시에 만나요").isNotEmpty())
        assertTrue(MeaningSafetyReview.warnings("가고 싶지 않아", "가고 싶어요").isNotEmpty())
        assertTrue(MeaningSafetyReview.warnings("3시", "3시에 만나요").isEmpty())
    }

    @Test fun comparisonNormalizesExplicitNumberUnitsButPreservesOriginal() {
        val raw = " 세 시에   １,０００원! "
        assertEquals("3시에 1000원", TextNormalizer.comparison(raw))
        assertEquals(" 세 시에   １,０００원! ", raw)
        assertEquals("한강", TextNormalizer.comparison("한강"))
        assertEquals("두바이", TextNormalizer.comparison("두바이"))
        assertEquals("한시적 한일", TextNormalizer.comparison("한시적 한일"))
        assertEquals("3시 10분에", TextNormalizer.comparison("삼 시 십 분에"))
        assertEquals("와 잠깐", TextNormalizer.comparison("와, 잠깐"))
    }

    @Test fun generalEditIsLowestPriorityAndNumericNamesNeverApproximate() {
        val original = item(1, "도와주세요", null).copy(expressions = listOf(
            MeaningExpressionEntity(profileId = 1, meaningId = 1, text = "오늘은 학교에 가고 싶어요", key = "오늘은 학교에 가고 싶어요")
        ))
        val result = MeaningResolver.resolve("오늘은 학교에 쉬고 싶어요", emptyList(), listOf(original), fuzzyThreshold = 25)
        assertEquals("edit", result.single().matchReason)
        assertTrue(result.single().lowConfidence)
        assertTrue(MeaningResolver.resolve("오늘은 학교에 쉬고 싶어요", emptyList(), listOf(original), fuzzyThreshold = 30).isEmpty())
        val named = original.copy(meaning = original.meaning.copy(category = "name"))
        assertTrue(MeaningResolver.resolve("오늘은 학교에 쉬고 싶어요", emptyList(), listOf(named), fuzzyThreshold = 25).isEmpty())
        val numbers = original.copy(expressions = listOf(MeaningExpressionEntity(profileId = 1, meaningId = 1, text = "13시", key = "13시")))
        assertTrue(MeaningResolver.resolve("3시", emptyList(), listOf(numbers), fuzzyThreshold = 25).isEmpty())
    }

    @Test fun cappedHistoryCannotPromotePartialOverExactOrCreateNewMeaning() {
        val now = 10_000_000_000L
        val exact = item(1, "정확한 뜻", null)
        val frequent = item(2, "자주 쓴 뜻", null).copy(
            meaning = item(2, "자주 쓴 뜻", null).meaning.copy(useCount = Int.MAX_VALUE, lastUsedAt = now),
            expressions = listOf(MeaningExpressionEntity(profileId = 1, meaningId = 2, text = "바람 산책", key = "바람 산책")),
        )
        val items = listOf(frequent, exact)
        assertEquals(1L, MeaningResolver.resolve("바람", emptyList(), items, approvedTextsNormalized = setOf("자주 쓴 뜻"), now = now).first().meaning.id)
        assertTrue(MeaningResolver.resolve("모르는 말", emptyList(), items, approvedTextsNormalized = setOf("자주 쓴 뜻"), now = now).isEmpty())
        assertEquals(0.8, MeaningResolver.stringSimilarity("abcde", "abXde"), 0.0001)
    }
}
