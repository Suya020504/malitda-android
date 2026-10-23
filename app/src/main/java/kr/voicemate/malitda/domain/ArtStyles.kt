package kr.voicemate.malitda.domain

/**
 * 학생별 그림체 10종. R4 서버(style_profiles.py)의 style_id와 같은 값을 쓴다.
 * 그림체 선택은 '선호'일 뿐이다 — 뜻 확인이나 전달 승인이 아니다.
 */
object ArtStyles {
    data class Style(val id: String, val label: String)

    val all: List<Style> = listOf(
        Style("storybook", "이야기책"),
        Style("photo", "사진형"),
        Style("simple", "단순 그림"),
        Style("watercolor", "수채화"),
        Style("colored-pencil", "색연필"),
        Style("clay", "찰흙 인형"),
        Style("paper-cut", "종이 오리기"),
        Style("pixel", "픽셀 그림"),
        Style("comic", "만화"),
        Style("line-art", "선 그림"),
    )

    const val DEFAULT = "storybook"

    fun label(id: String?): String = all.firstOrNull { it.id == id }?.label ?: all.first().label
    fun normalize(id: String?): String = id?.takeIf { s -> all.any { it.id == s } } ?: DEFAULT
}

/**
 * R4 서버 응답 → 사용자 문구. 유료 호출 결과가 불확실한 경우(502·504)는 자동으로 다시 보내지 않는다.
 * HTTP 처리와 분리해 단위 테스트로 검증한다.
 */
object R4Messages {
    fun forStatus(status: Int, reviewMode: Boolean): String = when {
        reviewMode || status == 403 -> "지금 PC 서버는 점검 모드라 새 그림을 만들 수 없어요. 사진이나 기본 그림을 써 주세요."
        status == 409 -> "그림체 설정이 방금 바뀌었어요. 다시 눌러 주세요."
        status == 429 -> "이번 서버 실행의 생성 횟수를 다 썼거나 다른 그림을 만드는 중이에요. 잠시 후 다시 해 주세요."
        status == 503 -> "PC 서버에 그림 생성 키가 설정되지 않았어요."
        status == 422 -> "서버가 뜻 그림 요청을 받지 못했어요. 서버를 최신 버전으로 맞춰 주세요."
        status == 504 -> "시간이 너무 오래 걸렸어요. 비용이 이미 들었을 수 있어 자동으로 다시 보내지 않았어요."
        status == 502 -> "그림을 만들지 못했어요. 비용이 들었을 수 있어 자동으로 다시 보내지 않았어요."
        else -> "그림을 만들지 못했어요. ($status)"
    }

    const val UNREACHABLE = "PC 서버에 연결할 수 없어요. 서버를 켜고 연결해 주세요."
}

/** 서버의 학생 그림체 설정을 앱 선택과 맞추기 위해 할 일. */
sealed interface StyleSync {
    /** 서버에 아직 설정이 없다 → 버전 0에서 새로 저장. */
    data object Create : StyleSync
    /** 서버 설정이 앱 선택과 다르다 → 현재 버전을 기준으로 바꾼다. */
    data class Update(val expectedRevision: Int) : StyleSync
    /** 이미 같다 → 이 버전으로 생성. */
    data class Ready(val revision: Int) : StyleSync

    companion object {
        fun decide(serverStyle: String?, serverRevision: Int?, wanted: String): StyleSync = when {
            serverStyle == null || serverRevision == null -> Create
            serverStyle != wanted -> Update(serverRevision)
            else -> Ready(serverRevision)
        }
    }
}
