package com.veleda.cyclewise.services

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import androidx.core.content.edit
import android.content.SharedPreferences

/**
 * Persists a 16-byte salt in plain SharedPreferences (MODE_PRIVATE).
 * Salt is not secret, so no need to encrypt it.
 */
class SaltStorage(context: Context) {

    companion object {
        private const val PREFS_NAME = "cyclewise_salt_prefs"
        private const val SALT_KEY   = "encryption_salt"
        private const val SALT_SIZE  = 16
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getOrCreateSalt(): ByteArray {
        // 1) Return existing if present
        prefs.getString(SALT_KEY, null)?.let { base64 ->
            return Base64.decode(base64, Base64.DEFAULT)
        }
        // 2) Otherwise generate, save, and return
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val encoded = Base64.encodeToString(salt, Base64.NO_WRAP)

        prefs.edit { putString(SALT_KEY, encoded) }
        return salt
    }
}