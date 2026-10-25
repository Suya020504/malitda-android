package kr.voicemate.malitda.data.pilot

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kr.voicemate.malitda.domain.PilotConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PilotStoreInstrumentedTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun configIsEncryptedAndScopedToOneProfile() = runBlocking {
        val store = PilotStore(context)
        val id = 990001L
        try {
            val config = PilotConfig(participantCode = "P8877", consented = true,
                consentVersion = PilotConfig.CONSENT_VERSION, consentedAt = 1,
                targetMeaningId = 1, practice = false, suitabilityReviewed = true)
            store.update(id, config)
            assertEquals(config, store.get(id))
            assertFalse(store.get(id + 1).canCollect)
            val stored = context.filesDir.resolve("datastore/malitda_pilot.preferences_pb")
            assertTrue(stored.exists())
            assertFalse(String(stored.readBytes(), Charsets.ISO_8859_1).contains("P8877"))
        } finally { store.withdraw(id) }
    }

    @Test fun withdrawalDoesNotRestoreConsentOnNextRead() = runBlocking {
        val store = PilotStore(context)
        val id = 990002L
        store.update(id, PilotConfig(participantCode = "P8878", consented = true,
            consentVersion = PilotConfig.CONSENT_VERSION, consentedAt = 1))
        store.withdraw(id)
        assertFalse(store.get(id).consented)
        assertFalse(store.get(id).canCollect)
        assertEquals("", store.get(id).participantCode)
    }
}
