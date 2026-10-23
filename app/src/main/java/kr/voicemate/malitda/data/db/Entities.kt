package kr.voicemate.malitda.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "expressions", indices = [Index("profileId")])
data class ExpressionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val category: String,
    val label: String,
    val text: String,
    val favorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

/** M1 승인 교정: (사용자, 정규화된 오인식) 당 승인문장 하나. */
@Entity(tableName = "corrections", indices = [Index(value = ["profileId", "sourceKey"], unique = true)])
data class CorrectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val sourceKey: String,
    val sourceRaw: String,
    val approvedText: String,
    val createdAt: Long,
    val lastUsedAt: Long,
    val useCount: Int = 0,
)

/** 리워드·통계용 카운터(문장 내용은 저장하지 않음). */
@Entity(tableName = "counters")
data class CounterEntity(
    @PrimaryKey val profileId: Long,
    val approvals: Int = 0,
    val shares: Int = 0,
    val expressionsAdded: Int = 0,
    val lastActiveDay: String = "",
    val streakDays: Int = 0,
    val mainCharacter: String? = null,
    val friendsSeen: Int = 1,
)

/** v2 개인 의미 사전: 지원자와 함께 등록한 뜻(그림·문장). DB v2에서 추가. */
@Entity(tableName = "meanings", indices = [Index("profileId")])
data class MeaningEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    /** 전할 뜻 문장(예: "산책하고 싶어요"). */
    val text: String,
    /** 내장 의미 그림 키(walk/water/rest/cold). null이면 그림 없음(U03-I). */
    val imageKey: String?,
    /** 사용 상황 메모(선택). */
    val situation: String?,
    /** 학생과 함께 확인을 마친 뜻만 전달 흐름에 나온다(U10-C). */
    val confirmed: Boolean,
    /** 비활성(U09-I)이면 홈·후보에서 제외. */
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val useCount: Int,
)

/**
 * 실증 기록(DB v3): 대화 시도 한 번의 결과. 설정에서 켰을 때만 남긴다.
 * 학생이 한 말·앱이 들은 문장은 저장하지 않는다 — 결과 종류와 전달한 뜻만 남긴다.
 */
@Entity(tableName = "trial_logs", indices = [Index("profileId")])
data class TrialLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val startedAt: Long,
    val endedAt: Long,
    /** 시작 방식: VOICE(말하기) / CARD(홈 카드). */
    val origin: String,
    /** 말로 시작해 첫 후보가 바로 맞았는가. */
    val voiceFirstHit: Boolean,
    /** 말로 시작했다가 카드로 바꿨는가. */
    val usedCardFallback: Boolean,
    /** '다른 뜻이에요'를 누른 횟수. */
    val otherMeaningCount: Int,
    /** 학생이 뜻을 확정해 전달 화면까지 갔는가. */
    val delivered: Boolean,
    /** 상대가 이해했는가: 1 예, 0 아니요, -1 기록 안 함. */
    val understood: Int,
    val supporterHelped: Boolean,
    val meaningId: Long?,
    val meaningText: String?,
)

/** 표현→뜻 연결: 정규화된 표현(key) 하나가 뜻 하나를 가리킨다. */
@Entity(
    tableName = "meaning_expressions",
    indices = [Index(value = ["profileId", "key"], unique = true), Index("meaningId")],
)
data class MeaningExpressionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val meaningId: Long,
    /** 평소 쓰는 표현 원문(예: "바람"). */
    val text: String,
    /** TextNormalizer.normalize(text) 결과. 조회 키. */
    val key: String,
)
