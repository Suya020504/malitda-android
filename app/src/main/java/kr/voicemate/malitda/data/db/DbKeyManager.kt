package kr.voicemate.malitda.data.db

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * DB 암호화 키 관리.
 *  - 무작위 32바이트 패스프레이즈를 생성해 Android Keystore의 AES-GCM 키로 감싼 뒤 SharedPreferences에 보관한다.
 *  - Keystore 키는 기기 밖으로 나가지 않으며 앱 삭제 시 함께 사라진다.
 */
class DbKeyManager(private val context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("malitda_keys", Context.MODE_PRIVATE)

    fun getOrCreateKey(): ByteArray {
        val wrapped = prefs.getString(PREF_WRAPPED, null)
        val iv = prefs.getString(PREF_IV, null)
        if (wrapped != null || iv != null) {
            check(wrapped != null && iv != null) { "보관 자료의 암호화 키가 불완전합니다. 기존 자료를 덮어쓰지 않습니다." }
            return try { unwrap(Base64.decode(wrapped, Base64.NO_WRAP), Base64.decode(iv, Base64.NO_WRAP)) }
            catch (error: Exception) { throw IllegalStateException("보관 자료의 암호화 키를 열지 못했습니다. 기존 키와 자료를 유지합니다.", error) }
        }
        check(!context.getDatabasePath(AppDatabase.NAME).let { it.exists() && it.length() > 0L }) {
            "기존 자료가 있지만 암호화 키를 찾을 수 없습니다. 복구 확인 없이 새 키를 만들지 않습니다."
        }
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val (ct, newIv) = wrap(raw)
        prefs.edit()
            .putString(PREF_WRAPPED, Base64.encodeToString(ct, Base64.NO_WRAP))
            .putString(PREF_IV, Base64.encodeToString(newIv, Base64.NO_WRAP))
            .apply()
        return raw
    }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun wrap(raw: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        return cipher.doFinal(raw) to cipher.iv
    }

    private fun unwrap(ct: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ct)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "malitda_db_wrap_key"
        private const val PREF_WRAPPED = "db_key_wrapped"
        private const val PREF_IV = "db_key_iv"
    }
}
