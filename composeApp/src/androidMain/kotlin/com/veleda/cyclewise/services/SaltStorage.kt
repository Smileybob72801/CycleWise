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

    /**
     * Returns the persisted 16-byte salt, generating and saving one on first call.
     *
     * Uses [SecureRandom] for generation and Base64 for SharedPreferences storage.
     * The salt is not secret — it only ensures that identical passphrases on different
     * devices produce different derived keys.
     */
    fun getOrCreateSalt(): ByteArray {
        prefs.getString(SALT_KEY, null)?.let { base64 ->
            return Base64.decode(base64, Base64.DEFAULT)
        }
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val encoded = Base64.encodeToString(salt, Base64.NO_WRAP)

        prefs.edit { putString(SALT_KEY, encoded) }
        return salt
    }

    /**
     * Replaces the persisted salt with [salt], which must be exactly [SALT_SIZE] bytes.
     *
     * Used during backup import to restore the salt that was active when the database
     * was exported. After calling this, subsequent [getOrCreateSalt] calls will return
     * the imported salt instead of generating a new one.
     *
     * @throws IllegalArgumentException if [salt] is not exactly [SALT_SIZE] bytes.
     */
    fun setSalt(salt: ByteArray) {
        require(salt.size == SALT_SIZE) { "Salt must be $SALT_SIZE bytes, got ${salt.size}" }
        val encoded = Base64.encodeToString(salt, Base64.NO_WRAP)
        prefs.edit { putString(SALT_KEY, encoded) }
    }

    /**
     * Returns the Base64-encoded salt string, or `null` if no salt has been persisted.
     *
     * Used by [BackupManager] to include the raw salt in backup archives without
     * decoding and re-encoding.
     */
    fun getSaltBase64(): String? = prefs.getString(SALT_KEY, null)

    /** Removes the persisted salt, forcing a new one to be generated on the next unlock. */
    fun clear() {
        prefs.edit().clear().apply()
    }
}