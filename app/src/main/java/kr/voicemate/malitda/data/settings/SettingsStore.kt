package kr.voicemate.malitda.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "malitda_settings")

data class Settings(
    val onboarded: Boolean = false,
    val consentAccepted: Boolean = false,
    val fontScale: Float = 1.0f,       // 보통 1.0 / 크게 1.2
    val ttsRate: Float = 1.0f,         // 느림 0.8 / 보통 1.0
    val haptics: Boolean = true,
    val visualEmphasis: Boolean = false,
    val rewardEnabled: Boolean = false,
    val currentProfileId: Long = 0L,
    /** 음성인식 엔진 id(SttRouter.WHISPER / VOSK). 사전 비교 결과에 따라 기본값을 정한다. */
    val sttEngine: String = "whisper",
    /** v2 기본 의미 4종을 넣었는지(최초 1회만). */
    val v2Seeded: Boolean = false,
    /** 실증 기록. 기본 꺼짐 — 지원자가 설정에서 켰을 때만 대화 시도 결과를 남긴다. */
    val trialLogging: Boolean = false,
)

class SettingsStore(context: Context) {
    private val ds = context.applicationContext.dataStore

    val settings: Flow<Settings> = ds.data.map { p ->
        Settings(
            onboarded = p[K.onboarded] ?: false,
            consentAccepted = p[K.consent] ?: false,
            fontScale = p[K.fontScale] ?: 1.0f,
            ttsRate = p[K.ttsRate] ?: 1.0f,
            haptics = p[K.haptics] ?: true,
            visualEmphasis = p[K.visual] ?: false,
            rewardEnabled = p[K.reward] ?: false,
            currentProfileId = p[K.profile] ?: 0L,
            sttEngine = p[K.sttEngine] ?: "whisper",
            v2Seeded = p[K.v2Seeded] ?: false,
            trialLogging = p[K.trialLogging] ?: false,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setOnboarded(v: Boolean) { ds.edit { it[K.onboarded] = v } }
    suspend fun setConsent(v: Boolean) { ds.edit { it[K.consent] = v } }
    suspend fun setFontScale(v: Float) { ds.edit { it[K.fontScale] = v } }
    suspend fun setTtsRate(v: Float) { ds.edit { it[K.ttsRate] = v } }
    suspend fun setHaptics(v: Boolean) { ds.edit { it[K.haptics] = v } }
    suspend fun setVisualEmphasis(v: Boolean) { ds.edit { it[K.visual] = v } }
    suspend fun setRewardEnabled(v: Boolean) { ds.edit { it[K.reward] = v } }
    suspend fun setCurrentProfile(id: Long) { ds.edit { it[K.profile] = id } }
    suspend fun setSttEngine(id: String) { ds.edit { it[K.sttEngine] = id } }
    suspend fun setV2Seeded(v: Boolean) { ds.edit { it[K.v2Seeded] = v } }
    suspend fun setTrialLogging(v: Boolean) { ds.edit { it[K.trialLogging] = v } }

    // ---------- 학생별 그림체·R4 익명 ID ----------
    fun artStyle(profileId: Long): Flow<String> =
        ds.data.map { kr.voicemate.malitda.domain.ArtStyles.normalize(it[stringKey("art_style_$profileId")]) }

    suspend fun setArtStyle(profileId: Long, styleId: String) {
        ds.edit { it[stringKey("art_style_$profileId")] = kr.voicemate.malitda.domain.ArtStyles.normalize(styleId) }
    }

    /** R4 서버에 보낼 익명 ID. 학생 이름·DB id 대신 이 무작위 값만 서버에 간다. */
    suspend fun r4AnonId(profileId: Long): String {
        val key = stringKey("r4_anon_$profileId")
        ds.data.first()[key]?.let { return it }
        val created = java.util.UUID.randomUUID().toString()
        var result = created
        ds.edit { p -> result = p[key] ?: created.also { p[key] = it } }
        return result
    }

    private fun stringKey(name: String) = androidx.datastore.preferences.core.stringPreferencesKey(name)
    suspend fun clearAll() { ds.edit { it.clear() } }

    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val consent = booleanPreferencesKey("consent_accepted")
        val fontScale = floatPreferencesKey("font_scale")
        val ttsRate = floatPreferencesKey("tts_rate")
        val haptics = booleanPreferencesKey("haptics")
        val visual = booleanPreferencesKey("visual_emphasis")
        val reward = booleanPreferencesKey("reward_enabled")
        val profile = longPreferencesKey("current_profile")
        val sttEngine = androidx.datastore.preferences.core.stringPreferencesKey("stt_engine")
        val v2Seeded = booleanPreferencesKey("v2_seeded")
        val trialLogging = booleanPreferencesKey("trial_logging")
    }
}
