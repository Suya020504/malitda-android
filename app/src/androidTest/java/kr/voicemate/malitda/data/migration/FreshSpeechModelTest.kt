package kr.voicemate.malitda.data.migration

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kr.voicemate.malitda.stt.ModelInstaller
import kr.voicemate.malitda.stt.PrepareState
import kr.voicemate.malitda.stt.WhisperSttEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** A warm emulator must not substitute for assets that have to be inside a fresh APK. */
@RunWith(AndroidJUnit4::class)
class FreshSpeechModelTest {
    @Test fun apkContainsThePinnedWhisperModelAndTheVoskAcousticModel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val digest = MessageDigest.getInstance("SHA-256")
        var size = 0L
        context.assets.open("whisper/ggml-base-q5_1.bin").use { input ->
            val bytes = ByteArray(1 shl 16)
            while (true) { val n = input.read(bytes); if (n < 0) break; digest.update(bytes, 0, n); size += n }
        }
        assertEquals(59_707_625L, size)
        assertEquals("422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
            digest.digest().joinToString("") { "%02x".format(it) })
        context.assets.open("model-ko/am/final.mdl").use { assertTrue(it.read() >= 0) }
    }

    @Test fun whisperPreparesFromAnEmptyPrivateModelDirectory() = runBlocking {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val isolated = File(base.cacheDir, "fresh-model-test-" + UUID.randomUUID()).apply { mkdirs() }
        val context = object : ContextWrapper(base) {
            override fun getFilesDir(): File = isolated
            override fun getApplicationContext(): Context = this
        }
        val engine = WhisperSttEngine(context, ModelInstaller.whisper(context))
        try {
            assertFalse(ModelInstaller.whisper(context).isInstalled())
            engine.prepare()
            assertTrue("Whisper preparation failed: ${engine.prepareState.value}", engine.prepareState.value is PrepareState.Ready)
            assertTrue(ModelInstaller.whisper(context).isInstalled())
        } finally { engine.release(); isolated.deleteRecursively() }
    }
}
