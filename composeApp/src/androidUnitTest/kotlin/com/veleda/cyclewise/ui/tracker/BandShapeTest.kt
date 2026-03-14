package com.veleda.cyclewise.ui.tracker

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * Unit tests for the [bandShape] helper function.
 *
 * Verifies the four combinations of start/end flags produce the expected
 * [RoundedCornerShape] configurations.
 */
@RunWith(RobolectricTestRunner::class)
class BandShapeTest {

    private val radius = 8.dp

    @Test
    fun `GIVEN start and end THEN fully rounded`() {
        // GIVEN — a single-cell band (both start and end)
        // WHEN
        val shape = bandShape(isStart = true, isEnd = true, radius = radius)
        // THEN — all four corners are rounded
        assertEquals(RoundedCornerShape(radius), shape)
    }

    @Test
    fun `GIVEN start only THEN left-rounded`() {
        // GIVEN — first cell of a multi-cell band
        // WHEN
        val shape = bandShape(isStart = true, isEnd = false, radius = radius)
        // THEN — only left (start) corners are rounded
        assertEquals(
            RoundedCornerShape(topStart = radius, bottomStart = radius),
            shape
        )
    }

    @Test
    fun `GIVEN end only THEN right-rounded`() {
        // GIVEN — last cell of a multi-cell band
        // WHEN
        val shape = bandShape(isStart = false, isEnd = true, radius = radius)
        // THEN — only right (end) corners are rounded
        assertEquals(
            RoundedCornerShape(topEnd = radius, bottomEnd = radius),
            shape
        )
    }

    @Test
    fun `GIVEN neither start nor end THEN flat`() {
        // GIVEN — middle cell of a band
        // WHEN
        val shape = bandShape(isStart = false, isEnd = false, radius = radius)
        // THEN — no rounded corners
        assertEquals(RoundedCornerShape(0), shape)
    }
}
