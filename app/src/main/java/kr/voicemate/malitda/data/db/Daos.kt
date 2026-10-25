package kr.voicemate.malitda.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 현재 P0의 프로필별 등록 한도. DAO 트랜잭션에서 검사해 동시 저장도 넘기지 않는다. */
class MeaningLimitReachedException(val limit: Int = 50) : IllegalStateException("등록할 수 있는 뜻은 최대 ${limit}개입니다.")
class MeaningAlreadyExistsException(val meaningId: Long) : IllegalStateException("같은 내용과 상황의 뜻이 이미 있습니다. 기존 뜻을 수정해야 합니다.")

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY id") fun all(): Flow<List<ProfileEntity>>
    @Query("SELECT * FROM profiles ORDER BY id") suspend fun allOnce(): List<ProfileEntity>
    @Query("SELECT * FROM profiles WHERE id = :id") suspend fun byId(id: Long): ProfileEntity?
    @Query("SELECT * FROM profiles WHERE id = :id") fun observe(id: Long): Flow<ProfileEntity?>
    @Insert suspend fun insert(p: ProfileEntity): Long
    @Update suspend fun update(p: ProfileEntity)
    @Query("DELETE FROM profiles WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface ExpressionDao {
    @Query("SELECT * FROM expressions WHERE profileId = :profileId ORDER BY favorite DESC, updatedAt DESC")
    fun observeAll(profileId: Long): Flow<List<ExpressionEntity>>
    @Query("SELECT * FROM expressions WHERE profileId = :profileId ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(profileId: Long, limit: Int): Flow<List<ExpressionEntity>>
    @Query("SELECT COUNT(*) FROM expressions WHERE profileId = :profileId") fun observeCount(profileId: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM expressions WHERE profileId = :profileId") suspend fun count(profileId: Long): Int
    @Query("SELECT * FROM expressions WHERE id = :id") suspend fun byId(id: Long): ExpressionEntity?
    @Query("SELECT * FROM expressions WHERE id = :id") fun observe(id: Long): Flow<ExpressionEntity?>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(e: ExpressionEntity): Long
    @Update suspend fun update(e: ExpressionEntity)
    @Query("DELETE FROM expressions WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM expressions WHERE profileId = :profileId") suspend fun deleteAll(profileId: Long)
}

@Dao
interface CorrectionDao {
    @Query("SELECT * FROM corrections WHERE profileId = :profileId ORDER BY lastUsedAt DESC")
    fun observeAll(profileId: Long): Flow<List<CorrectionEntity>>
    @Query("SELECT * FROM corrections WHERE profileId = :profileId") suspend fun all(profileId: Long): List<CorrectionEntity>
    @Query("SELECT * FROM corrections WHERE profileId = :profileId AND sourceKey = :sourceKey LIMIT 1")
    suspend fun findExact(profileId: Long, sourceKey: String): CorrectionEntity?
    @Query("SELECT COUNT(*) FROM corrections WHERE profileId = :profileId") fun observeCount(profileId: Long): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(c: CorrectionEntity): Long
    @Update suspend fun update(c: CorrectionEntity)
    @Delete suspend fun delete(c: CorrectionEntity)
    @Query("DELETE FROM corrections WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM corrections WHERE profileId = :profileId") suspend fun deleteAll(profileId: Long)
}

/** 중복 표현 검사 결과: 이 표현이 어느 뜻에 이미 붙어 있는지. */
data class ExpressionConflict(val expression: String, val meaningText: String)

/** 표현 연결의 원자적 추가 결과. 저장소가 기존 공개 결과형으로 변환한다. */
sealed interface ExpressionInsertResult {
    data object Added : ExpressionInsertResult
    data object AlreadyThere : ExpressionInsertResult
    data class TakenBy(val meaningText: String) : ExpressionInsertResult
}

/** 뜻과 그 뜻으로 이어지는 표현 목록(함께 조회용). */
data class MeaningWithExpressions(
    @androidx.room.Embedded val meaning: MeaningEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "meaningId")
    val expressions: List<MeaningExpressionEntity>,
)

@Dao
interface MeaningDao {
    /** 활성 상태의 확인된 개인 뜻 또는 명시적으로 표시한 연습 예시. */
    @Query("SELECT * FROM meanings WHERE profileId = :profileId AND (confirmed = 1 OR isSample = 1) AND active = 1 ORDER BY useCount DESC, createdAt ASC")
    fun observeUsable(profileId: Long): Flow<List<MeaningEntity>>
    @Query("SELECT * FROM meanings WHERE profileId = :profileId ORDER BY favorite DESC, updatedAt DESC")
    fun observeAll(profileId: Long): Flow<List<MeaningEntity>>
    @Query("SELECT * FROM meanings WHERE id = :id") suspend fun byId(id: Long): MeaningEntity?
    @Query("SELECT * FROM meanings WHERE id = :id") fun observe(id: Long): Flow<MeaningEntity?>
    @Query("SELECT COUNT(*) FROM meanings WHERE profileId = :profileId") suspend fun count(profileId: Long): Int
    @Query("SELECT COUNT(*) FROM meanings WHERE profileId = :profileId AND isSample = 0") suspend fun countPersonal(profileId: Long): Int
    @Query("SELECT * FROM meanings WHERE profileId = :profileId") suspend fun allFor(profileId: Long): List<MeaningEntity>
    @Insert suspend fun insert(m: MeaningEntity): Long
    @Update suspend fun update(m: MeaningEntity)
    @Query("DELETE FROM meanings WHERE id = :id") suspend fun delete(id: Long)
    // updatedAt은 내용 변경 감지(U03-V)에 쓰므로 사용 횟수만 올린다.
    @Query("UPDATE meanings SET useCount = useCount + 1, lastUsedAt = :now WHERE id = :id")
    suspend fun incrementUse(id: Long, now: Long)
    // 학생 확인(U10-C) 게이트: 활성화는 내용 변경이 아니므로 updatedAt을 건드리지 않는다.
    @Query("UPDATE meanings SET confirmed = :confirmed, confirmationMethod = NULL, confirmationResponse = NULL, confirmedAt = NULL WHERE id = :id")
    suspend fun setConfirmed(id: Long, confirmed: Boolean)
    @Query("UPDATE meanings SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
    @Query("UPDATE meanings SET favorite = NOT favorite WHERE id = :id") suspend fun toggleFavorite(id: Long)
    @Query("UPDATE meanings SET category = :category WHERE id = :id") suspend fun setCategory(id: Long, category: String)
    @Query("UPDATE meanings SET useCount = 0, lastUsedAt = 0 WHERE profileId = :profileId") suspend fun resetUseCount(profileId: Long)
    @Query("SELECT * FROM confirmation_events WHERE meaningId = :meaningId ORDER BY createdAt DESC, id DESC")
    suspend fun confirmationHistory(meaningId: Long): List<ConfirmationEventEntity>
    @Insert suspend fun insertConfirmation(event: ConfirmationEventEntity): Long
    /** 보관함(U11): 미확인이거나 사용 중지된 뜻 — 학생 후보에 나오지 않는다. */
    @Query("SELECT * FROM meanings WHERE profileId = :profileId AND ((confirmed = 0 AND isSample = 0) OR active = 0) ORDER BY updatedAt DESC")
    fun observeArchived(profileId: Long): Flow<List<MeaningEntity>>

    @androidx.room.Transaction
    @Query("SELECT * FROM meanings WHERE profileId = :profileId AND (confirmed = 1 OR isSample = 1) AND active = 1")
    suspend fun usableWithExpressions(profileId: Long): List<MeaningWithExpressions>
    // 중복 검사 후 경합이 생겨도 다른 뜻의 표현 연결을 덮어쓰지 않는다.
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertExpression(e: MeaningExpressionEntity): Long
    @Query("SELECT * FROM meaning_expressions WHERE meaningId = :meaningId")
    suspend fun expressionsFor(meaningId: Long): List<MeaningExpressionEntity>
    /** 다른 뜻에 이미 연결된 표현 찾기(중복 등록 경고용). */
    @Query(
        "SELECT me.text AS expression, m.text AS meaningText FROM meaning_expressions me " +
            "JOIN meanings m ON m.id = me.meaningId " +
            "WHERE me.profileId = :profileId AND me.key IN (:keys) AND me.meaningId != :excludeMeaningId"
    )
    suspend fun findExpressionConflicts(profileId: Long, keys: List<String>, excludeMeaningId: Long): List<ExpressionConflict>
    @Query("DELETE FROM meaning_expressions WHERE meaningId = :meaningId") suspend fun deleteExpressionsFor(meaningId: Long)
    /** 모든 프로필이 참조 중인 그림 키(사진 정리용). */
    @Query("SELECT imageKey FROM meanings WHERE imageKey IS NOT NULL")
    suspend fun allImageKeys(): List<String>

    /** 연속 탭·동시 요청도 같은 표현을 한 번만 추가하고, 다른 프로필의 뜻에는 연결하지 않는다. */
    @Transaction
    suspend fun addExpressionIfAvailable(expression: MeaningExpressionEntity): ExpressionInsertResult {
        require(expression.key.isNotBlank())
        val meaning = requireNotNull(byId(expression.meaningId)) { "Meaning no longer exists" }
        require(meaning.profileId == expression.profileId) { "Meaning belongs to a different profile" }
        if (expressionsFor(meaning.id).any { it.key == expression.key }) return ExpressionInsertResult.AlreadyThere
        insertExpression(expression)
        return ExpressionInsertResult.Added
    }

    @Transaction
    suspend fun insertWithExpressions(meaning: MeaningEntity, expressions: List<MeaningExpressionEntity>): Long {
        if (!meaning.isSample) {
            allFor(meaning.profileId).firstOrNull {
                !it.isSample && it.text == meaning.text && it.imageKey == meaning.imageKey && it.situation == meaning.situation
            }?.let { throw MeaningAlreadyExistsException(it.id) }
        }
        if (!meaning.isSample && countPersonal(meaning.profileId) >= 50) throw MeaningLimitReachedException()
        val id = insert(meaning)
        expressions.forEach { insertExpression(it.copy(profileId = meaning.profileId, meaningId = id)) }
        return id
    }

    @Transaction
    suspend fun updateWithExpressions(
        id: Long,
        text: String,
        imageKey: String?,
        situation: String?,
        expressions: List<MeaningExpressionEntity>,
        now: Long,
        category: String? = null,
    ) {
        val current = byId(id) ?: return
        deleteExpressionsFor(id)
        expressions.forEach { insertExpression(it.copy(profileId = current.profileId, meaningId = id)) }
        update(
            current.copy(
                text = text, imageKey = imageKey, situation = situation,
                confirmed = false, isSample = false,
                confirmationMethod = null, confirmationResponse = null, confirmedAt = null,
                updatedAt = maxOf(now, current.updatedAt + 1),
                category = category ?: current.category,
            )
        )
    }

    @Transaction
    suspend fun deleteWithExpressions(id: Long) {
        deleteConfirmationHistory(id)
        deleteExpressionsFor(id)
        delete(id)
    }

    @Transaction
    suspend fun removeSamples(profileId: Long): Int {
        val samples = allFor(profileId).filter { it.isSample }
        samples.forEach { deleteWithExpressions(it.id) }
        return samples.size
    }

    @Transaction
    suspend fun seedSamplesIfEmpty(profileId: Long, samples: List<MeaningWithExpressions>) {
        if (count(profileId) > 0) return
        samples.forEach { insertWithExpressions(it.meaning, it.expressions) }
    }

    /** 확인할 때 본 버전만 활성화한다. 내용 수정과 동시에 실행되어도 근거가 뒤섞이지 않는다. */
    @Transaction
    suspend fun activateConfirmed(id: Long, profileId: Long, expectedUpdatedAt: Long, method: String, now: Long, confirmedBy: String = "supporter"): Boolean =
        recordConfirmationResponse(id, profileId, expectedUpdatedAt, "matched", method, confirmedBy, now)

    /** 학생 반응·지원자 코드를 확인했던 내용 버전과 같은 트랜잭션에서 저장한다. */
    @Transaction
    suspend fun recordConfirmationResponse(
        id: Long, profileId: Long, expectedUpdatedAt: Long, response: String,
        method: String?, confirmedBy: String, now: Long,
    ): Boolean {
        require(response in setOf("matched", "different", "unsure", "no_response"))
        require(method == null || method in setOf("speech", "pointing", "gaze", "other"))
        if (response == "matched") require(method != null)
        require(confirmedBy.isNotBlank() && confirmedBy.length <= 64)
        val current = byId(id) ?: return false
        if (current.profileId != profileId || current.updatedAt != expectedUpdatedAt) return false
        insertConfirmation(ConfirmationEventEntity(
            profileId = profileId, meaningId = id, meaningVersion = expectedUpdatedAt,
            response = response, method = method, confirmedBy = confirmedBy, createdAt = now,
        ))
        val matched = response == "matched"
        update(
            current.copy(
                active = if (matched) true else current.active, confirmed = matched, isSample = false,
                confirmationMethod = method, confirmationResponse = response, confirmedAt = now,
            )
        )
        return true
    }

    @Query("DELETE FROM confirmation_events WHERE meaningId = :meaningId")
    suspend fun deleteConfirmationHistory(meaningId: Long)
}

@Dao
interface TrialLogDao {
    @Insert suspend fun insert(t: TrialLogEntity): Long
    @Query("SELECT * FROM trial_logs WHERE profileId = :profileId ORDER BY startedAt DESC")
    fun observeAll(profileId: Long): Flow<List<TrialLogEntity>>
    @Query("SELECT * FROM trial_logs ORDER BY profileId, startedAt")
    suspend fun allProfiles(): List<TrialLogEntity>
    @Query("SELECT * FROM trial_logs WHERE profileId = :profileId ORDER BY startedAt")
    suspend fun allFor(profileId: Long): List<TrialLogEntity>
    @Query("DELETE FROM trial_logs WHERE profileId = :profileId") suspend fun deleteAll(profileId: Long)
}

/** 현재 개인 의미·실증·확인 이력까지 같은 트랜잭션에서 삭제한다. */
@Dao
interface DataManagementDao {
    @Query("DELETE FROM confirmation_events WHERE profileId = :profileId") suspend fun deleteConfirmations(profileId: Long)
    @Query("DELETE FROM meaning_expressions WHERE profileId = :profileId") suspend fun deleteMeaningExpressions(profileId: Long)
    @Query("DELETE FROM meanings WHERE profileId = :profileId") suspend fun deleteMeanings(profileId: Long)
    @Query("DELETE FROM trial_logs WHERE profileId = :profileId") suspend fun deleteTrials(profileId: Long)
    @Query("DELETE FROM expressions WHERE profileId = :profileId") suspend fun deleteExpressions(profileId: Long)
    @Query("DELETE FROM corrections WHERE profileId = :profileId") suspend fun deleteCorrections(profileId: Long)
    @Query("DELETE FROM counters WHERE profileId = :profileId") suspend fun deleteCounters(profileId: Long)
    @Query("DELETE FROM profiles WHERE id = :profileId") suspend fun deleteProfile(profileId: Long)
}

@Dao
interface CounterDao {
    @Query("SELECT * FROM counters WHERE profileId = :profileId") fun observe(profileId: Long): Flow<CounterEntity?>
    @Query("SELECT * FROM counters WHERE profileId = :profileId") suspend fun get(profileId: Long): CounterEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(c: CounterEntity)
    @Query("DELETE FROM counters WHERE profileId = :profileId") suspend fun delete(profileId: Long)
}
