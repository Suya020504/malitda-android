package kr.voicemate.malitda.data.repo

import androidx.room.withTransaction
import kr.voicemate.malitda.data.db.AppDatabase
import kr.voicemate.malitda.data.db.ExpressionAlias
import kr.voicemate.malitda.data.db.MeaningEntry
import kr.voicemate.malitda.data.db.MeaningWithAliases
import kotlinx.coroutines.flow.Flow
import java.text.Normalizer

/** 확인 상태·별칭·버전을 한 트랜잭션으로 저장하는 로컬 개인 의미 저장소. */
class MeaningRepository(private val database: AppDatabase) {
    private val meanings = database.meaningDao()
    private val aliases = database.expressionAliasDao()

    sealed interface SaveResult {
        data class Ok(val id: Long, val version: Long) : SaveResult
        data class Invalid(val message: String) : SaveResult
        data object Conflict : SaveResult
        data object LimitReached : SaveResult
    }

    fun observe(profileId: Long): Flow<List<MeaningWithAliases>> = meanings.observe(profileId)

    suspend fun get(profileId: Long, id: Long): MeaningWithAliases? = meanings.get(profileId, id)

    /** 저장은 항상 초안이다. 확인자 정보는 confirm에서만 기록한다. */
    @Suppress("UNUSED_PARAMETER")
    suspend fun save(
        profileId: Long,
        id: Long?,
        expectedVersion: Long?,
        rawAliases: List<String>,
        displayText: String,
        imageRef: String?,
        contextLabel: String?,
        confirmedBy: String?,
    ): SaveResult {
        val text = displayText.trim()
        val context = contextLabel?.trim()?.ifBlank { null }
        if (text.isEmpty()) return SaveResult.Invalid("뜻을 입력해 주세요.")
        if (text.length > MAX_MEANING_LENGTH) return SaveResult.Invalid("뜻은 ${MAX_MEANING_LENGTH}자까지 입력할 수 있어요.")
        if (context != null && context.length > MAX_CONTEXT_LENGTH) return SaveResult.Invalid("상황은 ${MAX_CONTEXT_LENGTH}자까지 입력할 수 있어요.")
        if (rawAliases.size > MAX_ALIASES) return SaveResult.Invalid("표현은 ${MAX_ALIASES}개까지 입력할 수 있어요.")
        if (rawAliases.any { it.length > MAX_ALIAS_LENGTH }) return SaveResult.Invalid("표현은 ${MAX_ALIAS_LENGTH}자까지 입력할 수 있어요.")
        val preparedAliases = rawAliases.map { it to normalizeAlias(it) }
            .filter { (_, normalized) -> normalized.isNotEmpty() }
            .distinctBy { (_, normalized) -> normalized }
        if (preparedAliases.isEmpty()) return SaveResult.Invalid("표현을 하나 이상 입력해 주세요.")

        return database.withTransaction {
            if (database.profileDao().byId(profileId) == null) {
                return@withTransaction SaveResult.Invalid("사용자를 찾을 수 없어요.")
            }
            val now = System.currentTimeMillis()
            val savedId: Long
            val savedVersion: Long
            if (id == null) {
                if (expectedVersion != null) return@withTransaction SaveResult.Conflict
                if (meanings.count(profileId) >= MAX_MEANINGS) return@withTransaction SaveResult.LimitReached
                savedId = meanings.insert(MeaningEntry(
                    profileId = profileId, displayText = text, imageRef = imageRef,
                    contextLabel = context, createdAt = now, updatedAt = now,
                ))
                savedVersion = 1
            } else {
                val old = meanings.get(profileId, id)?.meaning ?: return@withTransaction SaveResult.Conflict
                if (expectedVersion == null || old.version != expectedVersion || old.version == Long.MAX_VALUE) {
                    return@withTransaction SaveResult.Conflict
                }
                if (meanings.updateDraft(profileId, id, expectedVersion, text, imageRef, context, now) != 1) {
                    return@withTransaction SaveResult.Conflict
                }
                savedId = id
                savedVersion = old.version + 1
                aliases.deleteForMeaning(profileId, id)
            }
            aliases.insertAll(preparedAliases.map { (raw, normalized) ->
                ExpressionAlias(profileId = profileId, meaningId = savedId, rawAlias = raw, normalizedAlias = normalized)
            })
            SaveResult.Ok(savedId, savedVersion)
        }
    }

    /** 확인 방법은 필수이며 함께 확인한 지원자의 이름은 선택 입력이다. */
    suspend fun confirm(
        profileId: Long, id: Long, expectedVersion: Long, method: String, confirmedBy: String?,
    ): Boolean {
        val confirmationMethod = method.trim()
        val supporter = confirmedBy?.trim()?.ifBlank { null }
        if (confirmationMethod.isEmpty() || confirmationMethod.length > MAX_CONFIRMATION_METHOD_LENGTH) return false
        if (supporter != null && supporter.length > MAX_SUPPORTER_LENGTH) return false
        if (expectedVersion < 1 || expectedVersion == Long.MAX_VALUE) return false
        return database.withTransaction {
            meanings.confirm(
                profileId, id, expectedVersion, confirmationMethod,
                supporter, System.currentTimeMillis(),
            ) == 1
        }
    }

    suspend fun deactivate(profileId: Long, id: Long, expectedVersion: Long): Boolean {
        if (expectedVersion < 1 || expectedVersion == Long.MAX_VALUE) return false
        return database.withTransaction {
            meanings.deactivate(profileId, id, expectedVersion, System.currentTimeMillis()) == 1
        }
    }

    suspend fun delete(profileId: Long, id: Long): Boolean = database.withTransaction {
        meanings.delete(profileId, id) == 1
    }

    companion object {
        const val MAX_MEANINGS = 50
        const val MAX_ALIASES = 5
        const val MAX_ALIAS_LENGTH = 80
        const val MAX_MEANING_LENGTH = 200
        const val MAX_CONTEXT_LENGTH = 120
        const val MAX_CONFIRMATION_METHOD_LENGTH = 120
        const val MAX_SUPPORTER_LENGTH = 80
        private val whitespace = Regex("[\\s\\p{Z}]+")

        /** 별칭은 NFC·앞뒤 공백·연속 공백만 정리하며 문장부호를 보존한다. */
        fun normalizeAlias(raw: String): String =
            Normalizer.normalize(raw, Normalizer.Form.NFC).trim().replace(whitespace, " ")
    }
}
