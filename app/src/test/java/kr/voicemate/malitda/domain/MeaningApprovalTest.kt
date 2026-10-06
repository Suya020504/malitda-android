package kr.voicemate.malitda.domain

import org.junit.Assert.*
import org.junit.Test

class MeaningApprovalTest {
    private val approved = MeaningApprovalSnapshot.capture(1, 7, 2, "산책하고 싶어요", "builtin:walk")
    @Test fun sameConfirmedMeaningIsValid() { assertTrue(approved.matches(1, 7, 2, "산책하고 싶어요", "builtin:walk", "CONFIRMED")) }
    @Test fun switchingStudentInvalidates() { assertFalse(approved.matches(2, 7, 2, "산책하고 싶어요", "builtin:walk", "CONFIRMED")) }
    @Test fun sameWordsAnotherMeaningAreNotApproval() { assertFalse(approved.matches(1, 8, 2, "산책하고 싶어요", "builtin:walk", "CONFIRMED")) }
    @Test fun editedVersionInvalidates() { assertFalse(approved.matches(1, 7, 3, "산책하고 싶어요", "builtin:walk", "CONFIRMED")) }
    @Test fun editedWordsOrImageInvalidate() {
        assertFalse(approved.matches(1, 7, 2, "산책하기 싫어요", "builtin:walk", "CONFIRMED"))
        assertFalse(approved.matches(1, 7, 2, "산책하고 싶어요", "file:meaning_media/new.png", "CONFIRMED"))
    }
    @Test fun draftAndInactiveCannotDeliver() { for (s in listOf("DRAFT", "INACTIVE")) assertFalse(approved.matches(1, 7, 2, "산책하고 싶어요", "builtin:walk", s)) }
}
