package kr.voicemate.malitda.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Local device gate, not an account or proof of a guardian's legal authority. */
object GuardianPin {
    private const val ITERATIONS = 600_000
    private const val PREFIX = "pbkdf2-sha256-v1"
    fun valid(pin: String): Boolean = pin.length == 6 && pin.all { it in '0'..'9' }

    fun encode(pin: String): String {
        require(valid(pin)) { "숫자 6자리를 입력합니다." }
        val salt = ByteArray(32).also(SecureRandom()::nextBytes)
        val hash = derive(pin, salt, ITERATIONS)
        val b64 = Base64.getEncoder()
        return "$PREFIX:$ITERATIONS:${b64.encodeToString(salt)}:${b64.encodeToString(hash)}"
    }

    fun matches(pin: String, encoded: String): Boolean {
        if (!valid(pin)) return false
        return runCatching {
            val fields = encoded.split(':')
            if (fields.size != 4 || fields[0] != PREFIX) return false
            val iterations = fields[1].toInt()
            if (iterations !in 100_000..1_000_000) return false
            val salt = Base64.getDecoder().decode(fields[2])
            val hash = Base64.getDecoder().decode(fields[3])
            if (salt.size != 32 || hash.size != 32) return false
            MessageDigest.isEqual(hash, derive(pin, salt, iterations))
        }.getOrDefault(false)
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }

    fun cooldownMillis(failedAttempts: Int): Long {
        if (failedAttempts <= 0 || failedAttempts % 5 != 0) return 0L
        return (30_000L * (1L shl ((failedAttempts / 5 - 1).coerceIn(0, 4)))).coerceAtMost(300_000L)
    }
}

sealed interface GuardianPinResult {
    data object Unlocked : GuardianPinResult
    data object NotConfigured : GuardianPinResult
    data class Incorrect(val attemptsUntilDelay: Int) : GuardianPinResult
    data class Locked(val remainingSeconds: Long) : GuardianPinResult
}
