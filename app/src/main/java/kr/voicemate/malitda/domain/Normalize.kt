package kr.voicemate.malitda.domain

import java.security.MessageDigest
import java.text.Normalizer

/**
 * M1 "완전 일치" 판정에 쓰는 정규화. 규칙은 이 세 가지뿐이며 유사도·편집거리는 쓰지 않는다.
 *  1) 유니코드 NFC 정규화 + 양끝 공백 제거
 *  2) 연속 공백을 하나로
 *  3) 문장 끝의 문장부호(. ! ? … ,)만 제거
 */
object TextNormalizer {
    private val WS = Regex("\\s+")
    private val TRAILING = charArrayOf('.', '!', '?', '…', ',', ' ', '。')

    fun normalize(text: String): String {
        val nfc = Normalizer.normalize(text, Normalizer.Form.NFC).trim()
        val collapsed = nfc.replace(WS, " ")
        return collapsed.trimEnd(*TRAILING)
    }

    /**
     * 후보 비교 전용 정규화. 화면 원문과 승인 토큰에는 쓰지 않는다.
     * 수 단위가 분명한 한/두/세 등만 바꾸며, 한강·두바이 같은 단어는 유지한다.
     */
    fun comparison(text: String): String {
        val digits = normalize(text).map { ch ->
            if (ch in '０'..'９') ('0'.code + ch.code - '０'.code).toChar() else ch
        }.joinToString("")
        val grouped = digits.replace(Regex("(?<=\\d),(?=\\d{3}(?:\\D|$))"), "")
        val nativeNumbers = mapOf(
            "한" to "1", "두" to "2", "세" to "3", "네" to "4", "다섯" to "5",
            "여섯" to "6", "일곱" to "7", "여덟" to "8", "아홉" to "9", "열" to "10",
        )
        val units = "(?:시간|시|분|초|원|개|명|번|층|살|일|월|년)"
        val nativeUnits = "(?:시간|시|개|명|번|층|살)"
        val unitBoundary = "(?=$|\\s|[.,!?]|(?:에|에서|부터|까지|마다|만|은|는|이|가|을|를|과|와|의|로|보다)(?:\\s|$|[.,!?]))"
        val numbered = grouped.replace(Regex("(^|\\s)(한|두|세|네|다섯|여섯|일곱|여덟|아홉|열)\\s*($nativeUnits)$unitBoundary")) { match ->
            match.groupValues[1] + nativeNumbers.getValue(match.groupValues[2]) + match.groupValues[3]
        }
        // 한자어 수사는 단위와 공백으로 분리된 경우만 비교한다. '일시' 같은 일반 명사는 유지한다.
        val sino = numbered.replace(Regex("(^|\\s)([영공일이삼사오육칠팔구십백천]+)\\s+($units)$unitBoundary")) { match ->
            val value = sinoNumber(match.groupValues[2])
            if (value == null) match.value else match.groupValues[1] + value + match.groupValues[3]
        }
        // 숫자에 붙은 단위 앞 공백을 맞춘다. 숫자 자체·시간·금액을 임의로 바꾸지 않는다.
        return sino.replace(Regex("(\\d)\\s+($units)"), "$1$2")
            .replace(Regex("(?<!\\d)[.,:]|[.,:](?!\\d)|[;!?…。]"), " ").replace(WS, " ").trim()
    }

    private fun sinoNumber(text: String): String? {
        if (text.length > 8) return null
        val values = mapOf('영' to 0, '공' to 0, '일' to 1, '이' to 2, '삼' to 3, '사' to 4, '오' to 5, '육' to 6, '칠' to 7, '팔' to 8, '구' to 9)
        if (text.none { it in "십백천" }) return text.map { values[it] ?: return null }.joinToString("")
        var total = 0
        var digit = 0
        var previousUnit = 10_000
        for (ch in text) {
            if (ch in values) {
                if (digit != 0) return null
                digit = values.getValue(ch)
            } else {
                val unit = when (ch) { '십' -> 10; '백' -> 100; '천' -> 1000; else -> return null }
                if (unit >= previousUnit) return null
                total += (if (digit == 0) 1 else digit) * unit
                digit = 0
                previousUnit = unit
            }
        }
        return (total + digit).toString()
    }

    fun sha256(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
