package kr.voicemate.malitda.domain

import org.junit.Assert.*
import org.junit.Test

class CorrectionLearningTest {
    private val approval = LiteralApproval(1, "물을 주세요", 7)
    @Test fun confirmedCorrectionCanBeExplicitlyRemembered() {
        assertEquals(CorrectionLearningSnapshot(1, "무를 주세요", "물을 주세요", 7),
            CorrectionLearning.prepare(approval, 1, "무를 주세요", "물을 주세요", 7, true))
    }
    @Test fun noApprovalOrNoConsentCannotCreateHistory() {
        assertNull(CorrectionLearning.prepare(null, 1, "무를 주세요", "물을 주세요", 7, true))
        assertNull(CorrectionLearning.prepare(approval, 1, "무를 주세요", "물을 주세요", 7, false))
    }
    @Test fun differentPersonCannotReuseApproval() {
        assertNull(CorrectionLearning.prepare(approval, 2, "무를 주세요", "물을 주세요", 7, true))
    }
    @Test fun editingOrReturningAfterAnotherRevisionRevokesRemembering() {
        assertNull(CorrectionLearning.prepare(approval, 1, "무를 주세요", "약을 주세요", 7, true))
        assertNull(CorrectionLearning.prepare(approval, 1, "무를 주세요", "물을 주세요", 8, true))
    }
    @Test fun unchangedOrDirectInputDoesNotBecomeRecognitionHistory() {
        assertNull(CorrectionLearning.prepare(approval, 1, " 물을 주세요! ", "물을 주세요", 7, true))
        assertNull(CorrectionLearning.prepare(approval, 1, null, "물을 주세요", 7, true))
        assertNull(CorrectionLearning.prepare(approval, 1, "...", "물을 주세요", 7, true))
    }
    @Test fun oldDialogCannotRememberAnotherRawTranscript() {
        val pending = CorrectionLearningSnapshot(1, "무를 주세요", "물을 주세요", 7)
        assertTrue(CorrectionLearning.isCurrent(pending, approval, 1, "무를 주세요", "물을 주세요", 7, true))
        assertFalse(CorrectionLearning.isCurrent(pending, approval, 1, "불을 주세요", "물을 주세요", 7, true))
    }
    @Test fun invalidPersonOrOversizedRawCannotBeSaved() {
        assertNull(CorrectionLearning.prepare(LiteralApproval(0, "물을 주세요", 7), 0, "무를 주세요", "물을 주세요", 7, true))
        assertNull(CorrectionLearning.prepare(approval, 1, "가".repeat(201), "물을 주세요", 7, true))
    }
}
