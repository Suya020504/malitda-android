package kr.voicemate.malitda.domain

import kr.voicemate.malitda.data.db.MeaningEntity

data class VoiceCandidateOption(val text: String, val meaning: MeaningEntity? = null, val remembered: Boolean = false)

/** Raw speech remains a separate visible choice; history and dictionary share the same 1..3 slots. */
object VoiceCandidateOptions {
    fun build(profileId: Long, raw: String, meanings: List<MeaningEntity>, candidates: List<Candidate>,
        alternatives: List<String>, count: Int): List<VoiceCandidateOption> {
        if (profileId <= 0) return emptyList()
        val usable = meanings.filter { it.profileId == profileId && it.active && (it.confirmed || it.isSample) }
        val result = mutableListOf<VoiceCandidateOption>()
        val rawKey = TextNormalizer.normalize(raw)
        val seen = mutableSetOf<String>()
        fun add(option: VoiceCandidateOption) {
            val key = TextNormalizer.normalize(option.text)
            if (key.isNotBlank() && option.text.length <= 200 &&
                (option.meaning != null || key != rawKey) && seen.add(key)) result += option
        }
        candidates.firstOrNull { it.source == CandidateSource.M1_EXACT }?.let { saved ->
            add(VoiceCandidateOption(saved.text, usable.firstOrNull {
                TextNormalizer.normalize(it.text) == TextNormalizer.normalize(saved.text)
            }, remembered = true))
        }
        usable.forEach { add(VoiceCandidateOption(it.text, it)) }
        candidates.filter { it.source != CandidateSource.RAW }.forEach { add(VoiceCandidateOption(it.text, remembered = it.isM1)) }
        alternatives.forEach { add(VoiceCandidateOption(it)) }
        return result.take(count.coerceIn(1, 3))
    }
}
