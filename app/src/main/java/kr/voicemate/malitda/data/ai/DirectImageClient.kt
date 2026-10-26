package kr.voicemate.malitda.data.ai

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.voicemate.malitda.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * 지원자용(디버그) 빌드에서 PC 서버 없이 앱이 OpenAI 이미지 모델을 직접 부른다.
 * 키는 local.properties → BuildConfig(디버그만)로 들어온다. 릴리스에는 키가 없어 이 경로가 꺼진다.
 * 보내는 것은 '전할 뜻' 문장과 그림체 설명뿐이다 — 학생 이름·사진·목소리는 보내지 않는다.
 * 만든 그림도 학생과 함께 확인(U10)을 거쳐야 쓰인다.
 */
object DirectImageClient {
    private const val ENDPOINT = "https://api.openai.com/v1/images/generations"
    /** 기기 하나에서 하루에 만들 수 있는 그림 수. 키가 새어도 앱을 통한 과금은 이 안에서 멈춘다. */
    const val DAILY_LIMIT = 20

    val isAvailable: Boolean get() = BuildConfig.OPENAI_API_KEY.isNotBlank()

    /** 그림체 ID(R4 서버와 같은 값) → 그림 지시문. */
    private val STYLE_PROMPTS = mapOf(
        "storybook" to "따뜻한 그림책 일러스트, 부드러운 선과 밝은 색",
        "photo" to "자연스러운 실제 사진 같은 장면, 밝은 자연광",
        "simple" to "아주 단순한 평면 그림, 굵은 윤곽선, 색 몇 가지만",
        "watercolor" to "부드러운 수채화, 번지는 물감 느낌, 연한 색",
        "colored-pencil" to "색연필로 그린 그림, 종이 질감이 살짝 보이는",
        "clay" to "부드러운 3D 찰흙 인형 느낌, 둥근 형태, 파스텔 색",
        "paper-cut" to "색종이를 오려 붙인 느낌, 겹쳐진 종이 층",
        "pixel" to "단순한 픽셀 아트, 큰 픽셀",
        "comic" to "밝은 만화 그림, 깔끔한 선과 선명한 색",
        "line-art" to "검은 선으로만 그린 단순한 선 그림, 흰 배경",
    )

    fun prompt(styleId: String, meaning: String): String = """
        아래 뜻을 글을 읽지 못하는 사람도 그림만 보고 짐작할 수 있게 한 장면으로 그려 주세요.
        - 화풍: ${STYLE_PROMPTS[styleId] ?: STYLE_PROMPTS.getValue("storybook")}
        - 주인공과 핵심 물건을 가운데에 크게, 배경은 단순하게
        - 사람이 나오면 얼굴을 자세히 그리지 말고 뒷모습이나 단순한 형태로
        - 글자, 숫자, 로고, 말풍선은 넣지 않기
        - 어둡거나 무섭거나 다치는 장면은 피하기
        전할 뜻: ${meaning.trim()}
    """.trimIndent()

    /** 오늘 남은 생성 횟수. */
    fun remainingToday(context: Context): Int {
        val p = prefs(context)
        val today = LocalDate.now().toString()
        return if (p.getString(K_DAY, null) == today) (DAILY_LIMIT - p.getInt(K_COUNT, 0)).coerceAtLeast(0) else DAILY_LIMIT
    }

    /** 생성한 이미지 바이트를 돌려준다. 실패하면 사용자에게 보여 줄 문장을 담은 예외를 던진다. */
    suspend fun generate(context: Context, styleId: String, meaning: String): ByteArray = withContext(Dispatchers.IO) {
        require(isAvailable) { "이 앱에는 그림 만들기 키가 들어 있지 않아요." }
        require(meaning.isNotBlank()) { "먼저 전할 뜻을 입력해 주세요." }
        require(remainingToday(context) > 0) { "오늘 만들 수 있는 그림 수(${DAILY_LIMIT}장)를 다 썼어요. 내일 다시 해 주세요." }
        // 비용은 요청을 보낼 때 생기므로 결과와 상관없이 먼저 센다(되풀이 누르기로 한도를 넘지 않게).
        countOne(context)
        // 모델은 계정·시기에 따라 쓸 수 있는 것이 다르다. 설정한 모델부터 차례로 시도하고,
        // '모델 없음·권한 없음·모르는 옵션'처럼 요금이 나가지 않는 오류일 때만 다음으로 넘어간다.
        val text = prompt(styleId, meaning)
        var last = ""
        for (model in (listOf(BuildConfig.OPENAI_IMAGE_MODEL) + FALLBACK_MODELS).distinct()) {
            for (minimal in listOf(false, true)) {
                try {
                    return@withContext request(model, text, minimal)
                } catch (e: NotCharged) {
                    last = "$model: ${e.message}"
                    if (!e.badOption) break // 이 모델은 못 쓴다 → 다음 모델
                }
            }
        }
        error("이 키로 쓸 수 있는 그림 모델을 찾지 못했어요. ($last)")
    }

