package kr.voicemate.malitda.domain

enum class StudentMeaningResponse { MATCHED, DIFFERENT, UNSURE, NO_RESPONSE }
enum class StudentConfirmationMethod(val storageKey: String) {
    SPEECH("speech"), POINTING("pointing"), GAZE("gaze"), OTHER("other")
}

/** 지원자의 추측이나 무응답은 학생의 뜻 확인으로 처리하지 않는다. */
data class StudentMeaningConfirmation(
    val response: StudentMeaningResponse? = null,
    val method: StudentConfirmationMethod? = null,
) {
    val canActivate: Boolean get() = response == StudentMeaningResponse.MATCHED && method != null
}

/** 이번 전달 승인. 학생·뜻·수정본·그림에 묶고 프로세스 종료 뒤 복원하지 않는다. */
data class MeaningDeliverySnapshot(
    val profileId: Long, val meaningId: Long, val updatedAt: Long,
    val text: String, val imageKey: String?,
) {
    fun matches(current: MeaningDeliverySnapshot?): Boolean = current != null && this == current
}
