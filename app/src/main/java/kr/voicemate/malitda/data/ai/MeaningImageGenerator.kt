package kr.voicemate.malitda.data.ai

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.voicemate.malitda.BuildConfig
import kr.voicemate.malitda.R
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * 지원자가 누르면 OpenAI 이미지 모델로 의미 그림을 만든다(디버그 빌드 전용).
 * 기존 의미 카드 4장을 레퍼런스로 함께 보내 같은 화풍을 맞춘다.
 * 외부로 보내는 것은 '전할 뜻' 문장과 레퍼런스 그림뿐이다 — 학생 이름·학생이 쓰는 표현은 보내지 않는다.
 * 만든 그림도 학생과 함께 확인(U10)을 거쳐야 쓰인다.
 */
object MeaningImageGenerator {
    private const val ENDPOINT = "https://api.openai.com/v1/images/edits"
    private val REFERENCES = listOf(
        R.drawable.v2_card_walk, R.drawable.v2_card_water, R.drawable.v2_card_rest, R.drawable.v2_card_cold,
    )

    /** 키가 설정된 빌드(디버그 + local.properties)에서만 버튼을 보여 준다. */
    val isAvailable: Boolean get() = BuildConfig.OPENAI_API_KEY.isNotBlank()

    fun prompt(meaning: String): String = """
        첨부한 4장의 그림과 같은 화풍으로 새 그림을 한 장 그려 주세요.
        - 화풍: 부드러운 3D 클레이 느낌, 둥근 형태, 파스텔 색감, 밝고 따뜻한 빛
        - 배경: 단순하고 흐릿하게, 주인공이 한눈에 보이도록
        - 사람이 나오면 얼굴을 자세히 그리지 말고 뒷모습이나 단순한 형태로
        - 글자, 숫자, 로고, 말풍선은 넣지 않기
        - 어둡거나 무섭거나 다치는 장면은 피하기
        - 글을 읽지 못하는 사람도 그림만 보고 아래 뜻을 짐작할 수 있게
        전할 뜻: ${meaning.trim()}
    """.trimIndent()

    /** 생성한 PNG 바이트를 돌려준다. 실패하면 사용자에게 보여 줄 문장을 담은 예외를 던진다. */
    suspend fun generate(context: Context, meaning: String): ByteArray = withContext(Dispatchers.IO) {
        require(isAvailable) { "그림 만들기가 설정되지 않았어요." }
        require(meaning.isNotBlank()) { "먼저 전할 뜻을 입력해 주세요." }

        val boundary = "malitda-" + UUID.randomUUID()
        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 180_000
            setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        try {
            DataOutputStream(conn.outputStream).use { out ->
                fun field(name: String, value: String) {
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    out.write(value.toByteArray(Charsets.UTF_8))
                    out.writeBytes("\r\n")
                }
                field("model", BuildConfig.OPENAI_IMAGE_MODEL)
                field("prompt", prompt(meaning))
                field("size", "1536x1024")
                field("quality", "medium")
                field("n", "1")
                REFERENCES.forEachIndexed { i, res ->
                    val bytes = context.resources.openRawResource(res).use { it.readBytes() }
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"image[]\"; filename=\"ref$i.png\"\r\n")
                    out.writeBytes("Content-Type: image/png\r\n\r\n")
                    out.write(bytes)
                    out.writeBytes("\r\n")
                }
                out.writeBytes("--$boundary--\r\n")
            }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
            if (code !in 200..299) error(friendlyError(code, body))
            val b64 = JSONObject(body).getJSONArray("data").getJSONObject(0).optString("b64_json")
            require(b64.isNotBlank()) { "그림을 받지 못했어요. 다시 시도해 주세요." }
            Base64.decode(b64, Base64.DEFAULT)
        } catch (e: java.net.UnknownHostException) {
            error("인터넷에 연결되어 있지 않아요.")
        } catch (e: java.net.SocketTimeoutException) {
            error("그림을 만드는 데 시간이 너무 오래 걸려요. 다시 시도해 주세요.")
        } finally {
            conn.disconnect()
        }
    }

    private fun friendlyError(code: Int, body: String): String {
        val detail = runCatching { JSONObject(body).getJSONObject("error").optString("message") }.getOrNull().orEmpty()
        return when (code) {
            401 -> "그림 만들기 키가 맞지 않아요. 설정을 확인해 주세요."
            429 -> "요청이 많거나 사용 한도를 넘었어요. 잠시 후 다시 시도해 주세요."
            400 -> if (detail.contains("safety", true)) "이 뜻으로는 그림을 만들 수 없어요. 문장을 바꿔 보세요."
                   else "그림을 만들 수 없어요. ($detail)"
            else -> "그림을 만들지 못했어요. ($code)"
        }
    }
}
