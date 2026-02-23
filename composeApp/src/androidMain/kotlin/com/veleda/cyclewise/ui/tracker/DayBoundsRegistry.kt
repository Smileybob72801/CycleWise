package com.veleda.cyclewise.ui.tracker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.datetime.LocalDate

/**
 * Maps calendar-day screen positions to [LocalDate] values.
 *
 * Each [CalendarDayCell] registers its root-coordinate [Rect] via [register]
 * when globally positioned, and removes itself via [unregister] on disposal.
 * The drag gesture handler then uses [dateAt] to convert pointer positions
 * into the [LocalDate] being hovered.
 */
class DayBoundsRegistry {
    private val bounds = mutableMapOf<LocalDate, Rect>()

    /**
     * Records the screen-space bounding [rect] for [date].
     *
     * @param date The calendar date this cell represents.
     * @param rect The cell's bounds in root coordinates (via `boundsInRoot()`).
     */
    fun register(date: LocalDate, rect: Rect) {
        bounds[date] = rect
    }

    /**
     * Removes the recorded bounds for [date].
     *
     * Called when the composable leaves composition so stale entries
     * do not produce false look-up hits.
     *
     * @param date The calendar date to unregister.
     */
    fun unregister(date: LocalDate) {
        bounds.remove(date)
    }

    /**
     * Returns the [LocalDate] whose registered bounds contain [position], or null
     * if no registered cell contains that point.
     *
     * @param position A pointer position in root coordinates.
     * @return The date at that position, or null if none match.
     */
    fun dateAt(position: Offset): LocalDate? =
        bounds.entries.firstOrNull { (_, rect) -> rect.contains(position) }?.key
}
