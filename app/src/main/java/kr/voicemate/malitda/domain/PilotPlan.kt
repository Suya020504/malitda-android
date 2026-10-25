package kr.voicemate.malitda.domain

/** 실증 설정은 진단명·실명·음성·전사문을 수집하지 않는다. */
data class PilotConfig(
    val participantCode: String = "",
    val consented: Boolean = false,
    val consentVersion: String = "",
    val consentedAt: Long = 0,
    val condition: String = "C",
    val taskCode: String = "T01",
    val targetMeaningId: Long? = null,
    val practice: Boolean = true,
    val deviceContext: String = "QUIET",
    val ordinal: Int = 0,
    val paused: Boolean = false,
    val consecutiveFailures: Int = 0,
    val suitabilityReviewed: Boolean = false,
    val taskSetVersion: String = "local-v1",
    val taskTargets: Map<String, Long> = emptyMap(),
) {
    val hasConsent: Boolean get() = consented && consentVersion == CONSENT_VERSION && consentedAt > 0 &&
        validParticipantCode(participantCode)
    val canCollect: Boolean get() = hasConsent && validTaskCode(taskCode) && condition in CONDITIONS && !paused &&
        validTaskVersion(taskSetVersion) && (practice || (suitabilityReviewed && (targetMeaningId ?: 0L) > 0L))

    /** 순서 추천은 비교조건의 변경을 자동 실행하지 않는다. 지원자가 다음 조건을 확인한다. */
    val recommendedCondition: String get() = PilotPlan.order(participantCode)[ordinal.coerceAtLeast(0) % 3]

    companion object {
        const val CONSENT_VERSION = "malitda-pilot-2026-10-26-v1"
        val CONDITIONS = listOf("A", "B", "C")
        val ENVIRONMENTS = listOf("QUIET", "NORMAL", "NOISY")
        fun validParticipantCode(code: String) = Regex("[A-Z][0-9]{3,5}").matches(code)
        fun validTaskCode(code: String) = Regex("T[0-9]{2,3}").matches(code)
        fun validTaskVersion(version: String) = Regex("local-v[1-9][0-9]{0,3}").matches(version)
    }
}

enum class PilotStopReason {
    PARTICIPANT_REQUEST, FATIGUE, THREE_FAILURES, SAFETY, BACKGROUND, PROFILE_CHANGE, CANCELLED,
}

object PilotPlan {
    /** 참여자 번호에 따른 라틴스퀘어 순서. 무작위배정·임상 효과를 뜻하지 않는다. */
    fun order(participantCode: String): List<String> {
        val index = ((participantCode.drop(1).toIntOrNull() ?: 1).coerceAtLeast(1) - 1) % 3
        return List(3) { PilotConfig.CONDITIONS[(index + it) % 3] }
    }

    fun shouldPause(consecutiveFailures: Int, participantAsked: Boolean = false,
                    fatigue: Boolean = false, safetyIssue: Boolean = false): Boolean =
        consecutiveFailures >= 3 || participantAsked || fatigue || safetyIssue

    /** Prefer a receiver's observed understanding; do not turn missing observations into failures. */
    fun observedSuccess(understood: Int, correct: Int): Boolean? = when {
        understood in 0..1 -> understood == 1
        correct in 0..1 -> correct == 1
        else -> null
    }

    fun afterCompleted(config: PilotConfig, successful: Boolean): PilotConfig {
        val failed = if (successful) 0 else config.consecutiveFailures + 1
        return config.copy(consecutiveFailures = failed, paused = shouldPause(failed), ordinal = config.ordinal + 1)
    }

    fun nextTaskVersion(previous: PilotConfig, targets: Map<String, Long>): String {
        if (previous.taskTargets.isEmpty() || previous.taskTargets == targets) return previous.taskSetVersion
        val n = previous.taskSetVersion.removePrefix("local-v").toIntOrNull() ?: 1
        return "local-v${(n + 1).coerceAtMost(9999)}"
    }
}

/** 면담의 정량 응답만 남긴다. 응답 없음은 0, 1~5는 응답값이며 이름·자유문장은 받지 않는다. */
data class PilotFeedback(
    val control: Int = 0,
    val fatigue: Int = 0,
    val willingness: Int = 0,
    val candidateEase: Int = 0,
    val ttsEase: Int = 0,
) {
    fun encoded(): String = listOf(control, fatigue, willingness, candidateEase, ttsEase)
        .joinToString(",") { it.coerceIn(0, 5).toString() }

    companion object {
        fun decode(encoded: String): PilotFeedback {
            val n = encoded.split(',').map { (it.toIntOrNull() ?: 0).coerceIn(0, 5) }
            return PilotFeedback(n.getOrElse(0) { 0 }, n.getOrElse(1) { 0 }, n.getOrElse(2) { 0 },
                n.getOrElse(3) { 0 }, n.getOrElse(4) { 0 })
        }
    }
}
