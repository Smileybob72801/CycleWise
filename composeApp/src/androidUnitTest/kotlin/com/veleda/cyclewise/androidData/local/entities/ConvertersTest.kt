package com.veleda.cyclewise.androidData.local.entities

import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.LibidoLevel
import com.veleda.cyclewise.domain.models.SymptomCategory
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Unit tests for all Room TypeConverter functions.
 * These are simple, fast tests verifying the correctness of data serialization/deserialization.
 */
class ConvertersTest {

    // --- Tests for LocalDate Converters ---

    @Test
    fun fromLocalDate_convertsDateToCorrectIsoString() {
        val date = LocalDate(2025, 12, 31)
        val expectedString = "2025-12-31"
        assertEquals(expectedString, Converters.fromLocalDate(date))
    }

    @Test
    fun toLocalDate_convertsIsoStringToCorrectDate() {
        val dateString = "2025-08-19"
        val expectedDate = LocalDate(2025, 8, 19)
        assertEquals(expectedDate, Converters.toLocalDate(dateString))
    }

    // --- Tests for Instant Converters ---

    @Test
    fun fromInstant_convertsInstantToCorrectEpochMillis() {
        val instant = Instant.fromEpochMilliseconds(1723996800000L) // Represents a specific moment
        val expectedMillis = 1723996800000L
        assertEquals(expectedMillis, Converters.fromInstant(instant))
    }

    @Test
    fun toInstant_convertsEpochMillisToCorrectInstant() {
        val millis = 1723996800000L
        val expectedInstant = Instant.fromEpochMilliseconds(1723996800000L)
        assertEquals(expectedInstant, Converters.toInstant(millis))
    }

    // --- Tests for FlowIntensity Converters ---

    @Test
    fun fromFlowIntensity_convertsEnumToString() {
        assertEquals("HEAVY", Converters.fromFlowIntensity(FlowIntensity.HEAVY))
        assertEquals("MEDIUM", Converters.fromFlowIntensity(FlowIntensity.MEDIUM))
        assertEquals("LIGHT", Converters.fromFlowIntensity(FlowIntensity.LIGHT))
    }

    @Test
    fun fromFlowIntensity_WHEN_null_THEN_returnsNull() {
        assertNull(Converters.fromFlowIntensity(null))
    }

    @Test
    fun toFlowIntensity_convertsStringToEnum() {
        assertEquals(FlowIntensity.HEAVY, Converters.toFlowIntensity("HEAVY"))
        assertEquals(FlowIntensity.MEDIUM, Converters.toFlowIntensity("MEDIUM"))
        assertEquals(FlowIntensity.LIGHT, Converters.toFlowIntensity("LIGHT"))
    }

    @Test
    fun toFlowIntensity_WHEN_null_THEN_returnsNull() {
        assertNull(Converters.toFlowIntensity(null))
    }

    @Test
    fun toFlowIntensity_WHEN_invalidString_THEN_throwsException() {
        // This is a critical edge case test. It ensures that if the database ever
        // contains corrupt data, the app will fail fast instead of misinterpreting it.
        assertFailsWith<IllegalArgumentException> {
            Converters.toFlowIntensity("INVALID_VALUE")
        }
    }

    // --- Tests for LibidoLevel Converters ---

    @Test
    fun fromLibidoLevel_convertsEnumToString() {
        assertEquals("HIGH", Converters.fromLibidoLevel(LibidoLevel.HIGH))
        assertEquals("MEDIUM", Converters.fromLibidoLevel(LibidoLevel.MEDIUM))
        assertEquals("LOW", Converters.fromLibidoLevel(LibidoLevel.LOW))
    }

    @Test
    fun toLibidoLevel_convertsStringToEnum() {
        assertEquals(LibidoLevel.HIGH, Converters.toLibidoLevel("HIGH"))
        assertEquals(LibidoLevel.MEDIUM, Converters.toLibidoLevel("MEDIUM"))
        assertEquals(LibidoLevel.LOW, Converters.toLibidoLevel("LOW"))
    }

    @Test
    fun toLibidoLevel_WHEN_null_THEN_returnsNull() {
        assertNull(Converters.toLibidoLevel(null))
    }

    // --- Tests for SymptomCategory Converters ---

    @Test
    fun fromSymptomCategory_convertsEnumToString() {
        assertEquals("PAIN", Converters.fromSymptomCategory(SymptomCategory.PAIN))
        assertEquals("OTHER", Converters.fromSymptomCategory(SymptomCategory.OTHER))
    }

    @Test
    fun toSymptomCategory_convertsStringToEnum() {
        assertEquals(SymptomCategory.PAIN, Converters.toSymptomCategory("PAIN"))
        assertEquals(SymptomCategory.OTHER, Converters.toSymptomCategory("OTHER"))
    }

    @Test
    fun toSymptomCategory_WHEN_null_THEN_returnsNull() {
        assertNull(Converters.toSymptomCategory(null))
    }
}