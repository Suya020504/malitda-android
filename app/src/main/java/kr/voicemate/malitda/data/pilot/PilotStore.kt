package kr.voicemate.malitda.data.pilot

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import kr.voicemate.malitda.domain.PilotConfig

private val Context.pilotDataStore by preferencesDataStore("malitda_pilot")

/** 기기 내 실증 설정. Android Keystore의 AES-GCM 키로 프로필별 설정을 암호화한다. */
class PilotStore(context: Context) {
    private val ds = context.applicationContext.pilotDataStore

    fun observe(profileId: Long): Flow<PilotConfig> = ds.data.map { p ->
        decode(profileId, p[stringPreferencesKey("pilot_$profileId")])
    }

    suspend fun get(profileId: Long): PilotConfig = observe(profileId).first()

    suspend fun update(profileId: Long, config: PilotConfig) {
        require(profileId > 0)
        require(config.participantCode.isBlank() || PilotConfig.validParticipantCode(config.participantCode))
        require(PilotConfig.validTaskCode(config.taskCode))
        require(config.condition in PilotConfig.CONDITIONS)
        require(PilotConfig.validTaskVersion(config.taskSetVersion))
        require(config.taskTargets.size <= 20 && config.taskTargets.all { PilotConfig.validTaskCode(it.key) && it.value > 0 })
        val json = JSONObject().apply {
            put("participantCode", config.participantCode)
            put("consented", config.consented)
            put("consentVersion", config.consentVersion)
            put("consentedAt", config.consentedAt)
            put("condition", config.condition)
            put("taskCode", config.taskCode)
            put("targetMeaningId", config.targetMeaningId ?: JSONObject.NULL)
            put("practice", config.practice)
            put("deviceContext", config.deviceContext)
            put("ordinal", config.ordinal)
            put("paused", config.paused)
            put("consecutiveFailures", config.consecutiveFailures)
            put("suitabilityReviewed", config.suitabilityReviewed)
            put("taskSetVersion", config.taskSetVersion)
            put("taskTargets", JSONObject(config.taskTargets.mapValues { it.value }))
        }.toString()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        cipher.updateAAD(profileId.toString().toByteArray(Charsets.UTF_8))
        val bytes = cipher.iv + cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        val encrypted = Base64.encodeToString(bytes, Base64.NO_WRAP)
        ds.edit { it[stringPreferencesKey("pilot_$profileId")] = encrypted }
    }

    /** 철회 후 수집 허용 상태를 복구하지 않는다. 기록 삭제는 같은 프로필의 DAO와 함께 호출한다. */
    suspend fun withdraw(profileId: Long) { ds.edit { it.remove(stringPreferencesKey("pilot_$profileId")) } }
    suspend fun clearAll() { ds.edit { it.clear() } }

    private fun decode(profileId: Long, encoded: String?): PilotConfig {
        if (encoded == null) return PilotConfig()
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            require(bytes.size >= 29)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            }
            cipher.updateAAD(profileId.toString().toByteArray(Charsets.UTF_8))
            val j = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
            PilotConfig(
                participantCode = j.optString("participantCode"), consented = j.optBoolean("consented"),
                consentVersion = j.optString("consentVersion"), consentedAt = j.optLong("consentedAt"),
                condition = j.optString("condition", "C"), taskCode = j.optString("taskCode", "T01"),
                targetMeaningId = if (j.isNull("targetMeaningId")) null else j.optLong("targetMeaningId"),
                practice = j.optBoolean("practice", true), deviceContext = j.optString("deviceContext", "QUIET"),
                ordinal = j.optInt("ordinal"), paused = j.optBoolean("paused"),
                consecutiveFailures = j.optInt("consecutiveFailures"),
                suitabilityReviewed = j.optBoolean("suitabilityReviewed"),
                taskSetVersion = j.optString("taskSetVersion", "local-v1"),
                taskTargets = j.optJSONObject("taskTargets")?.let { tasks ->
                    tasks.keys().asSequence().filter { PilotConfig.validTaskCode(it) }.mapNotNull { code ->
                        tasks.optLong(code).takeIf { it > 0 }?.let { code to it }
                    }.take(20).toMap()
                }.orEmpty(),
            )
        }.getOrElse { PilotConfig() } // 손상·키 분실 때 동의한 것으로 간주하지 않는다.
    }

    @Synchronized private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    private companion object { const val KEY_ALIAS = "malitda_pilot_aes_v1" }
}
