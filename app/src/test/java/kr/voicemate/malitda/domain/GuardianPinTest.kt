package kr.voicemate.malitda.domain

import org.junit.Assert.*
import org.junit.Test

class GuardianPinTest {
    @Test fun `only six ASCII digits are accepted`() {
        assertTrue(GuardianPin.valid("012345"))
        listOf("", "12345", "1234567", "１２３４５６", "12345a", "123 45").forEach {
            assertFalse(GuardianPin.valid(it))
        }
    }

    @Test fun `salted credentials verify without retaining the pin`() {
        val first = GuardianPin.encode("012345")
        val second = GuardianPin.encode("012345")
        assertNotEquals(first, second)
        assertFalse(first.contains("012345"))
        assertTrue(GuardianPin.matches("012345", first))
        assertFalse(GuardianPin.matches("012346", first))
        assertFalse(GuardianPin.matches("012345", "malformed"))
    }

    @Test fun `five failures require a persistent increasing delay`() {
        assertEquals(0L, GuardianPin.cooldownMillis(4))
        assertEquals(30_000L, GuardianPin.cooldownMillis(5))
        assertEquals(60_000L, GuardianPin.cooldownMillis(10))
        assertEquals(300_000L, GuardianPin.cooldownMillis(100))
    }
}
