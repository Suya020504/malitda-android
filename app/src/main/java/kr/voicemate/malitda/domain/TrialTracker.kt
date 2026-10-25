package kr.voicemate.malitda.domain

import kr.voicemate.malitda.data.db.TrialLogEntity

/** 한 과업의 시도와 재시도를 분리한다. 음성 원문과 전달 문장은 실증 기록에 저장하지 않는다. */
class TrialTracker {
    enum class Origin { VOICE, CARD, TEXT }

    data class Draft(
        val startedAt: Long,
        val origin: Origin,
        val config: PilotConfig = PilotConfig(),
        val firstVoiceMatchId: Long? = null,
        val voiceResultSeen: Boolean = false,
        val usedCardFallback: Boolean = false,
        val otherMeaningCount: Int = 0,
        val deliveredMeaningId: Long? = null,
        val understood: Int = -1,
        val supporterHelped: Boolean = false,
        val retryCount: Int = 0,
        val editCount: Int = 0,
        val accidentalSendCount: Int = 0,
        val observedCorrect: Int = -1,
        val usedSample: Boolean = false,
        val feedback: PilotFeedback = PilotFeedback(),
    ) {
        val delivered: Boolean get() = deliveredMeaningId != null
    }

    var draft: Draft? = null
        private set

    /** 같은 음성 과업의 다시 말하기만 유지한다. 새 과업 진입에서는 startNew를 쓴다. */
    fun start(origin: Origin, now: Long) {
        val old = draft
        draft = if (old == null || old.origin != origin) Draft(now, origin)
            else old.copy(retryCount = old.retryCount + if (origin == Origin.VOICE) 1 else 0)
    }

    fun startNew(origin: Origin, now: Long, config: PilotConfig = PilotConfig()) {
        draft = Draft(now, origin, config)
    }

    fun onVoiceResult(firstMatchId: Long?) {
        val d = draft ?: return
        if (!d.voiceResultSeen) draft = d.copy(firstVoiceMatchId = firstMatchId, voiceResultSeen = true)
    }

    fun onCardPicked() {
        val d = draft ?: return
        if (d.origin == Origin.VOICE) draft = d.copy(usedCardFallback = true)
    }

    fun onOtherMeaning() { draft = draft?.let { it.copy(otherMeaningCount = it.otherMeaningCount + 1) } }
    fun onEdited() { draft = draft?.let { it.copy(editCount = it.editCount + 1) } }
    fun onRetry() { draft = draft?.let { it.copy(retryCount = it.retryCount + 1) } }
    fun setJudgedCorrect(correct: Boolean?) { draft = draft?.copy(observedCorrect = when (correct) { true -> 1; false -> 0; null -> -1 }) }
    fun onAccidentalSend() { draft = draft?.let { it.copy(accidentalSendCount = it.accidentalSendCount + 1) } }

    /** text 매개변수는 기존 호출부 호환용이며 저장·캐시하지 않는다. */
    @Suppress("UNUSED_PARAMETER")
    fun onDelivered(meaningId: Long, meaningText: String = "", isSample: Boolean = false) {
        draft = draft?.copy(deliveredMeaningId = meaningId, usedSample = isSample)
    }

    fun setUnderstood(understood: Boolean?) {
        draft = draft?.copy(understood = when (understood) { true -> 1; false -> 0; null -> -1 })
    }
    fun setSupporterHelped(helped: Boolean) { draft = draft?.copy(supporterHelped = helped) }
    fun setFeedback(feedback: PilotFeedback) { draft = draft?.copy(feedback = feedback) }

    fun finish(profileId: Long, now: Long): TrialLogEntity? = end(profileId, now, "COMPLETED", "")

    fun cancel(profileId: Long, now: Long, reason: PilotStopReason = PilotStopReason.CANCELLED): TrialLogEntity? =
        end(profileId, now, "ABORTED", reason.name)

    fun discard() { draft = null }

