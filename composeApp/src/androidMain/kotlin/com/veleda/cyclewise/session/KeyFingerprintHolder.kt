package com.veleda.cyclewise.session

import java.security.MessageDigest

/**
 * Holds an in-memory SHA-256 fingerprint of the session's encryption key.
 *
 * Used to verify the current passphrase during the "Change Passphrase" flow without opening
 * a second raw SQLCipher connection (which would conflict with the Room-held database file).
 *
 * **Security properties:**
 * - Only a SHA-256 hash is stored, never the raw key — the hash cannot be reversed to recover
 *   the AES-256 encryption key.
 * - [matches] uses [MessageDigest.isEqual] for constant-time comparison, preventing timing attacks.
 * - [clear] zeroizes the stored fingerprint when the session scope is destroyed.
 *
 * This class must be registered as a **session-scoped** dependency so it is created on unlock
 * and destroyed on logout/autolock together with the [PeriodDatabase].
 */
class KeyFingerprintHolder {

    private var fingerprint: ByteArray? = null

    /**
     * Computes and stores the SHA-256 fingerprint of the given [derivedKey].
     *
     * Call this immediately after key derivation, before the key array is zeroized.
     *
     * @param derivedKey the 32-byte AES key derived from the user's passphrase.
     */
    fun store(derivedKey: ByteArray) {
        fingerprint = sha256(derivedKey)
    }

    /**
     * Returns `true` if the SHA-256 fingerprint of [candidateKey] matches the stored fingerprint.
     *
     * Uses [MessageDigest.isEqual] for constant-time comparison.
     *
     * @param candidateKey the key derived from the passphrase the user claims is "current".
     * @return `true` if the candidate matches, `false` if it doesn't or no fingerprint is stored.
     */
    fun matches(candidateKey: ByteArray): Boolean {
        val stored = fingerprint ?: return false
        return MessageDigest.isEqual(stored, sha256(candidateKey))
    }

    /**
     * Zeroizes the stored fingerprint.
     *
     * Called when the session scope is destroyed (logout / autolock).
     */
    fun clear() {
        fingerprint?.fill(0)
        fingerprint = null
    }

    private fun sha256(input: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input)
}
