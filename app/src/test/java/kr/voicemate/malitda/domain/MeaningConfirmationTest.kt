package kr.voicemate.malitda.domain

import org.junit.Assert.*
import org.junit.Test

class MeaningConfirmationTest {
    @Test fun `activation needs a student match and a response method`() {
        assertFalse(StudentMeaningConfirmation().canActivate)
        assertFalse(StudentMeaningConfirmation(StudentMeaningResponse.MATCHED).canActivate)
        StudentMeaningResponse.entries.filter { it != StudentMeaningResponse.MATCHED }.forEach {
            assertFalse(StudentMeaningConfirmation(it, StudentConfirmationMethod.POINTING).canActivate)
        }
        assertTrue(StudentMeaningConfirmation(StudentMeaningResponse.MATCHED, StudentConfirmationMethod.GAZE).canActivate)
    }

    @Test fun `delivery approval is bound to student meaning version text and image`() {
        val approved = MeaningDeliverySnapshot(1, 2, 100, "산책하고 싶어요", "walk")
        assertTrue(approved.matches(approved.copy()))
        listOf(
            approved.copy(profileId = 2), approved.copy(meaningId = 3),
            approved.copy(updatedAt = 101), approved.copy(text = "쉬고 싶어요"),
            approved.copy(imageKey = "rest"),
        ).forEach { assertFalse(approved.matches(it)) }
        assertFalse(approved.matches(null))
    }
}
