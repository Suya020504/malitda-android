package kr.voicemate.malitda.domain

import kr.voicemate.malitda.data.db.MeaningEntity
import kr.voicemate.malitda.data.db.MeaningWithExpressions

/**
 * v2 의미 후보 결정(아키텍처 변경도의 MeaningResolver).
 * 인식 원문·복수 후보를 정규화해 등록된 표현(key)과 맞춰 본다.
 * 결정론적 규칙만 쓴다: ① 완전 일치 ② 인식문이 표현을 포함(한 글자 표현은 독립된 단어일 때만) ③ 표현이 인식문을 포함.
 * 유사도·편집거리는 쓰지 않는다(기존 M1과 같은 원칙).
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
                inputs.forEachIndexed { rank, input ->
                    val score = when {
                        input == key -> 100
                        key.length >= 2 && input.contains(key) -> 60 + key.length
                        // 한 글자 표현("물")은 띄어 쓴 한 단어일 때만 맞춘다 — "물론" 같은 오탐 방지.
                        key.length == 1 && key in input.split(WORD_SEP) -> 60 + key.length
                        input.length >= 2 && key.contains(input) -> 40 + input.length
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
}
