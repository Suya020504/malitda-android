package kr.voicemate.malitda.data.feedback

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings

enum class FeedbackKind { START, SUCCESS, ERROR, STOP }

/**
 * A short optional state cue. The caller checks the current person's haptic preference first.
 * No sound, speech or repeated background vibration is generated.
 */
class FeedbackSignal(context: Context) {
    private val app = context.applicationContext
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= 31) app.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") (app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    }
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** true means a request was made, not a measured guarantee that the device physically vibrated. */
    fun pulse(kind: FeedbackKind): Boolean = runCatching {
        @Suppress("DEPRECATION")
        val systemEnabled = Settings.System.getInt(app.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
        val device = vibrator
        if (!systemEnabled || device == null || !device.hasVibrator()) return false
        val effect = when (kind) {
            FeedbackKind.START -> VibrationEffect.createOneShot(15L, VibrationEffect.DEFAULT_AMPLITUDE)
            FeedbackKind.SUCCESS -> VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE)
            FeedbackKind.ERROR -> VibrationEffect.createWaveform(longArrayOf(0L, 25L, 35L, 25L), -1)
            FeedbackKind.STOP -> VibrationEffect.createOneShot(10L, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        @Suppress("DEPRECATION")
        device.vibrate(effect, attributes)
        true
    }.getOrDefault(false)
}
