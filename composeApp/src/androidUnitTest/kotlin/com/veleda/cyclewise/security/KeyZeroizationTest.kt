package com.veleda.cyclewise.security

import android.content.Context
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.di.createDatabaseAndZeroizeKey
import com.veleda.cyclewise.domain.services.PassphraseService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Verifies that [createDatabaseAndZeroizeKey] zeroizes the derived key after database creation.
 *
 * The passphrase-derived key must never linger in memory after the database is opened.
 * These tests confirm the `try/finally` zeroization contract on both the success and
 * failure paths.
 */
class KeyZeroizationTest {

    private val context: Context = mockk(relaxed = true)
    private val passphraseService: PassphraseService = mockk()
    private val mockDb: PeriodDatabase = mockk(relaxed = true)

    private lateinit var capturedKey: ByteArray

    @Before
    fun setUp() {
        capturedKey = ByteArray(32) { 0xFF.toByte() }
        every { passphraseService.deriveKey(any()) } returns capturedKey
        mockkObject(PeriodDatabase.Companion)
    }

    @After
    fun tearDown() {
        unmockkObject(PeriodDatabase.Companion)
    }

    @Test
    fun createDatabaseAndZeroizeKey_WHEN_createSucceeds_THEN_keyIsZeroized() {
        // GIVEN create() returns successfully
        every { PeriodDatabase.create(any(), any(), any()) } returns mockDb

        // WHEN we create the database
        val db = createDatabaseAndZeroizeKey(context, passphraseService, "test-passphrase")

        // THEN the database is returned
        assertEquals(mockDb, db)

        // AND every byte of the key is zeroed
        assertTrue(
            capturedKey.all { it == 0.toByte() },
            "Key must be zeroized after successful database creation"
        )
    }

    @Test
    fun createDatabaseAndZeroizeKey_WHEN_createThrows_THEN_keyIsStillZeroized() {
        // GIVEN create() throws an exception
        every { PeriodDatabase.create(any(), any(), any()) } throws RuntimeException("DB creation failed")

        // WHEN we attempt to create the database
        assertFailsWith<RuntimeException>("DB creation failed") {
            createDatabaseAndZeroizeKey(context, passphraseService, "test-passphrase")
        }

        // THEN the key is still zeroized despite the failure
        assertTrue(
            capturedKey.all { it == 0.toByte() },
            "Key must be zeroized even when database creation fails"
        )
    }
}
