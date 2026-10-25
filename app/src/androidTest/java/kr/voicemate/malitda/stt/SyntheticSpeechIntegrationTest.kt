package kr.voicemate.malitda.stt

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import kr.voicemate.malitda.data.db.MeaningEntity
import kr.voicemate.malitda.data.db.MeaningExpressionEntity
import kr.voicemate.malitda.data.db.MeaningWithExpressions
import kr.voicemate.malitda.domain.MeaningResolver
import kr.voicemate.malitda.domain.TextNormalizer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * One synthetic Windows Heami utterance, not a human/disability speech recording.
 * Uses the packaged local Whisper model with no recognition hints and then the real rule resolver.
 * This is integration coverage, not an accuracy, comprehension or field-effect estimate.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SyntheticSpeechIntegrationTest {
    @Test(timeout = 180_000)
    fun syntheticWaterSpeechRecognizesAndConnectsToRegisteredPrivateMeaning() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val testAssets = instrumentation.context.assets
        val audio = testAssets.open("synthetic-water.wav").use { it.readBytes() }
        val hash = MessageDigest.getInstance("SHA-256").digest(audio).joinToString("") { "%02x".format(it) }
        assertEquals("325ee417e926da55f122333b4e549b9521753c4ab8a3c7cc5c99871a2e5954f7", hash)
        val provenance = org.json.JSONObject(testAssets.open("synthetic-water.provenance.json").bufferedReader().use { it.readText() })
        assertTrue(provenance.getBoolean("synthetic"))
        assertFalse(provenance.getBoolean("human_recording"))
        assertFalse(provenance.getBoolean("cloud_service_used"))
        assertEquals(16000, provenance.getInt("sample_rate"))

        val context = instrumentation.targetContext
        val engine = WhisperSttEngine(context, ModelInstaller.whisper(context))
        try {
            engine.setHints(emptyList())
            engine.prepare()
            assertTrue("Bundled local Whisper must become ready: ${engine.prepareState.value}", engine.prepareState.value is PrepareState.Ready)
            val result = engine.recognizeWav(ByteArrayInputStream(audio))
            assertTrue("Synthetic utterance must produce a non-empty raw transcript", result.raw.isNotBlank())

            // Aliases are fixed independently of recognition output; no raw-dependent registration.
            val meaning = MeaningEntity(
                id = 990001L, profileId = 990002L, text = "물 마시고 싶어요", imageKey = "water", situation = "마실 때",
                confirmed = true, active = true, createdAt = 1L, updatedAt = 1L, useCount = 0, isSample = false,
            )
            val expressions = listOf("물", "물 마시고 싶어요", "물 마시고 싶어").mapIndexed { index, text ->
                MeaningExpressionEntity(index.toLong() + 1L, meaning.profileId, meaning.id, text, TextNormalizer.normalize(text))
            }
            val matches = MeaningResolver.resolve(result.raw, result.alternatives,
                listOf(MeaningWithExpressions(meaning, expressions)), maxCandidates = 3)
            assertTrue("Real resolver must offer the registered water meaning for this one synthetic fixture; raw=${result.raw}",
                matches.any { it.meaning.id == meaning.id })
            Log.i("MalitdaSyntheticSTT", "synthetic_only=true; human_recording=false; hints=none; raw=${result.raw}; candidateIds=${matches.map { it.meaning.id }}; processingMs=${result.processingMs}; audioMs=${result.audioMs}")
        } finally {
            engine.release()
        }
        Unit
    }
}
