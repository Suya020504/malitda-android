package kr.voicemate.malitda.domain

/** 현재 개인 뜻 화면의 저장 키. 구형 표현 사전의 키도 의미가 같은 현재 분류로 읽는다. */
object MeaningCategory {
    val keys = setOf("people", "places", "time", "daily", "request", "other", "often")

    fun normalize(key: String?): String = when (key) {
        "name" -> "people"
        "place" -> "places"
        "message" -> "daily"
        in keys -> key!!
        else -> "often"
    }

    fun isIdentifier(key: String): Boolean = normalize(key) in setOf("people", "places")
}
