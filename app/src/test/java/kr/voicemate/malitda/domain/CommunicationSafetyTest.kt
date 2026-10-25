package kr.voicemate.malitda.domain

import org.junit.Assert.*
import org.junit.Test

class CommunicationSafetyTest {
    @Test fun literalApprovalCannotCrossPersonOrEdit() {
        val approved = LiteralApproval(4, "물 주세요", 12)
        assertTrue(approved.permits(4, "물 주세요", 12))
        assertFalse(approved.permits(5, "물 주세요", 12))
        assertFalse(approved.permits(4, "물 주세요!", 12))
        assertFalse(approved.permits(4, "물 주세요", 13))
    }
    @Test fun numbersAndNegationAreNeverSilentlyCorrected() {
        assertTrue(CommunicationSafety.sensitiveChange("3시 안 가요", "4시 가요"))
        assertFalse(CommunicationSafety.sensitiveChange("바람", "산책하고 싶어요"))
        assertFalse(CommunicationSafety.sensitiveChange(null, "물 주세요"))
    }
}
