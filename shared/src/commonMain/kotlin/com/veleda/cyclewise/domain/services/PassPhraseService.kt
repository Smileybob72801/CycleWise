package com.veleda.cyclewise.domain.services

/**
 * Derives a 256-bit AES-GCM encryption key from the user's passphrase using a KDF.
 *
 * The Android implementation uses Argon2id with a stable per-device salt.
 * The returned [ByteArray] is passed to SQLCipher's [SupportFactory] to open
 * the encrypted database.
 *
 * **Security contract:**
 * - The derived key must never be persisted to disk.
 * - Callers must zeroize the returned [ByteArray] when the session scope closes.
 * - The KDF must be computationally expensive to resist brute-force attacks.
 */
interface PassphraseService {
    /**
     * Derives a 32-byte (256-bit) encryption key from [passphrase].
     *
     * @param passphrase the user-entered secret.
     * @return a 32-byte key suitable for SQLCipher AES-256 encryption.
     */
    fun deriveKey(passphrase: String): ByteArray
}