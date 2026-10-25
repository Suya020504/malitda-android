package kr.voicemate.malitda.data.db

import androidx.room.ColumnInfo
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
    /** 학생과 함께 확인을 마친 개인 뜻. 연습 예시는 isSample로 별도 구분한다. */
    val confirmed: Boolean,
    /** 비활성(U09-I)이면 홈·후보에서 제외. */
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val useCount: Int,
    /** 앱이 넣은 연습 예시인지 표시. 과거 출처가 불명확한 기록은 개인 기록으로 보존한다. */
    @ColumnInfo(defaultValue = "0") val isSample: Boolean = false,
    val confirmationMethod: String? = null,
    val confirmationResponse: String? = null,
    val confirmedAt: Long? = null,
    @ColumnInfo(defaultValue = "'often'") val category: String = "often",
    @ColumnInfo(defaultValue = "0") val favorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") val lastUsedAt: Long = 0,
    /** 이전 의미 사전 분기에서 가져온 원래 버전·확인자. 새 확인 근거로 추정하지 않는다. */
    @ColumnInfo(defaultValue = "NULL") val legacyVersion: Long? = null,
    @ColumnInfo(defaultValue = "NULL") val legacyConfirmedBy: String? = null,
)

/**
 * 실증 기록(DB v3): 대화 시도 한 번의 결과. 설정에서 켰을 때만 남긴다.
 * 새 기록에는 학생 이름·음성·원문·뜻 문장을 저장하지 않는다. 동의한 익명 코드·결과 지표만 남긴다.
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
    /** 구버전 호환 필드. 신규 기록은 항상 null이며 익명 내보내기에는 포함하지 않는다. */
    val meaningText: String?,
    /** 실증 참여 동의가 확인된 뒤 설정한 임의 코드. 이름·원문 대신 코드와 수치만 반출한다. */
    @ColumnInfo(defaultValue = "''") val participantCode: String = "",
    @ColumnInfo(defaultValue = "'C'") val condition: String = "C",
    @ColumnInfo(defaultValue = "''") val taskCode: String = "",
    @ColumnInfo(defaultValue = "NULL") val targetMeaningId: Long? = null,
    @ColumnInfo(defaultValue = "'COMPLETED'") val outcome: String = "COMPLETED",
    @ColumnInfo(defaultValue = "1") val practice: Boolean = true,
    @ColumnInfo(defaultValue = "0") val consented: Boolean = false,
    @ColumnInfo(defaultValue = "''") val deviceContext: String = "",
    @ColumnInfo(defaultValue = "''") val qualitativeNote: String = "",
    @ColumnInfo(defaultValue = "-1") val judgedCorrect: Int = -1,
    @ColumnInfo(defaultValue = "0") val retryCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val editCount: Int = 0,
    @ColumnInfo(defaultValue = "''") val stopReason: String = "",
    @ColumnInfo(defaultValue = "0") val accidentalSendCount: Int = 0,
    @ColumnInfo(defaultValue = "'local-v1'") val taskSetVersion: String = "local-v1",
)

/** 같은 표현도 상황에 따라 여러 뜻을 가질 수 있다. 같은 뜻에 같은 표현을 중복 저장하지 않는다. */
@Entity(
    tableName = "meaning_expressions",
    indices = [Index(value = ["profileId", "key", "meaningId"], unique = true), Index("meaningId")],
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

/** 본인이 뜻을 확인하거나 거절한 기록. 확인자는 이름 대신 기기 안 지원자 코드를 쓴다. */
@Entity(tableName = "confirmation_events", indices = [Index("profileId"), Index("meaningId")])
data class ConfirmationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val meaningId: Long,
    val meaningVersion: Long,
    val response: String,
    val method: String?,
    val confirmedBy: String,
    val createdAt: Long,
)
