package com.veleda.cyclewise.ui.tracker

import androidx.compose.ui.graphics.Color
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for the hex parsing and formatting utilities in `CyclePhaseColors.kt`.
 */
class CyclePhaseColorsTest {

    // --- parseHexColor ---

    @Test
    fun parseHexColor_WHEN_valid6CharHex_THEN_returnsColor() {
        // GIVEN a valid 6-char uppercase hex
        val hex = "EF9A9A"

        // WHEN parsing
        val result = parseHexColor(hex)

        // THEN a non-null Color is returned with the expected ARGB value
        assertNotNull(result)
        assertEquals(Color(0xFFEF9A9A), result)
    }

    @Test
    fun parseHexColor_WHEN_invalidChars_THEN_returnsNull() {
        // GIVEN a string with non-hex characters
        val hex = "ZZZZZZ"

        // WHEN parsing
        val result = parseHexColor(hex)

        // THEN null is returned
        assertNull(result)
    }

    @Test
    fun parseHexColor_WHEN_wrongLength_THEN_returnsNull() {
        // GIVEN a hex string that is too short
        val hex = "EF9A"

        // WHEN parsing
        val result = parseHexColor(hex)

        // THEN null is returned
        assertNull(result)
    }

    @Test
    fun parseHexColor_WHEN_emptyString_THEN_returnsNull() {
        // GIVEN an empty string
        val hex = ""

        // WHEN parsing
        val result = parseHexColor(hex)

        // THEN null is returned
        assertNull(result)
    }

    @Test
    fun parseHexColor_WHEN_lowercaseHex_THEN_returnsColor() {
        // GIVEN a valid lowercase 6-char hex
        val hex = "80cbc4"

        // WHEN parsing
        val result = parseHexColor(hex)

        // THEN a non-null Color is returned
        assertNotNull(result)
        assertEquals(Color(0xFF80CBC4), result)
    }

    // --- hexFromColor ---

    @Test
    fun hexFromColor_WHEN_knownColor_THEN_returnsExpectedHex() {
        // GIVEN the default Menstruation color
        val color = CyclePhaseColors.Menstruation

        // WHEN converting to hex
        val hex = hexFromColor(color)

        // THEN the expected uppercase 6-char hex is returned
        assertEquals("EF9A9A", hex)
    }
}
