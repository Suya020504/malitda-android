package kr.voicemate.malitda.ui.v2

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Only callers that have checked the current explicit approval may pass a message here. */
object CommunicationIntents {
    fun shareText(context: Context, text: String) {
        require(text.isNotBlank())
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, "공유할 앱 선택").apply {
            // This is a chooser hand-off, never a delivery receipt.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun copySensitive(context: Context, text: String) {
        require(text.isNotBlank())
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val label = "malitda-" + UUID.randomUUID()
        val clip = ClipData.newPlainText(label, text).apply {
            description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        }
        manager.setPrimaryClip(clip)
        Handler(Looper.getMainLooper()).postDelayed({
            // Leave a later clipboard entry from another app alone.
            runCatching {
                if (manager.primaryClipDescription?.label?.toString() == label) {
                    if (android.os.Build.VERSION.SDK_INT >= 28) manager.clearPrimaryClip()
                    else manager.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }
        }, 60_000)
    }

    fun clearOwnClipboard(context: Context) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        runCatching {
            val label = manager.primaryClipDescription?.label?.toString().orEmpty()
            if (label.startsWith("malitda-") || label == "말잇다") {
                if (android.os.Build.VERSION.SDK_INT >= 28) manager.clearPrimaryClip()
                else manager.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }
    fun shareAnonymousCsv(context: Context, csv: String) {
        val directory = File(context.cacheDir, "trial_exports").apply { mkdirs() }
        directory.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
        val file = File(directory, "malitda-pilot-${UUID.randomUUID()}.csv")
        file.writeText("\uFEFF" + csv, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("말잇다 익명 실증 기록", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "익명 실증 CSV 공유").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }
}
