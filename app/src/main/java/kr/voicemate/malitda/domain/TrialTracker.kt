package kr.voicemate.malitda.domain

import kr.voicemate.malitda.data.db.TrialLogEntity

/**
 * 실증 기록용 '대화 시도' 추적. 홈에서 말하기나 카드로 시작해 홈으로 돌아올 때까지를 한 번의 시도로 본다.
 * 다시 말하기·다른 뜻 고르기는 같은 시도 안의 재시도로 센다.
 * 화면 코드와 분리해 단위 테스트로 규칙을 검증한다.
 */
class TrialTracker {
    enum class Origin { VOICE, CARD }

    data class Draft(
        val startedAt: Long,
        val origin: Origin,
        val firstVoiceMatchId: Long? = null,
        /** 첫 말하기 결과를 이미 받았는가 — 결과가 '없음'(null)이었던 경우와 구분한다. */
        val voiceResultSeen: Boolean = false,
        val usedCardFallback: Boolean = false,
        val otherMeaningCount: Int = 0,
        val deliveredMeaningId: Long? = null,
        val deliveredMeaningText: String? = null,
        val understood: Int = -1,
        val supporterHelped: Boolean = false,
    ) {
        val delivered: Boolean get() = deliveredMeaningId != null
    }

    var draft: Draft? = null
        private set

    /** 이미 진행 중인 시도가 있으면 이어 간다(다시 말하기 등). */
    fun start(origin: Origin, now: Long) {
        if (draft == null) draft = Draft(startedAt = now, origin = origin)
    }

    /** 말하기 결과의 첫 후보(없으면 null). 이 시도에서 처음 나온 후보만 기억한다. */
    fun onVoiceResult(firstMatchId: Long?) {
        val d = draft ?: return
        if (!d.voiceResultSeen) draft = d.copy(firstVoiceMatchId = firstMatchId, voiceResultSeen = true)
    }

    /** 카드 목록에서 직접 골랐다. 말로 시작한 시도라면 카드로 바꾼 것으로 센다. */
    fun onCardPicked() {
        val d = draft ?: return
        if (d.origin == Origin.VOICE) draft = d.copy(usedCardFallback = true)
    }

    fun onOtherMeaning() {
        draft = draft?.let { it.copy(otherMeaningCount = it.otherMeaningCount + 1) }
    }

    fun onDelivered(meaningId: Long, meaningText: String) {
        draft = draft?.copy(deliveredMeaningId = meaningId, deliveredMeaningText = meaningText)
    }

    fun setUnderstood(understood: Boolean?) {
        draft = draft?.copy(understood = when (understood) { true -> 1; false -> 0; null -> -1 })
    }

    fun setSupporterHelped(helped: Boolean) {
        draft = draft?.copy(supporterHelped = helped)
    }

    /** 시도를 끝내고 저장할 기록을 돌려준다. 진행 중인 시도가 없으면 null. */
    fun finish(profileId: Long, now: Long): TrialLogEntity? {
        val d = draft ?: return null
        draft = null
        val voiceFirstHit = d.origin == Origin.VOICE && !d.usedCardFallback && d.otherMeaningCount == 0 &&
            d.delivered && d.deliveredMeaningId == d.firstVoiceMatchId
        return TrialLogEntity(
            profileId = profileId, startedAt = d.startedAt, endedAt = now, origin = d.origin.name,
            voiceFirstHit = voiceFirstHit, usedCardFallback = d.usedCardFallback,
            otherMeaningCount = d.otherMeaningCount, delivered = d.delivered,
            understood = d.understood, supporterHelped = d.supporterHelped,
            meaningId = d.deliveredMeaningId, meaningText = d.deliveredMeaningText,
        )
    }
}

/** 기록 요약(설정의 실증 기록 화면용). */
data class TrialSummary(
    val total: Int,
    val delivered: Int,
    val understoodYes: Int,
    val understoodAnswered: Int,
    val voiceTrials: Int,
    val voiceFirstHit: Int,
    val cardFallback: Int,
    val supporterHelped: Int,
) {
    companion object {
        fun of(logs: List<TrialLogEntity>) = TrialSummary(
            total = logs.size,
            delivered = logs.count { it.delivered },
            understoodYes = logs.count { it.understood == 1 },
            understoodAnswered = logs.count { it.understood >= 0 },
            voiceTrials = logs.count { it.origin == TrialTracker.Origin.VOICE.name },
            voiceFirstHit = logs.count { it.voiceFirstHit },
            cardFallback = logs.count { it.usedCardFallback },
            supporterHelped = logs.count { it.supporterHelped },
        )
    }
}

/** 실증 기록 CSV. 시간은 ISO 형식, 뜻 문장의 쉼표·따옴표는 CSV 규칙대로 감싼다. */
object TrialCsv {
    const val HEADER =
        "profile,started_at,duration_sec,origin,voice_first_hit,card_fallback,other_meaning_count,delivered,understood,supporter_helped,meaning"

    fun of(logs: List<TrialLogEntity>, profileNames: Map<Long, String>): String = buildString {
        appendLine(HEADER)
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        logs.forEach { l ->
            val understood = when (l.understood) { 1 -> "yes"; 0 -> "no"; else -> "" }
            appendLine(
                listOf(
                    quote(profileNames[l.profileId] ?: l.profileId.toString()),
                    fmt.format(java.util.Date(l.startedAt)),
                    ((l.endedAt - l.startedAt) / 1000).toString(),
                    l.origin.lowercase(),
                    l.voiceFirstHit.toString(), l.usedCardFallback.toString(),
                    l.otherMeaningCount.toString(), l.delivered.toString(), understood,
                    l.supporterHelped.toString(), quote(l.meaningText.orEmpty()),
                ).joinToString(",")
            )
        }
    }

    private fun quote(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
}
