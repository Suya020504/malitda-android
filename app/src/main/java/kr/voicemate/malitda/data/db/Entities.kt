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
