package kr.voicemate.malitda.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrialTrackerTest {

    @Test
    fun voiceFirstHitWhenFirstCandidateIsDelivered() {
        val t = TrialTracker()
        t.start(TrialTracker.Origin.VOICE, 1_000)
        t.onVoiceResult(7)
        t.onDelivered(7, "산책하고 싶어요")
        t.setUnderstood(true)
        val log = t.finish(profileId = 1, now = 6_000)!!
        assertTrue(log.voiceFirstHit)
        assertTrue(log.delivered)
        assertEquals(1, log.understood)
        assertEquals("VOICE", log.origin)
        assertNull(t.draft)
    }

    @Test
    fun cardFallbackAfterVoiceIsNotAFirstHit() {
        val t = TrialTracker()
        t.start(TrialTracker.Origin.VOICE, 0)
        t.onVoiceResult(null) // 뜻을 못 찾음
        t.onCardPicked()
        t.onDelivered(3, "바람이 차가워요")
        val log = t.finish(1, 1)!!
        assertFalse(log.voiceFirstHit)
        assertTrue(log.usedCardFallback)
        assertTrue(log.delivered)
        assertEquals(-1, log.understood)
    }

    @Test
    fun otherMeaningCountsAndBlocksFirstHit() {
        val t = TrialTracker()
        t.start(TrialTracker.Origin.VOICE, 0)
        t.onVoiceResult(5)
        t.onOtherMeaning()
        t.onCardPicked()
        t.onDelivered(5, "물이 필요해요")
        val log = t.finish(1, 1)!!
        assertEquals(1, log.otherMeaningCount)
        assertFalse(log.voiceFirstHit)
    }

    @Test
    fun retriesStayInOneTrialAndFirstResultIsKept() {
        val t = TrialTracker()
        t.start(TrialTracker.Origin.VOICE, 100)
        t.onVoiceResult(null)
        t.start(TrialTracker.Origin.VOICE, 200) // 다시 말하기: 같은 시도
        t.onVoiceResult(9)
        assertEquals(100L, t.draft!!.startedAt)
        assertNull(t.draft!!.firstVoiceMatchId) // 첫 결과(못 찾음)를 기억한다
    }

    @Test
    fun abandonedCardTrialAndNoTrial() {
        val t = TrialTracker()
        assertNull(t.finish(1, 0))
        t.start(TrialTracker.Origin.CARD, 0)
        t.onCardPicked() // 카드로 시작했으니 '카드로 바꿈'이 아니다
        t.setSupporterHelped(true)
        val log = t.finish(1, 1)!!
        assertFalse(log.delivered)
        assertFalse(log.usedCardFallback)
        assertTrue(log.supporterHelped)
    }

    @Test
    fun summaryAndCsv() {
        val t = TrialTracker()
        val config = PilotConfig(participantCode = "P001", consented = true, practice = false, suitabilityReviewed = true,
            consentVersion = PilotConfig.CONSENT_VERSION, consentedAt = 1, targetMeaningId = 1)
        t.startNew(TrialTracker.Origin.VOICE, 0, config); t.onVoiceResult(1); t.onDelivered(1, "산책, 하고 싶어요"); t.setUnderstood(true)
        val a = t.finish(1, 3_000)!!
        t.startNew(TrialTracker.Origin.CARD, 0, config); t.setUnderstood(false)
        val b = t.finish(1, 1_000)!!
        val s = TrialSummary.of(listOf(a, b))
        assertEquals(2, s.total)
        assertEquals(1, s.delivered)
        assertEquals(1, s.understoodYes)
        assertEquals(2, s.understoodAnswered)
        assertEquals(1, s.voiceFirstHit)
        val csv = TrialCsv.of(listOf(a), TrialExportScope(1L, "P001", true))
        val row = csv.lines()[1]
        assertTrue(row.startsWith("P001,T01,local-v1,C,3,voice,"))
        assertFalse(row.contains("산책"))
        assertFalse(csv.contains("지우"))
        assertEquals(TrialCsv.HEADER.split(',').size, row.split(',').size)
    }
}
