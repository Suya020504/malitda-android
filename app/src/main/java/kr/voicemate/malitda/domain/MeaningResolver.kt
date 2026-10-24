package kr.voicemate.malitda.domain

import kr.voicemate.malitda.data.db.MeaningEntity
import kr.voicemate.malitda.data.db.MeaningWithExpressions

/**
 * v2 의미 후보 결정(아키텍처 변경도의 MeaningResolver).
 * 인식 원문·복수 후보를 정규화해 등록된 표현(key)과 맞춰 본다.
 * 결정론적 규칙만 쓴다: ① 완전 일치 ② 인식문이 표현을 포함(한 글자 표현은 독립된 단어일 때만) ③ 표현이 인식문을 포함
 * ④ 위가 모두 아니면, 한글 자모 하나만 다른 말(인식기 오인식: "아람"↔"바람", "못말라"↔"목말라")을 가장 낮은 순위 후보로 낸다.
 * ④는 뜻을 새로 만들지 않고 등록된 뜻을 '후보'로만 보여 준다 — 학생이 '이 뜻이 맞나요?'에서 확인해야 전달된다.
 * 짧은 표현(자모 4개 미만, 예: "물")은 ④에서 제외해 엉뚱한 연결을 막는다.
 */
object MeaningResolver {

    data class Match(val meaning: MeaningEntity, val matchedExpression: String, val score: Int)

    private val WORD_SEP = Regex("[\\s,.!?…~]+")

    fun resolve(raw: String, alternatives: List<String>, registered: List<MeaningWithExpressions>): List<Match> {
        val inputs = (listOf(raw) + alternatives)
            .map { TextNormalizer.normalize(it) }
            .filter { it.isNotBlank() }
            .distinct()
        if (inputs.isEmpty() || registered.isEmpty()) return emptyList()

        val best = HashMap<Long, Match>()
        registered.forEach { entry ->
            entry.expressions.forEach { expr ->
                val key = expr.key
                if (key.isBlank()) return@forEach
                // 음성 인식기는 띄어쓰기를 자주 빼먹는다("쉬고 싶어" → "쉬고싶어"). 공백을 뺀 형태로도 비교한다.
                val compactKey = key.filterNot { it.isWhitespace() }
                inputs.forEachIndexed { rank, input ->
                    val compactInput = input.filterNot { it.isWhitespace() }
                    val score = when {
                        input == key || compactInput == compactKey -> 100
                        key.length >= 2 && input.contains(key) -> 60 + key.length
                        compactKey.length >= 2 && compactInput.contains(compactKey) -> 60 + compactKey.length
                        // 한 글자 표현("물")은 띄어 쓴 한 단어일 때만 맞춘다 — "물론" 같은 오탐 방지.
                        key.length == 1 && key in input.split(WORD_SEP) -> 60 + key.length
                        input.length >= 2 && key.contains(input) -> 40 + input.length
                        oneJamoApart(input, compactKey) -> FUZZY_SCORE
                        else -> 0
                    }
                    if (score > 0) {
                        // 원문(raw)이 복수 후보보다 먼저 오므로 rank가 낮을수록 우선.
                        val adjusted = score - rank
                        val prev = best[entry.meaning.id]
                        if (prev == null || adjusted > prev.score) {
                            best[entry.meaning.id] = Match(entry.meaning, expr.text, adjusted)
                        }
                    }
                }
            }
        }
        return best.values.sortedWith(compareByDescending<Match> { it.score }.thenByDescending { it.meaning.useCount })
    }

    /** ④ 순위 점수: 다른 모든 규칙(40 이상)보다 낮다. */
    private const val FUZZY_SCORE = 30
    /** ④를 적용할 최소 자모 수. "물"(3)은 제외, "바람"(5)·"추워"(4)부터. */
    private const val FUZZY_MIN_JAMO = 4

    /** 인식문 전체 또는 그 안의 한 단어가 표현과 자모 하나(바꾸기·넣기·빼기)만 다른가. 공백은 무시한다. */
    internal fun oneJamoApart(input: String, compactKey: String): Boolean {
        val key = jamo(compactKey)
        if (key.size < FUZZY_MIN_JAMO) return false
        val candidates = listOf(input.filterNot { it.isWhitespace() }) + input.split(WORD_SEP).filter { it.isNotBlank() }
        return candidates.distinct().any { editDistanceAtMost1(jamo(it), key) }
    }

    /** 한글 음절을 초성·중성·종성으로 나눈다. 같은 자음이라도 초성과 종성은 다른 기호로 둔다. 한글이 아닌 글자는 그대로. */
    internal fun jamo(s: String): List<Int> {
        val out = ArrayList<Int>(s.length * 3)
        for (ch in s) {
            val code = ch.code
            if (code in 0xAC00..0xD7A3) {
                val idx = code - 0xAC00
                out += 0x10000 + idx / 588             // 초성
                out += 0x20000 + (idx % 588) / 28      // 중성
                if (idx % 28 != 0) out += 0x30000 + idx % 28 // 종성
            } else {
                out += code
            }
        }
        return out
    }

    private fun editDistanceAtMost1(a: List<Int>, b: List<Int>): Boolean {
        if (a == b) return true
        val (s, l) = if (a.size <= b.size) a to b else b to a
        if (l.size - s.size > 1) return false
        var i = 0
        while (i < s.size && s[i] == l[i]) i++
        return if (s.size == l.size) s.subList(i + 1, s.size) == l.subList(i + 1, l.size)  // 바꾸기 1
        else s.subList(i, s.size) == l.subList(i + 1, l.size)                               // 넣기/빼기 1
    }
}
