package kr.voicemate.malitda.domain

data class EasyTextSuggestion(val original: String, val text: String)

/** A bounded reading aid, never a substitute for the person's approval. */
object EasyText {
    private val endings = linkedMapOf(
        "싶습니다" to "싶어요", "않습니다" to "않아요", "없습니다" to "없어요",
        "있습니다" to "있어요", "아닙니다" to "아니에요", "필요합니다" to "필요해요",
        "괜찮습니다" to "괜찮아요", "모르겠습니다" to "모르겠어요",
        "어렵습니다" to "어려워요", "가능합니다" to "할 수 있어요",
    )
    private val phrases = linkedMapOf(
        "휴식을 취하고" to "쉬고", "도움을 요청하고" to "도와 달라고 말하고",
        "이용 방법" to "쓰는 방법", "사용 방법" to "쓰는 방법",
    )
    private val quotationOrLink = Regex("[\\\"'“”‘’「」『』`@]|https?://|www\\.", RegexOption.IGNORE_CASE)
    private val digits = Regex("\\d+(?:[.,:/-]\\d+)*")

    fun suggest(original: String): EasyTextSuggestion? {
        if (original.isBlank() || original.length > 200 || quotationOrLink.containsMatchIn(original)) return null
        var proposed = original
        // Known phrases only. Names, dates, personal aliases and unknown words are not inferred.
        for ((from, to) in phrases) proposed = proposed.replace(
            Regex("(?<![\\p{L}\\p{N}_])" + Regex.escape(from) + "(?=\\s|[을를이가은는]|[.!?]|$)"), to)
        for ((from, to) in endings) proposed = proposed.replace(
            Regex("(?<![\\p{L}\\p{N}_])" + Regex.escape(from) + "(?=[.!?…]*(?:\\s|$))"), to)
        if (proposed == original || proposed.length > 200 ||
            digits.findAll(original).map { it.value }.toList() != digits.findAll(proposed).map { it.value }.toList() ||
            CommunicationSafety.sensitiveChange(original, proposed)) return null
        return EasyTextSuggestion(original, proposed)
    }
}
