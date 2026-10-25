package kr.voicemate.malitda.data.ai

import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.voicemate.malitda.BuildConfig
import kr.voicemate.malitda.domain.R4Messages
import kr.voicemate.malitda.domain.StyleSync
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * 팀원 R4 서버(FastAPI) 클라이언트. 그림 생성 키는 PC 서버에만 있고 앱에는 없다.
 * 디버그 빌드에서 adb reverse로 연결한 이 기기의 127.0.0.1:8769만 쓴다(릴리스는 주소가 비어 꺼짐).
 * 서버로 보내는 것: 학생의 익명 ID, 그림체, '전할 뜻' 문장. 학생 이름·사진·음성은 보내지 않는다.
 */
object R4Client {
    class R4Exception(val status: Int, override val message: String) : Exception(message)

    data class Status(val reachable: Boolean, val reviewMode: Boolean, val ready: Boolean, val notice: String?, val demoMode: Boolean = false)

    private val base: String get() = BuildConfig.R4_SERVER_URL

    /** 이 빌드에 서버 연결이 들어 있는가(디버그만). */
    val isEnabled: Boolean get() = base.isNotBlank()

    /** 서버 상태. 연결 실패는 예외 대신 reachable=false로 돌려준다. */
    suspend fun status(): Status = withContext(Dispatchers.IO) {
        runCatching {
            val health = get("/health")
            val config = get("/api/config")
            Status(
                reachable = true,
                reviewMode = health.optString("mode") == "review" || config.optString("mode") == "review",
                ready = config.optBoolean("ready", false),
                notice = config.optString("notice").ifBlank { null },
                demoMode = config.optString("demo_mode") == "offline-mock" || health.optString("demo_mode") == "offline-mock",
            )
        }.getOrElse { if (it is CancellationException) throw it; Status(reachable = false, reviewMode = false, ready = false, notice = null) }
    }

    /** 학생 그림체를 서버에 맞추고 현재 버전을 돌려준다. */
    suspend fun syncStyle(anonId: String, styleId: String): Int = withContext(Dispatchers.IO) {
        val current = runCatching { get("/api/profiles/$anonId/style") }
            .recoverCatching { e -> if (e is R4Exception && e.status == 404) null else throw e }
            .getOrThrow()
        when (val action = StyleSync.decide(current?.optString("style_id"), current?.optInt("revision"), styleId)) {
            is StyleSync.Ready -> action.revision
            StyleSync.Create -> post("/api/profiles/$anonId/style", JSONObject().put("style_id", styleId).put("expected_revision", 0)).getInt("revision")
            is StyleSync.Update -> post("/api/profiles/$anonId/style", JSONObject().put("style_id", styleId).put("expected_revision", action.expectedRevision)).getInt("revision")
        }
    }

    /**
     * 학생 그림체로 뜻 그림 한 장을 만든다(유료). 요청 ID는 호출마다 새로 만든다 —
     * 결과가 불확실한 요청을 앱이 자동으로 다시 보내지 않는다.
     */
    suspend fun generate(anonId: String, styleId: String, meaning: String): ByteArray = withContext(Dispatchers.IO) {
        require(meaning.isNotBlank()) { "먼저 전할 뜻을 입력해 주세요." }
        val revision = syncStyle(anonId, styleId)
        val body = JSONObject()
            .put("request_id", "malitda-" + UUID.randomUUID().toString().replace("-", "").take(24))
            .put("description", meaning.trim().take(120))
            .put("meaning", meaning.trim().take(120))
            .put("consent", true)
            .put("profile_id", anonId)
            .put("profile_revision", revision)
        val result = post("/api/generate", body, readTimeoutMs = 200_000)
        val dataUrl = result.optString("data_url")
        require(dataUrl.startsWith("data:image/")) { "그림을 받지 못했어요. 다시 시도해 주세요." }
        Base64.decode(dataUrl.substringAfter(','), Base64.DEFAULT)
    }

    /** 그림체 미리보기 그림(서버 기본 그림). 없으면 null. */
    suspend fun stylePreviews(): Map<String, ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val rows = get("/api/styles").getJSONArray("styles")
            (0 until rows.length()).mapNotNull { i ->
                val row = rows.getJSONObject(i)
                val url = if (row.isNull("preview_url")) null else row.optString("preview_url").ifBlank { null }
                url?.takeIf { it.matches(Regex("^/(styles|comparison|representations)/[a-zA-Z0-9_.-]+$")) }
                    ?.let { u -> runCatching { row.getString("id") to download(u) }.getOrNull() }
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    // ---------- HTTP ----------

    private suspend fun get(path: String): JSONObject = request(path, null, 15_000)

    private suspend fun post(path: String, body: JSONObject, readTimeoutMs: Int = 15_000): JSONObject {
        // 서버가 다시 켜지면 토큰이 바뀌므로 쓰기 직전에 받아 온다.
        val token = get("/api/config").getString("session_token")
        return request(path, body, readTimeoutMs, token)
    }

    private suspend fun request(path: String, body: JSONObject?, readTimeoutMs: Int, token: String? = null): JSONObject {
        val conn = (URL(base + path).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 5_000
            readTimeout = readTimeoutMs
            if (body != null) {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Origin", base)
                setRequestProperty("X-Demo-Token", token)
            }
        }
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { conn.disconnect() }
            thread(name = "malitda-r4-http", isDaemon = true) {
                try {
                    if (!continuation.isActive) return@thread
                    body?.let { b -> conn.outputStream.use { it.write(b.toString().toByteArray(Charsets.UTF_8)) } }
                    val code = conn.responseCode
                    if (code !in 200..299) {
                        val review = runCatching {
                            JSONObject(conn.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()).optString("mode") == "review"
                        }.getOrDefault(false)
                        throw R4Exception(code, R4Messages.forStatus(code, review))
                    }
                    val result = JSONObject(readLimited(conn, 24_000_000).toString(Charsets.UTF_8))
                    if (continuation.isActive) continuation.resume(result)
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(
                        if (e is java.io.IOException) R4Exception(0, R4Messages.UNREACHABLE) else e)
                } finally { conn.disconnect() }
            }
        }
    }

    private suspend fun download(path: String): ByteArray {
        val conn = (URL(base + path).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false; connectTimeout = 5_000; readTimeout = 15_000
        }
        try {
            check(conn.responseCode == 200)
            return readLimited(conn, 6_000_000)
        } finally { conn.disconnect() }
    }

    private fun readLimited(conn: HttpURLConnection, limit: Int): ByteArray = conn.inputStream.use { input ->
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val n = input.read(chunk); if (n < 0) break
            check(out.size() + n <= limit) { "응답이 너무 커요" }
            out.write(chunk, 0, n)
        }
        out.toByteArray()
    }
}
