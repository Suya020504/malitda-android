package kr.voicemate.malitda.domain

import kr.voicemate.malitda.data.db.TrialLogEntity
import org.junit.Assert.*
import org.junit.Test

class PilotSafetyTest {
    @Test fun cancelledVoiceDoesNotBecomeTheNextCardTrial() {
        val tracker = TrialTracker()
        val config = PilotConfig(participantCode = "P001", consented = true, practice = false, suitabilityReviewed = true, consentVersion = PilotConfig.CONSENT_VERSION, consentedAt = 1, targetMeaningId = 1)
        tracker.startNew(TrialTracker.Origin.VOICE, 1_000, config)
        val stopped = tracker.cancel(1, 2_000, PilotStopReason.PARTICIPANT_REQUEST)!!
        tracker.startNew(TrialTracker.Origin.CARD, 3_000, config)
        val next = tracker.finish(1, 5_000)!!
        assertEquals("ABORTED", stopped.outcome)
        assertEquals("CARD", next.origin)
        assertEquals(3_000L, next.startedAt)
        assertFalse(next.usedCardFallback)
    }

    @Test fun exportHasOnlySelectedConsentedCompletedNonPracticeRecordsAndNoSentences() {
        val config = PilotConfig(participantCode = "P001", consented = true, practice = false, suitabilityReviewed = true, consentVersion = PilotConfig.CONSENT_VERSION, consentedAt = 1, targetMeaningId = 1)
        val t = TrialTracker()
        t.startNew(TrialTracker.Origin.CARD, 1000, config)
        t.onDelivered(1, "비밀 문장")
        val a = t.finish(10, 3000)!!
        val csv = TrialCsv.of(listOf(a, a.copy(profileId = 20), a.copy(consented = false),
            a.copy(outcome = "ABORTED"), a.copy(practice = true), a.copy(participantCode = "P002")),
            TrialExportScope(10, "P001", true))
        assertEquals(2, csv.trim().lines().size)
        assertTrue(csv.lines()[1].startsWith("P001,"))
        assertFalse(csv.contains("비밀"))
        assertTrue(csv.lineSequence().first().split(',').none { it in setOf("meaning", "meaning_text", "profile", "name", "started_at") })
        assertFalse(csv.contains("started"))
        assertNull(a.meaningText)
    }

    @Test fun unsuccessfulFinishedTaskIsNotMistakenForAnUnfinishedTask() {
        val t = TrialTracker()
        t.startNew(TrialTracker.Origin.VOICE, 0,
            PilotConfig(participantCode = "P002", consented = true, practice = false, suitabilityReviewed = true, consentVersion = PilotConfig.CONSENT_VERSION, consentedAt = 1, targetMeaningId = 1))
        t.onVoiceResult(null)
        val log = t.finish(2, 1000)!!
        assertFalse(log.delivered)
        assertEquals(1, TrialCsv.exportable(listOf(log), TrialExportScope(2, "P002", true)).size)
    }

    @Test fun revokedConsentAndInvalidCodesFailClosed() {
        val row = TrialLogEntity(profileId = 1, startedAt = 0, endedAt = 1, origin = "CARD",
            voiceFirstHit = false, usedCardFallback = false, otherMeaningCount = 0,
            delivered = false, understood = -1, supporterHelped = false, meaningId = null, meaningText = "secret",
            participantCode = "P001", consented = true, practice = false, targetMeaningId = 1)
        assertTrue(TrialCsv.exportable(listOf(row), TrialExportScope(1, "P001", false)).isEmpty())
        assertFalse(PilotConfig.validParticipantCode("홍길동"))
        assertFalse(PilotConfig.validParticipantCode("=1+2"))
        assertFalse(PilotConfig.validTaskCode("=NOW()"))
    }

    @Test fun balancedOrderAndStopRuleAreDeterministic() {
        assertEquals(listOf("A", "B", "C"), PilotPlan.order("P001"))
        assertEquals(listOf("B", "C", "A"), PilotPlan.order("P002"))
        assertEquals(listOf("C", "A", "B"), PilotPlan.order("P003"))
        assertTrue(PilotPlan.shouldPause(consecutiveFailures = 3))
        assertTrue(PilotPlan.shouldPause(consecutiveFailures = 0, participantAsked = true))
        assertFalse(PilotPlan.shouldPause(consecutiveFailures = 2))
    }

    @Test fun configurationUsesExplicitConsentVersionAndNoAutomaticRealTrial() {
        val initial = PilotConfig()
        assertFalse(initial.canCollect)
        assertTrue(initial.practice)
        assertFalse(initial.consented)
        val real = initial.copy(participantCode = "P001", taskCode = "T01", consented = true,
            consentVersion = PilotConfig.CONSENT_VERSION, consentedAt = 100,
            targetMeaningId = 1, practice = false, suitabilityReviewed = true)
        assertTrue(real.canCollect)
        assertFalse(real.copy(consentVersion = "old").canCollect)
    }
    @Test fun observedFailedCompletionRemainsInTheCsvDenominator() {
        val config = PilotConfig(participantCode = "P011", consented = true, practice = false,
            suitabilityReviewed = true, consentVersion = PilotConfig.CONSENT_VERSION, consentedAt = 1, targetMeaningId = 11)
        val tracker = TrialTracker()
        tracker.startNew(TrialTracker.Origin.VOICE, 1000, config)
        tracker.setJudgedCorrect(false)
        tracker.setUnderstood(false)
        val log = tracker.finish(3, 2000)!!
        assertEquals("COMPLETED", log.outcome)
        assertFalse(log.delivered)
        assertEquals(0, log.judgedCorrect)
        assertEquals(1, TrialSummary.of(listOf(log)).completed)
        assertEquals(1, TrialCsv.exportable(listOf(log), TrialExportScope(3, "P011", true)).size)
    }
    @Test fun completedTrialAdvancesTheCounterbalancedSuggestion() {
        val initial = PilotConfig(participantCode = "P001")
        val next = PilotPlan.afterCompleted(initial, false)
        assertEquals(1, next.ordinal)
        assertEquals("B", next.recommendedCondition)
        assertFalse(next.paused)
        assertTrue(PilotPlan.afterCompleted(next.copy(consecutiveFailures = 2), false).paused)
    }

    @Test fun receiverObservationTakesPriorityAndNoResponseIsNotFailure() {
        assertEquals(false, PilotPlan.observedSuccess(0, 1))
        assertEquals(true, PilotPlan.observedSuccess(1, 0))
        assertEquals(false, PilotPlan.observedSuccess(-1, 0))
        assertNull(PilotPlan.observedSuccess(-1, -1))
    }

    @Test fun aSampleCardNeverCountsAsARealPersonalizedTrial() {
        val tracker = TrialTracker()
        tracker.startNew(TrialTracker.Origin.CARD, 1,
            PilotConfig(participantCode = "P001", consented = true, practice = false,
                suitabilityReviewed = true, consentVersion = PilotConfig.CONSENT_VERSION, consentedAt = 1, targetMeaningId = 7))
        tracker.onDelivered(7, isSample = true)
        val log = tracker.finish(3, 100)!!
        assertTrue(log.practice)
        assertEquals(0, TrialSummary.of(listOf(log)).completed)
        assertTrue(TrialCsv.exportable(listOf(log), TrialExportScope(3, "P001", true)).isEmpty())
    }

}
