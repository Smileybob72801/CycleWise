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

class ConvertersTest {

    // --- Tests for LocalDate Converters ---

    @Test
    fun fromLocalDate_WHEN_validDate_THEN_returnsIsoString() {
        // ACT
        val result = Converters.fromLocalDate(LocalDate(2025, 12, 31))

        // ASSERT
        assertEquals("2025-12-31", result)
    }

    @Test
    fun toLocalDate_WHEN_validString_THEN_returnsCorrectDate() {
        // ACT
        val result = Converters.toLocalDate("2025-08-19")

        // ASSERT
        assertEquals(LocalDate(2025, 8, 19), result)
    }

    // --- Tests for Instant Converters ---

    @Test
    fun fromInstant_WHEN_validInstant_THEN_returnsEpochMillis() {
        // ACT
        val result = Converters.fromInstant(Instant.fromEpochMilliseconds(1723996800000L))

        // ASSERT
        assertEquals(1723996800000L, result)
    }

    @Test
    fun toInstant_WHEN_validMillis_THEN_returnsCorrectInstant() {
        // ACT
        val result = Converters.toInstant(1723996800000L)

        // ASSERT
        assertEquals(Instant.fromEpochMilliseconds(1723996800000L), result)
    }

    // --- Tests for FlowIntensity Converters ---

    @Test
    fun fromFlowIntensity_WHEN_validEnum_THEN_returnsString() {
        // ACT & ASSERT
        assertEquals("HEAVY", Converters.fromFlowIntensity(FlowIntensity.HEAVY))
        assertEquals("MEDIUM", Converters.fromFlowIntensity(FlowIntensity.MEDIUM))
        assertEquals("LIGHT", Converters.fromFlowIntensity(FlowIntensity.LIGHT))
    }

    @Test
    fun fromFlowIntensity_WHEN_null_THEN_returnsNull() {
        // ACT & ASSERT
        assertNull(Converters.fromFlowIntensity(null))
    }

    @Test
    fun toFlowIntensity_WHEN_validString_THEN_returnsEnum() {
        // ACT & ASSERT
        assertEquals(FlowIntensity.HEAVY, Converters.toFlowIntensity("HEAVY"))
        assertEquals(FlowIntensity.MEDIUM, Converters.toFlowIntensity("MEDIUM"))
        assertEquals(FlowIntensity.LIGHT, Converters.toFlowIntensity("LIGHT"))
    }

    @Test
    fun toFlowIntensity_WHEN_null_THEN_returnsNull() {
        // ACT & ASSERT
        assertNull(Converters.toFlowIntensity(null))
    }

    @Test
    fun toFlowIntensity_WHEN_invalidString_THEN_throwsException() {
        // ACT & ASSERT
        assertFailsWith<IllegalArgumentException> {
            Converters.toFlowIntensity("INVALID_VALUE")
        }
    }

    // --- Tests for LibidoLevel Converters ---

    @Test
    fun fromLibidoLevel_WHEN_validEnum_THEN_returnsString() {
        // ACT & ASSERT
        assertEquals("HIGH", Converters.fromLibidoLevel(LibidoLevel.HIGH))
        assertEquals("MEDIUM", Converters.fromLibidoLevel(LibidoLevel.MEDIUM))
        assertEquals("LOW", Converters.fromLibidoLevel(LibidoLevel.LOW))
    }

    @Test
    fun toLibidoLevel_WHEN_validString_THEN_returnsEnum() {
        // ACT & ASSERT
        assertEquals(LibidoLevel.HIGH, Converters.toLibidoLevel("HIGH"))
        assertEquals(LibidoLevel.MEDIUM, Converters.toLibidoLevel("MEDIUM"))
        assertEquals(LibidoLevel.LOW, Converters.toLibidoLevel("LOW"))
    }

    @Test
    fun toLibidoLevel_WHEN_null_THEN_returnsNull() {
        // ACT & ASSERT
        assertNull(Converters.toLibidoLevel(null))
    }

    @Test
    fun toLibidoLevel_WHEN_invalidString_THEN_throwsException() {
        // ACT & ASSERT
        assertFailsWith<IllegalArgumentException> {
            Converters.toLibidoLevel("INVALID_VALUE")
        }
    }

    // --- Tests for SymptomCategory Converters ---

    @Test
    fun fromSymptomCategory_WHEN_validEnum_THEN_returnsString() {
        // ACT & ASSERT
        assertEquals("PAIN", Converters.fromSymptomCategory(SymptomCategory.PAIN))
        assertEquals("OTHER", Converters.fromSymptomCategory(SymptomCategory.OTHER))
    }

    @Test
    fun toSymptomCategory_WHEN_validString_THEN_returnsEnum() {
        // ACT & ASSERT
        assertEquals(SymptomCategory.PAIN, Converters.toSymptomCategory("PAIN"))
        assertEquals(SymptomCategory.OTHER, Converters.toSymptomCategory("OTHER"))
    }

    @Test
    fun toSymptomCategory_WHEN_null_THEN_returnsNull() {
        // ACT & ASSERT
        assertNull(Converters.toSymptomCategory(null))
    }

    @Test
    fun toSymptomCategory_WHEN_invalidString_THEN_throwsException() {
        // ACT & ASSERT
        assertFailsWith<IllegalArgumentException> {
            Converters.toSymptomCategory("INVALID_VALUE")
        }
    }
}
