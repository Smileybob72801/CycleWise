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
 */
class PassphraseServiceAndroid(
    private val saltStorage: SaltStorage
) : PassphraseService {
    private val keyLength = 32

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