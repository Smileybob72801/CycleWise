package com.veleda.cyclewise.session

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [KeyFingerprintHolder] following the Given-When-Then convention.
 *
 * Verifies SHA-256 fingerprinting, constant-time comparison, and zeroization behavior.
 */
class KeyFingerprintHolderTest {

    // ── store + matches ─────────────────────────────────────────────

    @Test
    fun `matches WHEN sameKey THEN returnsTrue`() {
        // GIVEN — a holder with a stored fingerprint
        val holder = KeyFingerprintHolder()
        val key = "test-passphrase-key-32-bytes!!!!".toByteArray()
        holder.store(key)

        // WHEN — matching with the same key content
        val candidate = "test-passphrase-key-32-bytes!!!!".toByteArray()

        // THEN — matches returns true
        assertTrue(holder.matches(candidate))
    }

    @Test
    fun `matches WHEN differentKey THEN returnsFalse`() {
        // GIVEN — a holder with a stored fingerprint
        val holder = KeyFingerprintHolder()
        val key = "correct-key-for-database-encrypt".toByteArray()
        holder.store(key)

        // WHEN — matching with a different key
        val candidate = "wrong-key-this-should-not-match!".toByteArray()

        // THEN — matches returns false
        assertFalse(holder.matches(candidate))
    }

    @Test
    fun `matches WHEN noFingerprintStored THEN returnsFalse`() {
        // GIVEN — a holder with no stored fingerprint
        val holder = KeyFingerprintHolder()

        // WHEN — matching with any key
        val candidate = "some-key-value-for-testing-12345".toByteArray()

        // THEN — matches returns false
        assertFalse(holder.matches(candidate))
    }

    // ── clear ────────────────────────────────────────────────────────

    @Test
    fun `matches WHEN clearedAfterStore THEN returnsFalse`() {
        // GIVEN — a holder with a stored fingerprint that has been cleared
        val holder = KeyFingerprintHolder()
        val key = "test-passphrase-key-32-bytes!!!!".toByteArray()
        holder.store(key)
        holder.clear()

        // WHEN — matching with the original key
        val candidate = "test-passphrase-key-32-bytes!!!!".toByteArray()

        // THEN — matches returns false (fingerprint was zeroized)
        assertFalse(holder.matches(candidate))
    }

    // ── store overwrites previous fingerprint ────────────────────────

    @Test
    fun `matches WHEN storedTwice THEN matchesOnlyLatestKey`() {
        // GIVEN — a holder where a second key replaces the first
        val holder = KeyFingerprintHolder()
        val oldKey = "old-key-for-initial-passphrase!!".toByteArray()
        val newKey = "new-key-after-passphrase-change!".toByteArray()
        holder.store(oldKey)
        holder.store(newKey)

        // WHEN — matching with the new key
        // THEN — matches the new key
        assertTrue(holder.matches("new-key-after-passphrase-change!".toByteArray()))

        // WHEN — matching with the old key
        // THEN — does not match the old key
        assertFalse(holder.matches("old-key-for-initial-passphrase!!".toByteArray()))
    }

    // ── clear on fresh holder is a no-op ─────────────────────────────

    @Test
    fun `clear WHEN noFingerprintStored THEN doesNotThrow`() {
        // GIVEN — a fresh holder with no stored fingerprint
        val holder = KeyFingerprintHolder()

        // WHEN — clear is called
        holder.clear()

        // THEN — no exception is thrown and matches still returns false
        assertFalse(holder.matches("any-key-should-not-match-here!!".toByteArray()))
    }
}
