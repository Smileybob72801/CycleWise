package com.veleda.cyclewise.ui.tracker

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.ui.theme.CyclePhasePalette
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.datetime.toKotlinLocalDate

/**
 * A single calendar-day cell rendered inside the [HorizontalCalendar] grid.
 *
 * Displays the day number, optional period/phase background colouring,
 * dot indicators for logged symptoms, medications, and notes, a today
 * border ring, and a bounce-scale animation on press.
 *
 * Long-press is handled at the calendar container level (via [DayBoundsRegistry]
 * coordinate look-up), so this cell only handles single-tap via [onTap].
 *
 * @param day               The [CalendarDay] from the calendar library.
 * @param dayInfo           Aggregated day status (period, symptoms, medications, notes, phase).
 * @param isToday           True if this cell represents today's date — draws a primary border ring.
 * @param isStartDate       True if the date is the start of a period range.
 * @param isEndDate         True if the date is the end of a period range.
 * @param isInExistingRange True if this date falls inside any saved period.
 * @param isInSelectionRange True if this date is part of an in-progress drag selection.
 * @param isPhaseStart      True if this date is the first day of its displayed phase band.
 * @param isPhaseEnd        True if this date is the last day of its displayed phase band.
 * @param palette           The current [CyclePhasePalette] providing per-phase colours.
 * @param displayPhase      The cycle phase to render (null suppresses phase colouring).
 * @param isDragging        True while a period-range drag gesture is in progress. Disables the
 *                          [clickable] modifier so that releasing a drag does not fire [onTap].
 * @param onTap             Callback for a single tap, or null if the cell is not tappable.
 * @param boundsRegistry    Optional [DayBoundsRegistry] — when provided, this cell registers
 *                          its root-coordinate bounds so the container-level drag gesture
 *                          can map pointer positions to dates.
 */
@Composable
internal fun CalendarDayCell(
    day: CalendarDay,
    dayInfo: CalendarDayInfo?,
    isToday: Boolean,
    isStartDate: Boolean,
    isEndDate: Boolean,
    isInExistingRange: Boolean,
    isInSelectionRange: Boolean,
    isDragging: Boolean = false,
    isPhaseStart: Boolean = true,
    isPhaseEnd: Boolean = true,
    palette: CyclePhasePalette,
    displayPhase: CyclePhase? = null,
    onTap: (() -> Unit)?,
    boundsRegistry: DayBoundsRegistry? = null
) {
    val dims = LocalDimensions.current
    val date = day.date.toKotlinLocalDate()
    val inRange = isInExistingRange || isInSelectionRange
    val hasDisplayPhase = displayPhase != null

    val periodShape = when {
        isStartDate && isEndDate -> CircleShape
        isStartDate -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
        isEndDate -> RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
        inRange -> RoundedCornerShape(0)
        else -> CircleShape
    }

    val phaseShape = when {
        isPhaseStart && isPhaseEnd -> RoundedCornerShape(50)
        isPhaseStart -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
        isPhaseEnd -> RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
        else -> RoundedCornerShape(0)
    }

    val bgShape = when {
        dayInfo?.isPeriodDay == true -> periodShape
        isInSelectionRange -> periodShape
        hasDisplayPhase -> phaseShape
        else -> CircleShape
    }

    val isPhaseMiddle = hasDisplayPhase && !isPhaseStart && !isPhaseEnd

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dayPressScale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(
                vertical = when {
                    inRange && !isStartDate && !isEndDate -> 0.dp
                    isPhaseMiddle -> 0.dp
                    else -> dims.xs
                }
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag("day-$date")
            .border(
                width = if (isToday) dims.xxs else 0.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .background(
                color = when {
                    dayInfo?.isPeriodDay == true ->
                        palette.menstruation.fill

                    isInSelectionRange ->
                        palette.menstruation.fill.copy(alpha = 0.4f)

                    hasDisplayPhase ->
                        palette.forPhase(displayPhase!!).fillSubtle

                    else -> Color.Transparent
                },
                shape = bgShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = day.position == DayPosition.MonthDate && !isDragging,
                onClick = { onTap?.invoke() }
            )
            .onGloballyPositioned { coords ->
                boundsRegistry?.register(date, coords.boundsInRoot())
            },
        contentAlignment = Alignment.Center
    ) {
        DisposableEffect(date) {
            onDispose { boundsRegistry?.unregister(date) }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (day.position == DayPosition.MonthDate)
                    MaterialTheme.colorScheme.onSurface
                else
                    Color.Gray
            )

            Row(
                modifier = Modifier.padding(top = dims.xxs),
                horizontalArrangement = Arrangement.spacedBy(dims.xxs)
            ) {
                if (dayInfo?.hasSymptoms == true) {
                    Box(
                        modifier = Modifier
                            .size(dims.xs)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                            .testTag("symptom-dot-$date")
                    )
                }
                if (dayInfo?.hasMedications == true) {
                    Box(
                        modifier = Modifier
                            .size(dims.xs)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                            .testTag("medication-dot-$date")
                    )
                }
                if (dayInfo?.hasNotes == true) {
                    Box(
                        modifier = Modifier
                            .size(dims.xs)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline)
                            .testTag("notes-dot-$date")
                    )
                }
            }
            if (dayInfo?.isPeriodDay == true) {
                Box(modifier = Modifier.testTag("period-day-$date"))
            }
        }
    }
}
