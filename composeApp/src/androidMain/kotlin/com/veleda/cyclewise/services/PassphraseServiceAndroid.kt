package com.veleda.cyclewise.services

import com.veleda.cyclewise.domain.services.PassphraseService
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.crypto.generators.Argon2BytesGenerator


class PassphraseServiceAndroid(
    private val saltStorage: SaltStorage) : PassphraseService {
    private val keyLength = 32 // 256-bit

    override fun deriveKey(passphrase: String): ByteArray {

        // 1) get-or-create a stable salt
        val salt = saltStorage.getOrCreateSalt()

        // 2) build Argon2 parameters
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withParallelism(1)
            .withMemoryAsKB(64 * 1024)   // 64 MB
            .withIterations(3)
            .build()

        // 3) derive
        val generator = Argon2BytesGenerator().apply { init(params) }
        val key = ByteArray(keyLength)
        generator.generateBytes(passphrase.toCharArray(), key, 0, keyLength)
        return key
    }
}