package com.veleda.cyclewise.ui.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.ui.theme.CyclePhasePalette
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number

/**
 * Exploratory prototype: 12-month compact heat-map card showing period days at a glance.
 *
 * **Status:** NOT wired into navigation. This is an exploration spike to evaluate
 * whether the kizitonwose calendar library's API can support a year-view heat map,
 * or whether a custom grid is needed.
 *
 * **Findings:**
 * - The `HeatMapCalendar` composable from the library targets GitHub-style contribution
 *   graphs and is not a good fit for menstrual cycle data (it expects count-based data).
 * - A custom Compose grid (this implementation) is more flexible and gives full control
 *   over cell colors per cycle phase.
 * - Performance with 365 cells is acceptable since each cell is a simple `Box`.
 *
 * @param year     The year to display.
 * @param periods  All periods for color-coding.
 * @param dayDetails Per-date calendar annotations for phase coloring.
 * @param palette  Cycle phase colour palette.
 * @param modifier Modifier applied to the card.
 */
@Composable
internal fun YearAtAGlanceCard(
    year: Int,
    periods: List<Period>,
    dayDetails: Map<LocalDate, CalendarDayInfo>,
    palette: CyclePhasePalette,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(dims.md),
            verticalArrangement = Arrangement.spacedBy(dims.xs),
        ) {
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.titleMedium,
            )
            // 12 months in a 3x4 grid
            for (row in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dims.xs),
                ) {
                    for (col in 0 until 3) {
                        val monthIndex = row * 3 + col + 1
                        MiniMonth(
                            year = year,
                            month = Month(monthIndex),
                            periods = periods,
                            dayDetails = dayDetails,
                            palette = palette,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMonth(
    year: Int,
    month: Month,
    periods: List<Period>,
    dayDetails: Map<LocalDate, CalendarDayInfo>,
    palette: CyclePhasePalette,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current
    val daysInMonth = when (month) {
        Month.FEBRUARY -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        else -> 31
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = month.name.take(3),
            style = MaterialTheme.typography.labelSmall,
        )
        // Simple 7-column grid of day cells
        val firstDay = LocalDate(year, month.number, 1)
        val startDayOfWeek = firstDay.dayOfWeek.ordinal // Monday=0

        val cells = mutableListOf<LocalDate?>()
        repeat(startDayOfWeek) { cells.add(null) }
        for (day in 1..daysInMonth) {
            cells.add(LocalDate(year, month.number, day))
        }
        // Pad to full weeks
        while (cells.size % 7 != 0) cells.add(null)

        for (weekStart in cells.indices step 7) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dims.xxs),
            ) {
                for (i in 0 until 7) {
                    val date = cells.getOrNull(weekStart + i)
                    val info = date?.let { dayDetails[it] }
                    val isPeriod = info?.isPeriodDay == true
                    val phase = info?.cyclePhase

                    val cellColor = when {
                        isPeriod -> palette.forPhase(CyclePhase.MENSTRUATION).fill
                        phase != null -> palette.forPhase(phase).fillSubtle
                        date != null -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(dims.xxs))
                            .background(cellColor),
                    )
                }
            }
        }
    }
}
