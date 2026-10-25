package kr.voicemate.malitda.domain

import org.junit.Assert.*
import org.junit.Test

class OfflineTtsPolicyTest {
    @Test fun onlyInstalledOfflineKoreanVoiceCanReadPrivateText() {
        assertTrue(OfflineTtsPolicy.permits("ko", false, true))
        assertFalse(OfflineTtsPolicy.permits("ko", true, true))
        assertFalse(OfflineTtsPolicy.permits("ko", false, false))
        assertFalse(OfflineTtsPolicy.permits("en", false, true))
        assertFalse(OfflineTtsPolicy.permits(null, null, true))
    }
    @Test fun staleOfflineStateCannotApproveANetworkVoice() {
        assertFalse(OfflineTtsPolicy.permits("ko", true, true, readyOffline = true))
        assertFalse(OfflineTtsPolicy.permits("ko", false, true, readyOffline = false))
    }
}
