package com.veleda.cyclewise.domain.services

/**
 * Derives a 256-bit encryption key from the user’s passphrase.
 * The returned ByteArray will be used to construct the SQLCipher SupportFactory.
 */
interface PassphraseService {
    /**
     * @param passphrase the user-entered secret
     * @return a 32-byte (256-bit) key
     */
    fun deriveKey(passphrase: String): ByteArray
}