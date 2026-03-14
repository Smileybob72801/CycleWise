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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.ui.theme.CyclePhasePalette
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.datetime.toKotlinLocalDate

/** Alpha applied to the period-range fill when a day is in the selection/removal range. */
private const val PERIOD_FILL_ALPHA = 0.4f

/**
 * A single calendar-day cell rendered inside the [HorizontalCalendar] grid.
 *
 * Displays the day number, optional period/phase background colouring,
 * dot indicators for logged symptoms, medications, and notes, a today
 * border ring, an optional heatmap border, and a bounce-scale animation on press.
 *
 * When a heatmap metric is active, the cell draws a merged outline border via
 * [Modifier.drawBehind]. Adjacent heatmap days share a continuous border with no
 * inner dividers — middle cells extend the drawn rect beyond cell bounds and clip
 * it so only the top/bottom lines remain.
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
 * @param isInRemovalRange  True if this date is a current period day that will be removed by an
 *                          in-progress shrink drag. Renders with 40 % alpha menstruation fill
 *                          instead of the full period colour.
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
 * @param cellAspectRatio   Width-to-height ratio applied to the cell. Defaults to `1f` (square).
 *                          On landscape tablets the calendar container may not have enough vertical
 *                          space for square cells, so [CalendarGrid] computes a wider ratio that
 *                          keeps all six possible rows visible.
 * @param heatmapColor     Applied as a border around the cell via drawBehind. Adjacent
 *                          heatmap days share a continuous outline with no inner dividers.
 *                          Takes priority over the today border when active.
 * @param isHeatmapStart   True if this date is the first day of a contiguous heatmap data run.
 * @param isHeatmapEnd     True if this date is the last day of a contiguous heatmap data run.
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
    isInRemovalRange: Boolean = false,
    isDragging: Boolean = false,
    isPhaseStart: Boolean = true,
    isPhaseEnd: Boolean = true,
    palette: CyclePhasePalette,
    displayPhase: CyclePhase? = null,
    onTap: (() -> Unit)?,
    boundsRegistry: DayBoundsRegistry? = null,
    cellAspectRatio: Float = 1f,
    heatmapColor: Color? = null,
    isHeatmapStart: Boolean = true,
    isHeatmapEnd: Boolean = true,
) {
    val dims = LocalDimensions.current
    val date = day.date.toKotlinLocalDate()
    val inRange = isInExistingRange || isInSelectionRange
    val hasDisplayPhase = displayPhase != null

    val bandColor = when {
        dayInfo?.isPeriodDay == true && !isInRemovalRange -> palette.menstruation.fill
        isInSelectionRange || isInRemovalRange ->
            palette.menstruation.fill.copy(alpha = PERIOD_FILL_ALPHA)
        hasDisplayPhase -> palette.forPhase(displayPhase!!).fillSubtle
        else -> Color.Transparent
    }

    val bgShape = when {
        dayInfo?.isPeriodDay == true || isInSelectionRange ->
            bandShape(isStart = isStartDate, isEnd = isEndDate, radius = dims.sm)
        hasDisplayPhase ->
            bandShape(isStart = isPhaseStart, isEnd = isPhaseEnd, radius = dims.sm)
        else -> CircleShape
    }

    val isBandStart = when {
        heatmapColor != null -> isHeatmapStart
        dayInfo?.isPeriodDay == true || isInSelectionRange -> isStartDate
        hasDisplayPhase -> isPhaseStart
        else -> false
    }
    val isBandEnd = when {
        heatmapColor != null -> isHeatmapEnd
        dayInfo?.isPeriodDay == true || isInSelectionRange -> isEndDate
        hasDisplayPhase -> isPhaseEnd
        else -> false
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dayPressScale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(cellAspectRatio)
            .padding(
                top = if (inRange || hasDisplayPhase || heatmapColor != null) dims.xxs else dims.xs,
                bottom = if (inRange || hasDisplayPhase || heatmapColor != null) dims.xxs else dims.xs,
                start = if (isBandStart) dims.xxs else 0.dp,
                end = if (isBandEnd) dims.xxs else 0.dp
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag("day-$date")
            .drawBehind {
                if (heatmapColor != null) {
                    val strokeWidthPx = dims.xs.toPx()
                    val r = dims.sm.toPx()
                    val halfStroke = strokeWidthPx / 2
                    val extend = r + strokeWidthPx

                    val left = if (isHeatmapStart) halfStroke else -extend
                    val top = halfStroke
                    val right = if (isHeatmapEnd) size.width - halfStroke
                        else size.width + extend
                    val bottom = size.height - halfStroke

                    clipRect(0f, 0f, size.width, size.height) {
                        drawRoundRect(
                            color = heatmapColor,
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            cornerRadius = CornerRadius(r, r),
                            style = Stroke(width = strokeWidthPx)
                        )
                    }
                }
            }
            .border(
                width = if (isToday && heatmapColor == null) dims.xxs else 0.dp,
                color = if (isToday && heatmapColor == null)
                    MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .background(color = bandColor, shape = bgShape)
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
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.padding(top = dims.xxs),
                horizontalArrangement = Arrangement.spacedBy(dims.xxs)
            ) {
                if (dayInfo?.hasSymptoms == true) {
                    Box(
                        modifier = Modifier
                            .size(dims.sm)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                            .testTag("symptom-dot-$date")
                    )
                }
                if (dayInfo?.hasMedications == true) {
                    Box(
                        modifier = Modifier
                            .size(dims.sm)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                            .testTag("medication-dot-$date")
                    )
                }
                if (dayInfo?.hasNotes == true) {
                    Box(
                        modifier = Modifier
                            .size(dims.sm)
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

/**
 * Returns the [Shape] for one cell of a horizontal band (period or phase).
 *
 * Start/end caps get rounded corners on the leading/trailing side;
 * middle cells are flat rectangles. A single-cell band is rounded on all sides.
 *
 * @param isStart True when this cell is the first day of the band.
 * @param isEnd   True when this cell is the last day of the band.
 * @param radius  Corner radius for the rounded edges.
 */
internal fun bandShape(isStart: Boolean, isEnd: Boolean, radius: Dp): Shape = when {
    isStart && isEnd -> RoundedCornerShape(radius)
    isStart -> RoundedCornerShape(topStart = radius, bottomStart = radius)
    isEnd -> RoundedCornerShape(topEnd = radius, bottomEnd = radius)
    else -> RoundedCornerShape(0)
}
