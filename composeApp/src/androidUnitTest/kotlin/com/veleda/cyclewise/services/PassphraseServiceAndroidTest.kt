package com.veleda.cyclewise.services

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import java.util.Arrays
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PassphraseServiceAndroidTest {

    // --- SETUP ---
    private lateinit var mockSaltStorage: SaltStorage
    private lateinit var service: PassphraseServiceAndroid

    // Define some constant, reusable test data
    private val testPassphrase = "super_secret_password_123"
    private val saltA = "salt_A_16_bytes".toByteArray() // 16 bytes
    private val saltB = "salt_B_16_bytes".toByteArray() // 16 bytes, different from A

    @Before
    fun setUp() {
        // Create a mock of our dependency before each test
        mockSaltStorage = mockk<SaltStorage>()
        // Create a real instance of the service, injecting the mock
        service = PassphraseServiceAndroid(mockSaltStorage)
    }

    // --- TEST CASES ---

    @Test
    fun deriveKey_WHEN_calledWithSamePassphraseAndSalt_THEN_producesSameKey() {
        // ARRANGE
        // Configure the mock to always return the same salt (saltA)
        every { mockSaltStorage.getOrCreateSalt() } returns saltA

        // ACT
        val key1 = service.deriveKey(testPassphrase)
        val key2 = service.deriveKey(testPassphrase)

        // ASSERT
        // We must use Arrays.equals() to compare the content of byte arrays,
        // not the standard assertEquals which would compare their memory references.
        assertTrue(
            key1.contentEquals(key2),
            "Keys derived from the same passphrase and salt should be identical"
        )
    }

    @Test
    fun deriveKey_WHEN_calledWithDifferentPassphrase_THEN_producesDifferentKey() {
        // ARRANGE
        every { mockSaltStorage.getOrCreateSalt() } returns saltA

        // ACT
        val key1 = service.deriveKey("password123")
        val key2 = service.deriveKey("password456")

        // ASSERT
        assertFalse(
            key1.contentEquals(key2),
            "Keys derived from different passphrases should be different"
        )
    }

    @Test
    fun deriveKey_WHEN_calledWithDifferentSalt_THEN_producesDifferentKey() {
        // ARRANGE
        // Configure the mock with a sequence of return values.
        // The first time it's called it will return saltA, the second time saltB.
        every { mockSaltStorage.getOrCreateSalt() } returns saltA andThen saltB

        // ACT
        val key1 = service.deriveKey(testPassphrase)
        val key2 = service.deriveKey(testPassphrase)

        // ASSERT
        assertFalse(
            key1.contentEquals(key2),
            "Keys derived from the same passphrase but different salts should be different"
        )
    }

    @Test
    fun deriveKey_THEN_returnsKeyOfCorrectLength() {
        // ARRANGE
        every { mockSaltStorage.getOrCreateSalt() } returns saltA
        val expectedKeyLength = 32 // 256 bits / 8 bits per byte

        // ACT
        val key = service.deriveKey(testPassphrase)

        // ASSERT
        assertEquals(
            expectedKeyLength,
            key.size,
            "The derived key should be 32 bytes (256 bits) long"
        )
    }
}