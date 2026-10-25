package kr.voicemate.malitda.data.settings

enum class ConfirmationMode(val label: String) {
    PICTURE("그림 먼저"), TEXT("글 먼저"), TTS("소리 먼저");

    companion object {
        fun parse(value: String?) = entries.firstOrNull { it.name == value } ?: PICTURE
    }
}

/** Defaults are migrated without replacing an existing student's chosen art style or meanings. */
data class ProfilePreferences(
    val confirmationMode: ConfirmationMode = ConfirmationMode.PICTURE,
    val candidateCount: Int = 3,
    val ttsRate: Float = 1.0f,
    val haptics: Boolean = true,
    val allowSimilarExpressions: Boolean = true,
) {
    fun normalized() = copy(
        candidateCount = candidateCount.coerceIn(1, 3),
        ttsRate = if (ttsRate.isFinite()) ttsRate.coerceIn(0.5f, 1.2f) else 1f,
    )
}
