package com.veleda.cyclewise.security

import android.content.Context
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.di.createDatabaseAndZeroizeKey
import com.veleda.cyclewise.domain.services.PassphraseService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies that [createDatabaseAndZeroizeKey] passes a **copy** of the derived key to
 * [PeriodDatabase.create] and zeroizes the original immediately afterward.
 *
 * The passphrase-derived key must never linger in memory after the database is built.
 * These tests confirm:
 * - The key passed to [PeriodDatabase.create] is a distinct copy containing the real
 *   (non-zero) bytes — so SQLCipher's [SupportFactory] can later consume the real key
 *   when [db.openHelper.writableDatabase] is called by the ViewModel.
 * - The original key is zeroized after the function returns, on both success and failure paths.
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
    fun createDatabaseAndZeroizeKey_WHEN_createSucceeds_THEN_copyIsNonZeroAndOriginalIsZeroized() {
        // GIVEN create() captures the key it receives and returns successfully
        val receivedKey = slot<ByteArray>()
        every { PeriodDatabase.create(any(), capture(receivedKey), any()) } returns mockDb

        // WHEN we create the database
        val db = createDatabaseAndZeroizeKey(context, passphraseService, "test-passphrase")

        // THEN the database is returned
        assertEquals(mockDb, db)

        // AND the key passed to create() is a distinct copy (not the same array object)
        assertFalse(
            receivedKey.captured === capturedKey,
            "create() must receive a copy, not the original key array"
        )

        // AND the copy contained the real (non-zero) key bytes
        assertTrue(
            receivedKey.captured.any { it != 0.toByte() },
            "The copy passed to create() must contain the real key bytes"
        )

        // AND the original key is zeroed after the function returns
        assertTrue(
            capturedKey.all { it == 0.toByte() },
            "Original key must be zeroized after successful database creation"
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

        // THEN the original key is still zeroized despite the failure
        assertTrue(
            capturedKey.all { it == 0.toByte() },
            "Key must be zeroized even when database creation fails"
        )
    }
}
