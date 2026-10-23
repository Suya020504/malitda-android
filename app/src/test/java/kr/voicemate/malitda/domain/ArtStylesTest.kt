package kr.voicemate.malitda.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtStylesTest {
    @Test
    fun tenStylesMatchingServerIds() {
        assertEquals(10, ArtStyles.all.size)
        assertEquals(
            setOf("storybook", "photo", "simple", "watercolor", "colored-pencil", "clay", "paper-cut", "pixel", "comic", "line-art"),
            ArtStyles.all.map { it.id }.toSet(),
        )
        assertEquals("수채화", ArtStyles.label("watercolor"))
        assertEquals(ArtStyles.DEFAULT, ArtStyles.normalize("unknown"))
        assertEquals(ArtStyles.DEFAULT, ArtStyles.normalize(null))
    }

    @Test
    fun styleSyncDecision() {
        assertEquals(StyleSync.Create, StyleSync.decide(null, null, "clay"))
        assertEquals(StyleSync.Update(3), StyleSync.decide("watercolor", 3, "clay"))
        assertEquals(StyleSync.Ready(3), StyleSync.decide("clay", 3, "clay"))
    }

    @Test
    fun uncertainPaidResultsSayNoAutoRetry() {
        assertTrue(R4Messages.forStatus(504, false).contains("자동으로 다시 보내지 않았어요"))
        assertTrue(R4Messages.forStatus(502, false).contains("자동으로 다시 보내지 않았어요"))
        // 점검 모드는 상태 코드와 관계없이 점검 안내를 보여 준다.
        assertTrue(R4Messages.forStatus(500, true).contains("점검 모드"))
        assertTrue(R4Messages.forStatus(403, false).contains("점검 모드"))
    }
}
