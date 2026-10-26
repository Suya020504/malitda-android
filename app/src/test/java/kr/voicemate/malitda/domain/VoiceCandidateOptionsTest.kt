package kr.voicemate.malitda.domain

import kr.voicemate.malitda.data.db.MeaningEntity
import org.junit.Assert.*
import org.junit.Test

class VoiceCandidateOptionsTest {
    private fun meaning(id: Long, text: String, profile: Long = 1) = MeaningEntity(
        id = id, profileId = profile, text = text, imageKey = null, situation = null,
        confirmed = true, active = true, createdAt = 1, updatedAt = 1, useCount = 0)
    @Test fun rememberedCorrectionIsVisibleEvenWhenDictionaryFillsAllSlots() {
        val list = VoiceCandidateOptions.build(1, "무를 주세요", listOf(meaning(1,"산책"),meaning(2,"쉬기"),meaning(3,"간식")),
            listOf(Candidate("물을 주세요",CandidateSource.M1_EXACT)),emptyList(),3)
        assertEquals(3,list.size);assertEquals("물을 주세요",list.first().text);assertTrue(list.first().remembered)
    }
    @Test fun matchingDictionaryMeaningIsNotDuplicated() {
        val m=meaning(1,"물을 주세요")
        val list=VoiceCandidateOptions.build(1,"무를 주세요",listOf(m),listOf(Candidate(m.text,CandidateSource.M1_EXACT)),listOf(m.text),3)
        assertEquals(1,list.size);assertEquals(m,list.single().meaning)
    }
    @Test fun rawIsShownSeparatelyAndIsNeverAutoSelected() {
        val list=VoiceCandidateOptions.build(1,"물을 주세요",emptyList(),listOf(Candidate("물을 주세요",CandidateSource.RAW)),listOf("물을 주세요"),3)
        assertTrue(list.isEmpty())
    }
    @Test fun oneCandidatePreferenceStillOffersTheSavedCorrection() {
        val list=VoiceCandidateOptions.build(1,"raw",listOf(meaning(1,"other")),listOf(Candidate("corrected",CandidateSource.M1_EXACT)),emptyList(),1)
        assertEquals(listOf("corrected"),list.map{it.text})
    }
    @Test fun anotherStudentsMeaningIsExcluded() {
        assertTrue(VoiceCandidateOptions.build(1,"raw",listOf(meaning(1,"private",2)),emptyList(),emptyList(),3).isEmpty())
    }
    @Test fun pictureMeaningRemainsAvailableWhenSpeechAlreadyMatchesItsText() {
        val m=meaning(1,"물을 주세요")
        assertEquals(m,VoiceCandidateOptions.build(1,m.text,listOf(m),emptyList(),emptyList(),3).single().meaning)
    }
}
