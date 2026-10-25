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

    data class Match(
        val meaning: MeaningEntity, val matchedExpression: String, val score: Int,
        val lowConfidence: Boolean = false, val matchReason: String = "exact",
    )

    private val WORD_SEP = Regex("[\\s,.!?…~]+")

    fun resolve(
        raw: String, alternatives: List<String>, registered: List<MeaningWithExpressions>,
        context: String? = null, maxCandidates: Int = 3, fuzzyThreshold: Int = 30,
        approvedTextsNormalized: Set<String> = emptySet(), now: Long = System.currentTimeMillis(),
    ): List<Match> {
        val inputs = (listOf(raw) + alternatives)
            .map { TextNormalizer.comparison(it) }
            .filter { it.isNotBlank() }
            .distinct()
        if (inputs.isEmpty() || registered.isEmpty()) return emptyList()

        val best = HashMap<Long, Match>()
        registered.forEach { entry ->
            if (!entry.meaning.active || (!entry.meaning.confirmed && !entry.meaning.isSample)) return@forEach
            entry.expressions.forEach { expr ->
                val key = TextNormalizer.comparison(expr.key)
                if (key.isBlank()) return@forEach
                // 음성 인식기는 띄어쓰기를 자주 빼먹는다("쉬고 싶어" → "쉬고싶어"). 공백을 뺀 형태로도 비교한다.
                val compactKey = key.filterNot { it.isWhitespace() }
                inputs.forEachIndexed { rank, input ->
                    val compactInput = input.filterNot { it.isWhitespace() }
                    val identifier = MeaningCategory.isIdentifier(entry.meaning.category)
                    val numericCompatible = numericParts(input) == numericParts(key)
                    val score = when {
                        input == key || compactInput == compactKey -> 100
                        identifier && key in input.split(WORD_SEP) -> 60 + key.length.coerceAtMost(30)
                        identifier || !numericCompatible -> 0
                        key.length >= 2 && input.contains(key) -> 60 + key.length.coerceAtMost(30)
                        compactKey.length >= 2 && compactInput.contains(compactKey) -> 60 + compactKey.length.coerceAtMost(30)
                        // 한 글자 표현("물")은 띄어 쓴 한 단어일 때만 맞춘다 — "물론" 같은 오탐 방지.
                        key.length == 1 && key in input.split(WORD_SEP) -> 60 + key.length
                        input.length >= 2 && key.contains(input) -> 40 + input.length.coerceAtMost(30)
                        oneJamoApart(input, compactKey) && FUZZY_SCORE >= fuzzyThreshold -> FUZZY_SCORE
                        generalEditCandidate(compactInput, compactKey) && GENERAL_EDIT_SCORE >= fuzzyThreshold -> GENERAL_EDIT_SCORE
                        else -> 0
                    }
                    if (score > 0) {
                        // 원문(raw)이 복수 후보보다 먼저 오므로 rank가 낮을수록 우선.
                        // 상황은 이미 표현이 맞은 뜻의 순서만 조정한다. 상황만으로 새 뜻을 만들지 않는다.
                        val adjusted = score - rank + contextBonus(context, entry.meaning.situation) +
                            historyBonus(entry.meaning, approvedTextsNormalized, now)
                        val prev = best[entry.meaning.id]
                        if (prev == null || adjusted > prev.score) {
                            best[entry.meaning.id] = Match(
                                entry.meaning, expr.text, adjusted,
                                lowConfidence = score <= FUZZY_SCORE,
                                matchReason = when {
                                    score == FUZZY_SCORE -> "jamo"
                                    score == GENERAL_EDIT_SCORE -> "edit"
                                    score == 100 -> "exact"
                                    else -> "partial"
                                },
                            )
                        }
                    }
                }
            }
        }
        return best.values.sortedWith(
            compareByDescending<Match> {
                when (it.matchReason) { "exact" -> 3; "partial" -> 2; "jamo" -> 1; else -> 0 }
            }.thenBy { it.meaning.isSample }.thenByDescending { it.score }.thenBy { it.meaning.id }
        ).take(maxCandidates.coerceIn(1, 3))
    }

    /** 반복 사용도 최대 3점, 최근 선택 최대 3점, 이전 명시 교정 최대 2점이다. */
    private fun historyBonus(meaning: MeaningEntity, approvedTextsNormalized: Set<String>, now: Long): Int {
        val frequency = meaning.useCount.coerceIn(0, 3)
        val age = now - meaning.lastUsedAt
        val recency = when {
            meaning.lastUsedAt <= 0 || age < 0 -> 0
            age <= 7L * 24 * 60 * 60 * 1000 -> 3
            age <= 30L * 24 * 60 * 60 * 1000 -> 1
            else -> 0
        }
        val correction = if (TextNormalizer.normalize(meaning.text) in approvedTextsNormalized) 2 else 0
        return (frequency + recency + correction).coerceAtMost(8)
    }

    private fun numericParts(text: String): List<String> = Regex("\\d+(?:[.:]\\d+)*").findAll(text).map { it.value }.toList()

    private fun generalEditCandidate(input: String, key: String): Boolean {
        if (input.length < 6 || key.length < 6 || kotlin.math.abs(input.length - key.length) > 1) return false
        if (MeaningSafetyReview.warnings(input, key).isNotEmpty()) return false
        return stringSimilarity(input, key) >= 1.0 - 1.0 / maxOf(input.length, key.length)
    }

    /** 일반 문자 편집거리를 0~1로 정규화한다. 비교용이며 새 문장을 생성하지 않는다. */
    internal fun stringSimilarity(left: String, right: String): Double {
        val a = left.take(Limits.MAX_TEXT_LENGTH)
        val b = right.take(Limits.MAX_TEXT_LENGTH)
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            previous = current
        }
        return 1.0 - previous[b.length].toDouble() / maxOf(a.length, b.length)
    }

    private fun contextBonus(context: String?, situation: String?): Int {
        val selected = TextNormalizer.normalize(context.orEmpty())
        val saved = TextNormalizer.normalize(situation.orEmpty())
        if (selected.isBlank() || saved.isBlank()) return 0
        val selectedTokens = selected.split(WORD_SEP).filter { it.length >= 2 }.toSet()
        val savedTokens = saved.split(WORD_SEP).filter { it.length >= 2 }.toSet()
        return if (selected == saved || selectedTokens.intersect(savedTokens).isNotEmpty()) 12 else 0
    }

    /** ④ 순위 점수: 다른 모든 규칙(40 이상)보다 낮다. */
    private const val FUZZY_SCORE = 30
    private const val GENERAL_EDIT_SCORE = 28
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
