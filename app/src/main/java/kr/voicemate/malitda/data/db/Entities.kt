package kr.voicemate.malitda.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

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

/** 개인 의미는 확인된 상태일 때만 발화 해석 후보로 사용할 수 있다. */
@Entity(
    tableName = "meaning_entries",
    foreignKeys = [ForeignKey(
        entity = ProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["profileId", "id"], unique = true)],
)
data class MeaningEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val displayText: String,
    val imageRef: String? = null,
    val contextLabel: String? = null,
    val status: String = DRAFT,
    @ColumnInfo(defaultValue = "1") val version: Long = 1,
    val confirmationMethod: String? = null,
    val confirmedBy: String? = null,
    val confirmedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        const val DRAFT = "DRAFT"
        const val CONFIRMED = "CONFIRMED"
        const val INACTIVE = "INACTIVE"
    }
}

/** 같은 별칭을 여러 의미에 연결할 수 있지만 다른 프로필의 의미에는 연결할 수 없다. */
@Entity(
    tableName = "expression_aliases",
    foreignKeys = [ForeignKey(
        entity = MeaningEntry::class,
        parentColumns = ["profileId", "id"],
        childColumns = ["profileId", "meaningId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index(value = ["profileId", "normalizedAlias", "meaningId"], unique = true),
        Index(value = ["profileId", "meaningId"]),
        Index(value = ["meaningId"]),
    ],
)
data class ExpressionAlias(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val meaningId: Long,
    val rawAlias: String,
    val normalizedAlias: String,
)

data class MeaningWithAliases(
    @Embedded val meaning: MeaningEntry,
    @Relation(parentColumn = "id", entityColumn = "meaningId")
    val aliases: List<ExpressionAlias>,
)
