package kr.voicemate.malitda.domain

import kr.voicemate.malitda.data.settings.ConfirmationMode
import kr.voicemate.malitda.data.settings.ProfilePreferences
import org.junit.Assert.*
import org.junit.Test

class ProfilePreferencesTest {
    @Test fun `one two or three visible candidates remain explicit user choices`() {
        (1..3).forEach { count -> assertEquals(count, ProfilePreferences(candidateCount = count).normalized().candidateCount) }
        assertEquals(1, ProfilePreferences(candidateCount = -3).normalized().candidateCount)
        assertEquals(3, ProfilePreferences(candidateCount = 999).normalized().candidateCount)
    }

    @Test fun `corrupted preferences fall back safely`() {
        assertEquals(ConfirmationMode.PICTURE, ConfirmationMode.parse("unknown"))
        assertEquals(ConfirmationMode.TTS, ConfirmationMode.parse("TTS"))
        assertEquals(1f, ProfilePreferences(ttsRate = Float.NaN).normalized().ttsRate)
        assertEquals(1.2f, ProfilePreferences(ttsRate = 999f).normalized().ttsRate)
    }

    @Test fun `preference change does not imply approval or enable pilot logging`() {
        val defaults = ProfilePreferences()
        val text = defaults.copy(confirmationMode = ConfirmationMode.TEXT)
        assertEquals(3, text.candidateCount)
        assertTrue(text.haptics)
        assertTrue(text.allowSimilarExpressions)
        assertEquals(1f, text.ttsRate)
    }
}
