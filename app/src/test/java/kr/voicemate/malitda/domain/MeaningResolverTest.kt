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
    fun singleCharacterExpressionMatchesOnlyAsSeparateWord() {
        val water = meaning("물이 필요해요")
        val reg = listOf(entry(water, "물"))
        assertEquals(water.id, MeaningResolver.resolve("물", emptyList(), reg).single().meaning.id)
        // 실제 에뮬레이터 재현: "물 마시고 싶어요"에서 '물'을 찾아야 한다.
        assertEquals(water.id, MeaningResolver.resolve("물 마시고 싶어요", emptyList(), reg).single().meaning.id)
        assertEquals(water.id, MeaningResolver.resolve("물, 주세요", emptyList(), reg).single().meaning.id)
        // 다른 단어 안에 든 한 글자는 맞추지 않는다.
        assertTrue(MeaningResolver.resolve("물론 좋아요", emptyList(), reg).isEmpty())
        assertTrue(MeaningResolver.resolve("선물 받았어", emptyList(), reg).isEmpty())
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
    fun matchesWhenRecognizerDropsSpaces() {
        val rest = meaning("쉬고 싶어요")
        val reg = listOf(entry(rest, "쉬고 싶어"))
        // 실제 TTS 음성 시험에서 Whisper가 "쉬고싶어."로 받아썼다.
        assertEquals(rest.id, MeaningResolver.resolve("쉬고싶어.", emptyList(), reg).single().meaning.id)
        assertEquals(rest.id, MeaningResolver.resolve("나 쉬고싶어 지금", emptyList(), reg).single().meaning.id)
    }

    @Test
    fun oneJamoMisrecognitionBecomesLowRankCandidate() {
        val cold = meaning("바람이 차가워요")
        val water = meaning("물이 필요해요")
        val reg = listOf(entry(cold, "바람", "추워"), entry(water, "물", "목말라"))
        // 2026-10-24 TTS 음성 시험에서 Whisper가 실제로 낸 오인식
        assertEquals(cold.id, MeaningResolver.resolve("아람", emptyList(), reg).single().meaning.id)   // 초성 하나
        assertEquals(water.id, MeaningResolver.resolve("못말라", emptyList(), reg).single().meaning.id) // 받침 하나
        assertEquals(water.id, MeaningResolver.resolve("나 못말라", emptyList(), reg).single().meaning.id) // 문장 속 한 단어
        // 정확히 맞는 다른 뜻이 있으면 그쪽이 먼저
        val both = MeaningResolver.resolve("바람 못말라", emptyList(), reg)
        assertEquals(cold.id, both.first().meaning.id)
        assertTrue(both.first().score > both.last().score)
    }

    @Test
    fun fuzzyDoesNotOverreach() {
        val cold = meaning("바람이 차가워요")
        val water = meaning("물이 필요해요")
        val rest = meaning("쉬고 싶어요")
        val reg = listOf(entry(cold, "바람", "추워"), entry(water, "물"), entry(rest, "쉬고 싶어"))
        assertTrue(MeaningResolver.resolve("좋아", emptyList(), reg).isEmpty())          // 추워 오인식은 너무 달라 못 잡는다
        assertTrue(MeaningResolver.resolve("불", emptyList(), reg).isEmpty())            // 한 글자 표현 "물"은 ④ 제외
        assertTrue(MeaningResolver.resolve("물론 좋아요", emptyList(), reg).isEmpty())
        assertTrue(MeaningResolver.resolve("집에 가고 싶어", emptyList(), reg).isEmpty()) // 쉬고↔가고는 자모 둘
        assertTrue(MeaningResolver.resolve("화장실 가고 싶어요", emptyList(), reg).isEmpty())
    }

    @Test
    fun jamoDistance() {
        assertTrue(MeaningResolver.oneJamoApart("아람", "바람"))
        assertTrue(MeaningResolver.oneJamoApart("바라", "바람"))      // 받침 빠짐
        assertTrue(!MeaningResolver.oneJamoApart("사랑", "바람"))     // 둘 다름
        assertTrue(!MeaningResolver.oneJamoApart("물", "불"))         // 자모 3개 미만 표현은 제외
    }

    @Test
    fun emptyWhenNothingRegisteredOrNoMatch() {
        assertTrue(MeaningResolver.resolve("바람", emptyList(), emptyList()).isEmpty())
        val walk = meaning("산책하고 싶어요")
        assertTrue(MeaningResolver.resolve("전혀 다른 말", emptyList(), listOf(entry(walk, "바람"))).isEmpty())
    }
}
