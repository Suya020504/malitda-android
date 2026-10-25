package kr.voicemate.malitda.domain

/** 숫자·시간·부정 표현 변경은 뜻이 같다는 보장이 없어 학생에게 다시 확인하도록 표시한다. */
object MeaningSafetyReview {
    private val numbers = Regex("\\d+(?:[.,:]\\d+)*")
    private val negative = Regex("(?:^|\\s)안\\s|않|아니|없|못(?:\\s|해|하|가|먹|마시|하겠)|싫")

    fun warnings(raw: String, candidate: String): List<String> = buildList {
        if (raw.isBlank()) return@buildList
        val rawNumbers = numbers.findAll(TextNormalizer.comparison(raw)).map { it.value }.toList()
        val candidateNumbers = numbers.findAll(TextNormalizer.comparison(candidate)).map { it.value }.toList()
        if (rawNumbers != candidateNumbers && (rawNumbers.isNotEmpty() || candidateNumbers.isNotEmpty())) {
            add("숫자나 시간이 달라졌습니다. 전할 뜻이 맞는지 다시 확인해야 합니다.")
        }
        if (negative.containsMatchIn(raw) != negative.containsMatchIn(candidate)) {
            add("아니라는 뜻이 바뀌었을 수 있습니다. 본인의 뜻을 다시 확인해야 합니다.")
        }
    }
}
