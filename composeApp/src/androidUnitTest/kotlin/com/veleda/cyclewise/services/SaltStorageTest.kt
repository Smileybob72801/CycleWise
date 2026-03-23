package com.veleda.cyclewise.services

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

/**
 * Unit tests for [SaltStorage], specifically the [SaltStorage.setSalt] method
 * added for the backup import flow.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class SaltStorageTest {

    private lateinit var saltStorage: SaltStorage

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        saltStorage = SaltStorage(context)
        saltStorage.clear()
    }

    @Test
    fun `setSalt stores the salt and getOrCreateSalt returns it`() {
        // Given a known salt value
        val importedSalt = ByteArray(16) { it.toByte() }

        // When we set it
        saltStorage.setSalt(importedSalt)

        // Then getOrCreateSalt returns the same bytes
        val retrieved = saltStorage.getOrCreateSalt()
        assertContentEquals(importedSalt, retrieved)
    }

    @Test
    fun `setSalt overwrites a previously generated salt`() {
        // Given an auto-generated salt exists
        val originalSalt = saltStorage.getOrCreateSalt()

        // When we overwrite it with an imported salt
        val importedSalt = ByteArray(16) { (0xFF - it).toByte() }
        saltStorage.setSalt(importedSalt)

        // Then the retrieved salt is the imported one, not the original
        val retrieved = saltStorage.getOrCreateSalt()
        assertContentEquals(importedSalt, retrieved)
    }

    @Test
    fun `setSalt rejects salt that is too short`() {
        val tooShort = ByteArray(8)
        assertFailsWith<IllegalArgumentException> {
            saltStorage.setSalt(tooShort)
        }
    }

    @Test
    fun `setSalt rejects salt that is too long`() {
        val tooLong = ByteArray(32)
        assertFailsWith<IllegalArgumentException> {
            saltStorage.setSalt(tooLong)
        }
    }

    @Test
    fun `getSaltBase64 returns null when no salt exists`() {
        // Given a cleared storage
        saltStorage.clear()

        // Then getSaltBase64 returns null
        kotlin.test.assertNull(saltStorage.getSaltBase64())
    }

    @Test
    fun `getSaltBase64 returns non-null after salt is created`() {
        // Given a salt has been generated
        saltStorage.getOrCreateSalt()

        // Then getSaltBase64 returns a non-null Base64 string
        kotlin.test.assertNotNull(saltStorage.getSaltBase64())
    }
}
