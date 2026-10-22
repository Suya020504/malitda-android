package kr.voicemate.malitda.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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

/** 뜻과 그 뜻으로 이어지는 표현 목록(함께 조회용). */
data class MeaningWithExpressions(
    @androidx.room.Embedded val meaning: MeaningEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "meaningId")
    val expressions: List<MeaningExpressionEntity>,
)

@Dao
interface MeaningDao {
    /** 전달 흐름에 쓰는 뜻: 확인 완료 + 활성. 자주 쓴 순, 같으면 등록한 순(디자인 카드 순서). */
    @Query("SELECT * FROM meanings WHERE profileId = :profileId AND confirmed = 1 AND active = 1 ORDER BY useCount DESC, createdAt ASC")
    fun observeUsable(profileId: Long): Flow<List<MeaningEntity>>
    @Query("SELECT * FROM meanings WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun observeAll(profileId: Long): Flow<List<MeaningEntity>>
    @Query("SELECT * FROM meanings WHERE id = :id") suspend fun byId(id: Long): MeaningEntity?
    @Query("SELECT * FROM meanings WHERE id = :id") fun observe(id: Long): Flow<MeaningEntity?>
    @Query("SELECT COUNT(*) FROM meanings WHERE profileId = :profileId") suspend fun count(profileId: Long): Int
    @Insert suspend fun insert(m: MeaningEntity): Long
    @Update suspend fun update(m: MeaningEntity)
    @Query("DELETE FROM meanings WHERE id = :id") suspend fun delete(id: Long)
    // updatedAt은 내용 변경 감지(U03-V)에 쓰므로 사용 횟수만 올린다.
    @Query("UPDATE meanings SET useCount = useCount + 1 WHERE id = :id")
    suspend fun incrementUse(id: Long)

    @androidx.room.Transaction
    @Query("SELECT * FROM meanings WHERE profileId = :profileId AND confirmed = 1 AND active = 1")
    suspend fun usableWithExpressions(profileId: Long): List<MeaningWithExpressions>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExpression(e: MeaningExpressionEntity): Long
    @Query("SELECT * FROM meaning_expressions WHERE meaningId = :meaningId")
    suspend fun expressionsFor(meaningId: Long): List<MeaningExpressionEntity>
    @Query("DELETE FROM meaning_expressions WHERE meaningId = :meaningId") suspend fun deleteExpressionsFor(meaningId: Long)
}

@Dao
interface CounterDao {
    @Query("SELECT * FROM counters WHERE profileId = :profileId") fun observe(profileId: Long): Flow<CounterEntity?>
    @Query("SELECT * FROM counters WHERE profileId = :profileId") suspend fun get(profileId: Long): CounterEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(c: CounterEntity)
    @Query("DELETE FROM counters WHERE profileId = :profileId") suspend fun delete(profileId: Long)
}
