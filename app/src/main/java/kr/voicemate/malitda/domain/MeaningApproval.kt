package kr.voicemate.malitda.domain

/** 이번 대화에서 확인한 의미의 스냅샷. 등록 확인과 별개이며 영구 저장하지 않는다. */
data class MeaningApprovalSnapshot(
    val profileId: Long,
    val meaningId: Long,
    val version: Long,
    val textHash: String,
    val imageRef: String?,
) {
    fun matches(profileId: Long, meaningId: Long, version: Long, text: String, imageRef: String?, status: String): Boolean =
        status == "CONFIRMED" && this.profileId == profileId && this.meaningId == meaningId &&
            this.version == version && textHash == Approval.token(text) && this.imageRef == imageRef

    companion object {
        fun capture(profileId: Long, meaningId: Long, version: Long, text: String, imageRef: String?) =
            MeaningApprovalSnapshot(profileId, meaningId, version, Approval.token(text), imageRef)
    }
}
