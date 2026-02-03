package com.amrdeveloper.linkhub.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object PasswordUtils {
    private const val SALT_LENGTH = 16
    private const val HASH_ALGORITHM = "SHA-256"

    fun hashPassword(password: String): String {
        val salt = generateSalt()
        val hash = hashWithSalt(password, salt)
        return "${Base64.encodeToString(salt, Base64.NO_WRAP)}:${Base64.encodeToString(hash, Base64.NO_WRAP)}"
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false

        return try {
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expectedHash = Base64.decode(parts[1], Base64.NO_WRAP)
            val actualHash = hashWithSalt(password, salt)
            expectedHash.contentEquals(actualHash)
        } catch (e: Exception) {
            false
        }
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun hashWithSalt(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        digest.update(salt)
        return digest.digest(password.toByteArray(Charsets.UTF_8))
    }
}
