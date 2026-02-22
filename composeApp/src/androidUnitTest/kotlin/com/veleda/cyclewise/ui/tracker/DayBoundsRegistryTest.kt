package com.veleda.cyclewise.ui.tracker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DayBoundsRegistryTest {

    private val registry = DayBoundsRegistry()

    @Test
    fun dateAt_WHEN_positionInsideBounds_THEN_returnsDate() {
        // GIVEN — a date registered with a known rect
        val date = LocalDate(2025, 6, 10)
        val rect = Rect(0f, 0f, 100f, 100f)
        registry.register(date, rect)

        // WHEN — querying a point inside the rect
        val result = registry.dateAt(Offset(50f, 50f))

        // THEN — the registered date is returned
        assertEquals(date, result)
    }

    @Test
    fun dateAt_WHEN_positionOutsideBounds_THEN_returnsNull() {
        // GIVEN — a date registered with a known rect
        val date = LocalDate(2025, 6, 10)
        val rect = Rect(0f, 0f, 100f, 100f)
        registry.register(date, rect)

        // WHEN — querying a point outside the rect
        val result = registry.dateAt(Offset(150f, 150f))

        // THEN — null is returned
        assertNull(result)
    }

    @Test
    fun unregister_THEN_dateNoLongerFound() {
        // GIVEN — a date registered and then unregistered
        val date = LocalDate(2025, 6, 10)
        val rect = Rect(0f, 0f, 100f, 100f)
        registry.register(date, rect)
        registry.unregister(date)

        // WHEN — querying a point that was inside the rect
        val result = registry.dateAt(Offset(50f, 50f))

        // THEN — null is returned
        assertNull(result)
    }

    @Test
    fun dateAt_WHEN_multipleDatesRegistered_THEN_returnsCorrectDate() {
        // GIVEN — multiple non-overlapping dates registered
        val date1 = LocalDate(2025, 6, 10)
        val date2 = LocalDate(2025, 6, 11)
        val date3 = LocalDate(2025, 6, 12)
        registry.register(date1, Rect(0f, 0f, 100f, 100f))
        registry.register(date2, Rect(110f, 0f, 210f, 100f))
        registry.register(date3, Rect(220f, 0f, 320f, 100f))

        // WHEN — querying a point inside each rect
        val result1 = registry.dateAt(Offset(50f, 50f))
        val result2 = registry.dateAt(Offset(160f, 50f))
        val result3 = registry.dateAt(Offset(270f, 50f))

        // THEN — the correct date is returned for each
        assertEquals(date1, result1)
        assertEquals(date2, result2)
        assertEquals(date3, result3)
    }
}
