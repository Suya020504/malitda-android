package kr.voicemate.malitda.domain

/** A literal message is usable only for the same person, exact text and editing revision. */
data class LiteralApproval(val profileId: Long, val text: String, val revision: Long) {
    fun permits(profile: Long, currentText: String, currentRevision: Long): Boolean =
        text.isNotBlank() && profileId == profile && text == currentText && revision == currentRevision
}

object CommunicationSafety {
    /** Highlight changes that deserve another check; never infer or repair the intended facts. */
    fun sensitiveChange(raw: String?, proposed: String): Boolean {
        if (raw.isNullOrBlank()) return false
        val numbers = Regex("\\d+(?:[.,:]\\d+)*")
        val negatives = Regex("안\\s|않|못|없|아니|싫")
        return numbers.findAll(raw).map { it.value }.toList() != numbers.findAll(proposed).map { it.value }.toList() ||
            negatives.containsMatchIn(raw) != negatives.containsMatchIn(proposed)
    }
}
