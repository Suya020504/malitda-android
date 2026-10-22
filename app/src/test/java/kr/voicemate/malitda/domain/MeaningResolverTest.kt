package kr.voicemate.malitda.domain

import kr.voicemate.malitda.data.db.MeaningEntity
import kr.voicemate.malitda.data.db.MeaningExpressionEntity
import kr.voicemate.malitda.data.db.MeaningWithExpressions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** v2 의미 후보 결정 규칙 검증(PR #1의 테스트 접근을 현 구현에 맞게 이식). */
class MeaningResolverTest {

    private var nextId = 1L
    private fun meaning(text: String, useCount: Int = 0) = MeaningEntity(
        id = nextId++, profileId = 1, text = text, imageKey = null, situation = null,
        confirmed = true, active = true, createdAt = 0, updatedAt = 0, useCount = useCount,
    )

    private fun entry(m: MeaningEntity, vararg exprs: String) = MeaningWithExpressions(
        meaning = m,
        expressions = exprs.map {
            MeaningExpressionEntity(profileId = 1, meaningId = m.id, text = it, key = TextNormalizer.normalize(it))
        },
    )

    @Test
    fun exactMatchOnRawRanksFirst() {
        val walk = meaning("산책하고 싶어요")
        val cold = meaning("바람이 차가워요")
        val matches = MeaningResolver.resolve(
            raw = "바람",
            alternatives = emptyList(),
            registered = listOf(entry(walk, "바람"), entry(cold, "추워")),
        )
        assertEquals(1, matches.size)
        assertEquals(walk.id, matches.first().meaning.id)
        assertEquals("바람", matches.first().matchedExpression)
    }

    @Test
    fun rawContainingExpressionMatches() {
        val walk = meaning("산책하고 싶어요")
        val matches = MeaningResolver.resolve("바람 바람 불어요", emptyList(), listOf(entry(walk, "바람")))
        assertEquals(walk.id, matches.single().meaning.id)
    }

    @Test
    fun singleCharacterExpressionRequiresExactMatch() {
        val water = meaning("물이 필요해요")
        // 한 글자 키는 오탐을 막기 위해 포함 매칭을 하지 않는다 — 완전 일치만.
        assertTrue(MeaningResolver.resolve("물 주세요 빨리", emptyList(), listOf(entry(water, "물"))).isEmpty())
        assertEquals(water.id, MeaningResolver.resolve("물", emptyList(), listOf(entry(water, "물"))).single().meaning.id)
    }

    @Test
    fun exactMatchInAlternativeBeatsContainsInRaw() {
        val walk = meaning("산책하고 싶어요")
        val rest = meaning("쉬고 싶어요")
        val matches = MeaningResolver.resolve(
            raw = "쉬자 그래",
            alternatives = listOf("바람"),
            registered = listOf(entry(walk, "바람"), entry(rest, "쉬자")),
        )
        // 복수 후보의 완전 일치(100-1)가 원문의 포함 일치(60+2)보다 높다.
        assertEquals(walk.id, matches.first().meaning.id)
        assertEquals(2, matches.size)
    }

    @Test
    fun longerExpressionWinsWhenBothContained() {
        val a = meaning("산책하고 싶어요")
        val b = meaning("같이 나가고 싶어요")
        val matches = MeaningResolver.resolve(
            raw = "산책 가자 지금",
            alternatives = emptyList(),
            registered = listOf(entry(a, "산책"), entry(b, "산책 가자")),
        )
        assertEquals(b.id, matches.first().meaning.id)
    }

    @Test
    fun normalizationIgnoresTrailingPunctuationAndSpaces() {
        val walk = meaning("산책하고 싶어요")
        val matches = MeaningResolver.resolve("바람!  ", emptyList(), listOf(entry(walk, "바람")))
        assertEquals(walk.id, matches.single().meaning.id)
    }

    @Test
    fun emptyWhenNothingRegisteredOrNoMatch() {
        assertTrue(MeaningResolver.resolve("바람", emptyList(), emptyList()).isEmpty())
        val walk = meaning("산책하고 싶어요")
        assertTrue(MeaningResolver.resolve("전혀 다른 말", emptyList(), listOf(entry(walk, "바람"))).isEmpty())
    }
}
