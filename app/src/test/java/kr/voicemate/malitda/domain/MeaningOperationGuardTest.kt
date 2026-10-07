package kr.voicemate.malitda.domain

import org.junit.Assert.*
import org.junit.Test

class MeaningOperationGuardTest {
    @Test fun rejectionInvalidatesAnApprovalWaitingForDatabase() {
        val guard = MeaningOperationGuard()
        val approval = guard.begin(1)
        guard.begin(1) // 학생이 '다른 뜻이에요'를 선택함
        assertFalse(guard.accepts(approval, 1))
    }
    @Test fun reselectionInvalidatesAnEarlierShareOrSpeech() {
        val guard = MeaningOperationGuard()
        val delivery = guard.begin(1)
        val selection = guard.begin(1)
        assertFalse(guard.accepts(delivery, 1))
        assertTrue(guard.accepts(selection, 1))
    }
    @Test fun SwitchingAwayAndBackDoesNotReviveOldApproval() {
        val guard = MeaningOperationGuard()
        val old = guard.begin(1)
        guard.begin(2)
        guard.begin(1)
        assertFalse(guard.accepts(old, 1))
    }
    @Test fun DifferentStudentIsNeverAccepted() {
        val guard = MeaningOperationGuard()
        assertFalse(guard.accepts(guard.begin(1), 2))
    }
}