    private val FALLBACK_MODELS = listOf("gpt-image-2", "gpt-image-1.5", "gpt-image-1", "gpt-image-1-mini", "dall-e-3")

    /** 요청이 받아들여지지 않아 요금이 나가지 않는 오류. badOption이면 같은 모델을 옵션 없이 다시 시도한다. */
    private class NotCharged(message: String, val badOption: Boolean) : Exception(message)

    private fun request(model: String, prompt: String, minimal: Boolean): ByteArray {
        val body = JSONObject()
            .put("model", model)
            .put("prompt", prompt)
            .put("n", 1)
            .apply {
                if (!minimal) {
                    if (model.startsWith("dall-e")) put("size", "1792x1024").put("quality", "standard")
                    else put("size", "1536x1024").put("quality", "medium")
                }
            }
            .toString()
        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 180_000
            setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
            if (code !in 200..299) {
                val detail = runCatching { JSONObject(text).getJSONObject("error").optString("message") }.getOrNull().orEmpty()
                if (code == 400 || code == 403 || code == 404) {
                    if (detail.contains("does not exist", true) || detail.contains("verified", true) ||
                        detail.contains("access to model", true) || detail.contains("not supported", true)
                    ) throw NotCharged(detail.take(100), badOption = false)
                    if (!minimal && (detail.contains("Unknown parameter", true) || detail.contains("Invalid value", true)))
                        throw NotCharged(detail.take(100), badOption = true)
                }
                error(friendlyError(code, text))
            }
            val item = JSONObject(text).getJSONArray("data").getJSONObject(0)
            val b64 = item.optString("b64_json")
            if (b64.isNotBlank()) Base64.decode(b64, Base64.DEFAULT)
            else {
                // 일부 모델은 그림 주소(url)로 돌려준다.
                val url = item.optString("url")
                require(url.isNotBlank()) { "그림을 받지 못했어요. 다시 시도해 주세요." }
                URL(url).openStream().use { it.readBytes() }
            }
        } catch (e: java.net.UnknownHostException) {
            error("인터넷에 연결되어 있지 않아요.")
        } catch (e: java.net.SocketTimeoutException) {
            error("그림을 만드는 데 시간이 너무 오래 걸려요. 결과가 늦게 만들어졌을 수 있어 자동으로 다시 보내지 않았어요.")
        } finally {
            conn.disconnect()
        }
    }

    private fun friendlyError(code: Int, body: String): String {
        val detail = runCatching { JSONObject(body).getJSONObject("error").optString("message") }.getOrNull().orEmpty()
        return when (code) {
            401 -> "그림 만들기 키가 맞지 않아요. 키를 확인해 주세요."
            429 -> "요청이 많거나 키의 사용 한도를 넘었어요. 잠시 후 다시 해 주세요."
            400 -> if (detail.contains("safety", true)) "이 뜻으로는 그림을 만들 수 없어요. 문장을 바꿔 보세요."
                   else "그림을 만들 수 없어요. (${detail.take(80)})"
            else -> "그림을 만들지 못했어요. ($code)"
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences("direct_image", Context.MODE_PRIVATE)

    private fun countOne(context: Context) {
        val p = prefs(context)
        val today = LocalDate.now().toString()
        val n = if (p.getString(K_DAY, null) == today) p.getInt(K_COUNT, 0) else 0
        p.edit().putString(K_DAY, today).putInt(K_COUNT, n + 1).apply()
    }

    private const val K_DAY = "day"
    private const val K_COUNT = "count"
}
