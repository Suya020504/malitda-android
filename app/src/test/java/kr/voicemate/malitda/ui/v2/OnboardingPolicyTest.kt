package kr.voicemate.malitda.ui.v2

import org.junit.Assert.*
import org.junit.Test

class OnboardingPolicyTest {
    @Test fun explanationIsReadableBeforeConsent() {
        assertFalse(OnboardingPolicy.requiresConsent(true, "U00"))
    }
    @Test fun communicationAndSupporterChoicesStillRequireConsent() {
        assertEquals("U00C", OnboardingPolicy.destination(false, false))
        assertEquals("U00C", OnboardingPolicy.destination(true, false))
    }
    @Test fun consentAllowsCommunicationButDoesNotGrantSupporterUnlock() {
        assertEquals("U01", OnboardingPolicy.destination(false, true))
        assertEquals("U07", OnboardingPolicy.destination(true, true))
        assertTrue(OnboardingPolicy.requiresConsent(true, "U07"))
    }
    @Test fun existingDataManagementRemainsAnExceptionWithSeparatePinGate() {
        assertFalse(OnboardingPolicy.requiresConsent(true, "U12DATA"))
    }
    @Test fun consentCannotBeSkippedForUseOrEditingRoutes() {
        listOf("U01", "U02", "U03", "U04", "U05", "U07", "U08", "U11", "U14", "U00C").forEach {
            assertTrue(it, OnboardingPolicy.requiresConsent(true, it))
            assertFalse(it, OnboardingPolicy.requiresConsent(false, it))
        }
    }
}
