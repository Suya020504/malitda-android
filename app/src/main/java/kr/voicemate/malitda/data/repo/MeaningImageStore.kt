package kr.voicemate.malitda.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/**
 * 의미 그림으로 쓸 사진을 앱 전용 불변 PNG로 저장한다(PR #1에서 이식, Suya020504).
 * 원본과 외부 URI는 바꾸지 않는다. 파일명은 내용 해시라 같은 사진은 한 번만 저장된다.
 * imageKey 형식: "file:meaning_media/<sha256>.png" (filesDir 기준 상대 경로).
 */
object MeaningImageStore {
    private const val DIR = "meaning_media"
    const val KEY_PREFIX = "file:"

    suspend fun import(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val data = context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArrayOutputStream()
            val block = ByteArray(8192)
            while (true) {
                val n = input.read(block); if (n < 0) break
                require(buffer.size() + n <= 24 * 1024 * 1024) { "사진이 너무 커요. 작은 사진을 선택해 주세요." }
                buffer.write(block, 0, n)
            }
            buffer.toByteArray()
        } ?: error("사진을 열 수 없어요.")
        importBytes(context, data)
    }

    /** 이미 받은 이미지 바이트(생성 그림 등)를 같은 규칙으로 저장한다. */
    suspend fun importBytes(context: Context, data: ByteArray): String = withContext(Dispatchers.IO) {
        val bitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(java.nio.ByteBuffer.wrap(data))) { decoder, info, _ ->
                val ratio = maxOf(info.size.width, info.size.height) / 1024f
                if (ratio > 1) decoder.setTargetSize(
                    (info.size.width / ratio).toInt().coerceAtLeast(1),
                    (info.size.height / ratio).toInt().coerceAtLeast(1),
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "사용할 수 없는 사진이에요." }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
            BitmapFactory.decodeByteArray(data, 0, data.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: error("사진을 읽을 수 없어요.")
        }
        val png = ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); out.toByteArray() }
        bitmap.recycle()
        val hash = MessageDigest.getInstance("SHA-256").digest(png).joinToString("") { "%02x".format(it) }
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val target = File(dir, "$hash.png")
        if (!target.exists()) {
            File(dir, "$hash.part").apply { writeBytes(png) }
                .let { require(it.renameTo(target) || target.exists()) { "사진을 저장할 수 없어요." } }
        }
        "$KEY_PREFIX$DIR/$hash.png"
    }

    /** imageKey가 사진 키면 실제 파일을 돌려준다(없으면 null). */
    fun fileFor(filesDir: File, imageKey: String?): File? {
        if (imageKey == null || !imageKey.startsWith(KEY_PREFIX)) return null
        val rel = imageKey.removePrefix(KEY_PREFIX)
        if (!rel.startsWith("$DIR/")) return null
        val f = File(filesDir, rel)
        return if (f.isFile) f else null
    }

    /** 어떤 뜻도 참조하지 않는 사진을 지운다(삭제·수정 뒤 정리). */
    suspend fun removeUnreferenced(filesDir: File, references: Set<String>) = withContext(Dispatchers.IO) {
        val dir = File(filesDir, DIR).canonicalFile
        dir.listFiles()?.filter { it.isFile && it.name.matches(Regex("[a-f0-9]{64}\\.png")) }
            ?.filter { "$KEY_PREFIX$DIR/${it.name}" !in references }
            ?.forEach { if (it.canonicalFile.parentFile == dir) it.delete() }
    }
}
