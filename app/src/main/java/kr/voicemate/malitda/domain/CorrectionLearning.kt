package kr.voicemate.malitda.domain

/** One explicit request to remember the exact correction the current person approved. */
data class CorrectionLearningSnapshot(val profileId: Long, val raw: String, val approved: String, val revision: Long)

object CorrectionLearning {
    fun prepare(approval: LiteralApproval?, profileId: Long, raw: String?, text: String,
        revision: Long, consentAccepted: Boolean): CorrectionLearningSnapshot? {
        if (!consentAccepted || profileId <= 0 || approval?.permits(profileId, text, revision) != true) return null
        val heard = raw?.trim().orEmpty()
        if (heard.isBlank() || heard.length > 200 || text.isBlank() || text.length > 200) return null
        val key = TextNormalizer.normalize(heard)
        if (key.isBlank() || key == TextNormalizer.normalize(text)) return null
        return CorrectionLearningSnapshot(profileId, heard, text, revision)
    }

    fun isCurrent(snapshot: CorrectionLearningSnapshot, approval: LiteralApproval?, profileId: Long,
        raw: String?, text: String, revision: Long, consentAccepted: Boolean): Boolean =
        snapshot == prepare(approval, profileId, raw, text, revision, consentAccepted)
}
