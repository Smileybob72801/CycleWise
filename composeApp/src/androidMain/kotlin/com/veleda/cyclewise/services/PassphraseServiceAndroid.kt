package com.veleda.cyclewise.services

import com.veleda.cyclewise.domain.services.PassphraseService
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.crypto.generators.Argon2BytesGenerator


/**
 * Android implementation of [PassphraseService] using Argon2id KDF (via BouncyCastle).
 *
 * **KDF parameters:**
 * - Algorithm: Argon2id (resistant to both side-channel and GPU attacks)
 * - Parallelism: 1
 * - Memory: 64 MB (64 * 1024 KB)
 * - Iterations: 3
 * - Output key length: 32 bytes (256-bit AES)
 *
 * The salt is a stable 16-byte value stored in plaintext [SaltStorage] (not secret).
 *
 * @param saltStorage provides the per-device 16-byte salt for the KDF. The salt is
 *        stored in plaintext and is **not** a secret — its purpose is to ensure that
 *        the same passphrase on different devices produces different keys, preventing
 *        precomputed rainbow-table attacks.
 */
class PassphraseServiceAndroid(
    private val saltStorage: SaltStorage
) : PassphraseService {
    private val keyLength = 32

    /**
     * Derives a 32-byte AES-256 key from [passphrase] using Argon2id.
     *
     * This is a **blocking, CPU-intensive** operation. With the current parameters
     * (64 MB memory, 3 iterations) expect ~1-3 seconds of wall-clock time on a
     * typical Android device. Callers should invoke this on [Dispatchers.IO] or
     * another background dispatcher to avoid blocking the main thread.
     *
     * The returned [ByteArray] is **caller-owned**. The caller is responsible for
     * zeroizing it (e.g., `key.fill(0)`) once it has been consumed by SQLCipher's
     * `SupportFactory`. See [createDatabaseAndZeroizeKey] for the canonical usage
     * pattern.
     *
     * @param passphrase the raw user-entered secret.
     * @return a fresh 32-byte array containing the derived key.
     */
    override fun deriveKey(passphrase: String): ByteArray {
        val salt = saltStorage.getOrCreateSalt()

        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withParallelism(1)
            .withMemoryAsKB(64 * 1024)
            .withIterations(3)
            .build()

        val generator = Argon2BytesGenerator().apply { init(params) }
        val key = ByteArray(keyLength)
        generator.generateBytes(passphrase.toCharArray(), key, 0, keyLength)
        return key
    }
}