    private fun end(profileId: Long, now: Long, outcome: String, stopReason: String): TrialLogEntity? {
        val d = draft ?: return null
        draft = null
        val voiceFirstHit = d.origin == Origin.VOICE && !d.usedCardFallback && d.otherMeaningCount == 0 &&
            d.delivered && d.deliveredMeaningId == d.firstVoiceMatchId
        return TrialLogEntity(
            profileId = profileId, startedAt = d.startedAt, endedAt = now.coerceAtLeast(d.startedAt),
            origin = d.origin.name, voiceFirstHit = voiceFirstHit, usedCardFallback = d.usedCardFallback,
            otherMeaningCount = d.otherMeaningCount, delivered = d.delivered, understood = d.understood,
            supporterHelped = d.supporterHelped, meaningId = d.deliveredMeaningId, meaningText = null,
            participantCode = d.config.participantCode, condition = d.config.condition,
            taskCode = d.config.taskCode, targetMeaningId = d.config.targetMeaningId,
            outcome = outcome, practice = d.config.practice || d.usedSample, consented = d.config.canCollect,
            deviceContext = d.config.deviceContext, qualitativeNote = d.feedback.encoded(),
            judgedCorrect = if (d.observedCorrect >= 0) d.observedCorrect else if (d.config.targetMeaningId == null || !d.delivered || (d.deliveredMeaningId ?: 0L) <= 0L) -1
                else if (d.deliveredMeaningId == d.config.targetMeaningId) 1 else 0,
            retryCount = d.retryCount, editCount = d.editCount, stopReason = stopReason,
            accidentalSendCount = d.accidentalSendCount,
            taskSetVersion = d.config.taskSetVersion,
        )
    }
}

/** 완료·중단·연습을 구분해 평가의 분모에 섞지 않는다. */
data class TrialSummary(
    val total: Int,
    val delivered: Int,
    val understoodYes: Int,
    val understoodAnswered: Int,
    val voiceTrials: Int,
    val voiceFirstHit: Int,
    val cardFallback: Int,
    val supporterHelped: Int,
    val aborted: Int = 0,
    val practice: Int = 0,
    val completed: Int = 0,
) {
    companion object {
        fun of(logs: List<TrialLogEntity>): TrialSummary {
            val completed = logs.filter { it.outcome == "COMPLETED" && !it.practice && it.consented }
            return TrialSummary(
                total = logs.size, delivered = completed.count { it.delivered },
                understoodYes = completed.count { it.understood == 1 },
                understoodAnswered = completed.count { it.understood >= 0 },
                voiceTrials = completed.count { it.origin == TrialTracker.Origin.VOICE.name },
                voiceFirstHit = completed.count { it.voiceFirstHit },
                cardFallback = completed.count { it.usedCardFallback },
                supporterHelped = completed.count { it.supporterHelped },
                aborted = logs.count { it.outcome == "ABORTED" }, practice = logs.count { it.practice },
                completed = completed.size,
            )
        }
    }
}

/** 공유 전에 현재 참여자와 범위를 확인한다. 이름을 받던 API는 폐기했다. */
data class TrialExportScope(val profileId: Long, val participantCode: String, val currentlyConsented: Boolean)

object TrialCsv {
    const val HEADER = "participant_code,task_code,task_set_version,condition,duration_sec,origin,voice_first_hit,card_fallback,retries,edits,other_meaning_count,delivered,understood,supporter_helped,judged_correct,accidental_sends"

    fun exportable(logs: List<TrialLogEntity>, scope: TrialExportScope): List<TrialLogEntity> {
        if (scope.profileId <= 0 || !scope.currentlyConsented || !PilotConfig.validParticipantCode(scope.participantCode)) return emptyList()
        return logs.filter { it.profileId == scope.profileId && it.participantCode == scope.participantCode &&
            it.consented && it.outcome == "COMPLETED" && !it.practice &&
            PilotConfig.validTaskCode(it.taskCode) && it.condition in PilotConfig.CONDITIONS &&
            PilotConfig.validTaskVersion(it.taskSetVersion) &&
            it.origin in TrialTracker.Origin.entries.map { origin -> origin.name } }
            .sortedBy { it.startedAt }
    }

    /** 코드·조건·수치만 반출한다. 절대시각·기기ID·뜻ID·문장·면담 내용은 포함하지 않는다. */
    fun of(logs: List<TrialLogEntity>, scope: TrialExportScope): String = buildString {
        appendLine(HEADER)
        exportable(logs, scope).forEach { l ->
            appendLine(listOf(scope.participantCode, l.taskCode, l.taskSetVersion, l.condition,
                ((l.endedAt - l.startedAt).coerceAtLeast(0) / 1000).toString(), l.origin.lowercase(),
                bit(l.voiceFirstHit), bit(l.usedCardFallback), l.retryCount.coerceAtLeast(0).toString(),
                l.editCount.coerceAtLeast(0).toString(), l.otherMeaningCount.coerceAtLeast(0).toString(),
                bit(l.delivered), l.understood.coerceIn(-1, 1).toString(), bit(l.supporterHelped),
                l.judgedCorrect.coerceIn(-1, 1).toString(), l.accidentalSendCount.coerceAtLeast(0).toString())
                .joinToString(","))
        }
    }
    private fun bit(value: Boolean) = if (value) "1" else "0"
}
