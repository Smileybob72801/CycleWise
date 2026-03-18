package com.veleda.cyclewise.ui.tracker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
 * Displays the day number, optional period/phase/heatmap background colouring,
 * shaped indicators for logged symptoms (cross), medications (capsule), and
 * notes (page with folded corner), a today border ring, an optional phase
 * border, and a bounce-scale animation on press. The indicator row always
 * reserves a fixed height so the day number stays vertically stable regardless
 * of whether indicators are present.
 *
 * When a heatmap metric is active, the rendering roles swap: heatmap data takes
 * over the fill channel (background) and phase colors move to the border channel
 * via [phaseBorderColor]. When no heatmap is active, the calendar renders in its
 * normal mode with phase fills and no phase borders. The transition between modes
 * uses an [animateColorAsState] crossfade on the band color for a smooth visual switch.
 *
 * When [phaseBorderColor] is set, the cell draws a merged outline border via
 * [Modifier.drawBehind]. Adjacent phase days share a continuous border with no
 * inner dividers — middle cells extend the drawn rect beyond cell bounds and clip
 * it so only the top/bottom lines remain. The heatmap fill is also drawn inside
 * `drawBehind` (rather than via `.background()`) using the same rounded rect as
 * the border stroke, so fill and border share identical geometry and heatmap
 * color cannot leak past the border at corners.
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
 * @param heatmapColor      When a heatmap metric is active, used as the cell's background fill
 *                          for days that have logged data for the selected metric. Adjacent
 *                          heatmap days share a continuous fill band. When null, normal
 *                          phase-fill rendering applies (unless [isHeatmapModeActive] suppresses it).
 * @param isHeatmapStart    True if this date is the first day of a contiguous heatmap data run.
 * @param isHeatmapEnd      True if this date is the last day of a contiguous heatmap data run.
 * @param phaseBorderColor  When a heatmap metric is active, the phase color is rendered as a
 *                          merged outline border instead of a fill. Null when no heatmap is active
 *                          or the day has no assigned phase.
 * @param isHeatmapModeActive True when any heatmap metric is globally selected, independent of
 *                          whether this specific day has heatmap data. Used to suppress period
 *                          fill and shape overrides so period days are treated uniformly as
 *                          border-only when the heatmap is active.
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
    phaseBorderColor: Color? = null,
    isHeatmapModeActive: Boolean = false,
) {
    val dims = LocalDimensions.current
    val date = day.date.toKotlinLocalDate()
    val inRange = isInExistingRange || isInSelectionRange
    val hasDisplayPhase = displayPhase != null

    val rawBandColor = when {
        isInSelectionRange || isInRemovalRange ->
            palette.menstruation.fill.copy(alpha = PERIOD_FILL_ALPHA)
        !isHeatmapModeActive && dayInfo?.isPeriodDay == true && !isInRemovalRange ->
            palette.menstruation.fill
        phaseBorderColor != null -> Color.Transparent // fill drawn in drawBehind
        heatmapColor != null -> heatmapColor
        hasDisplayPhase -> palette.forPhase(displayPhase!!).fillSubtle
        else -> Color.Transparent
    }

    val bandColor by animateColorAsState(
        targetValue = rawBandColor,
        animationSpec = tween(300),
        label = "bandColorCrossfade",
    )

    val bgShape = when {
        isInSelectionRange ->
            bandShape(isStart = isStartDate, isEnd = isEndDate, radius = dims.sm)
        !isHeatmapModeActive && dayInfo?.isPeriodDay == true ->
            bandShape(isStart = isStartDate, isEnd = isEndDate, radius = dims.sm)
        phaseBorderColor != null ->
            bandShape(isStart = isPhaseStart, isEnd = isPhaseEnd, radius = dims.sm)
        heatmapColor != null ->
            bandShape(isStart = isHeatmapStart, isEnd = isHeatmapEnd, radius = dims.sm)
        hasDisplayPhase ->
            bandShape(isStart = isPhaseStart, isEnd = isPhaseEnd, radius = dims.sm)
        else -> CircleShape
    }

    val isBandStart = when {
        phaseBorderColor != null -> isPhaseStart
        isInSelectionRange -> isStartDate
        !isHeatmapModeActive && dayInfo?.isPeriodDay == true -> isStartDate
        heatmapColor != null -> isHeatmapStart
        hasDisplayPhase -> isPhaseStart
        else -> false
    }
    val isBandEnd = when {
        phaseBorderColor != null -> isPhaseEnd
        isInSelectionRange -> isEndDate
        !isHeatmapModeActive && dayInfo?.isPeriodDay == true -> isEndDate
        heatmapColor != null -> isHeatmapEnd
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
                top = dims.xxs,
                bottom = dims.xxs,
                start = if (isBandStart) dims.xxs else 0.dp,
                end = if (isBandEnd) dims.xxs else 0.dp
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag("day-$date")
            .background(color = bandColor, shape = bgShape)
            .drawBehind {
                if (phaseBorderColor != null) {
                    val strokeWidthPx = kotlin.math.round(dims.xs.toPx())
                    val r = dims.sm.toPx()
                    val halfStroke = strokeWidthPx / 2
                    val extend = r + strokeWidthPx
                    val cellH = kotlin.math.round(size.height)

                    val left = if (isPhaseStart) halfStroke else -extend
                    val top = halfStroke
                    val right = if (isPhaseEnd) size.width - halfStroke
                        else size.width + extend
                    val bottom = cellH - halfStroke

                    clipRect(0f, 0f, size.width, cellH) {
                        if (heatmapColor != null && !isInSelectionRange && !isInRemovalRange) {
                            drawRoundRect(
                                color = heatmapColor,
                                topLeft = Offset(left, top),
                                size = Size(right - left, bottom - top),
                                cornerRadius = CornerRadius(r, r),
                            )
                        }
                        drawRoundRect(
                            color = phaseBorderColor,
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            cornerRadius = CornerRadius(r, r),
                            style = Stroke(width = strokeWidthPx)
                        )
                    }
                }
            }
            .border(
                width = if (isToday && heatmapColor == null && phaseBorderColor == null) dims.xxs else 0.dp,
                color = if (isToday && heatmapColor == null && phaseBorderColor == null)
                    MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
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
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .padding(top = dims.xxs)
                    .height(dims.sm),
                horizontalArrangement = Arrangement.spacedBy(dims.xxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dayInfo?.hasSymptoms == true) {
                    SymptomIndicator(
                        color = MaterialTheme.colorScheme.secondary,
                        size = dims.sm,
                        modifier = Modifier.testTag("symptom-indicator-$date")
                    )
                }
                if (dayInfo?.hasMedications == true) {
                    MedicationIndicator(
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.testTag("medication-indicator-$date")
                    )
                }
                if (dayInfo?.hasNotes == true) {
                    NotesIndicator(
                        color = MaterialTheme.colorScheme.outline,
                        size = dims.sm,
                        modifier = Modifier.testTag("notes-indicator-$date")
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
 * Small cross/plus indicator drawn on a [Canvas] to represent logged symptoms.
 *
 * Two perpendicular lines with rounded caps form the cross. The stroke width
 * and arm length are proportional to [size] so the shape scales cleanly.
 *
 * @param color    Fill colour for the cross strokes.
 * @param size     Side length of the square bounding box.
 * @param modifier Modifier forwarded to the [Canvas] (use for test tags, padding, etc.).
 */
@Composable
private fun SymptomIndicator(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.width * 0.3f
        val center = this.size.width / 2
        val arm = this.size.width * 0.4f
        drawLine(
            color = color,
            start = Offset(center - arm, center),
            end = Offset(center + arm, center),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(center, center - arm),
            end = Offset(center, center + arm),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Horizontal capsule/pill indicator representing logged medications.
 *
 * Rendered as a [Box] with fully rounded ends (`RoundedCornerShape(50)`).
 * Width is [dims.sm][com.veleda.cyclewise.ui.theme.Dimensions.sm] and height
 * is [dims.xs][com.veleda.cyclewise.ui.theme.Dimensions.xs], producing an
 * elongated pill shape that is visually distinct from the cross and page indicators.
 *
 * @param color    Fill colour for the capsule.
 * @param modifier Modifier forwarded to the outer [Box] (use for test tags, padding, etc.).
 */
@Composable
private fun MedicationIndicator(color: Color, modifier: Modifier = Modifier) {
    val dims = LocalDimensions.current
    Box(
        modifier = modifier
            .size(width = dims.sm, height = dims.xs)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

/**
 * Page/document indicator drawn on a [Canvas] to represent logged notes.
 *
 * A filled vertical rectangle with a triangular fold cut from the top-right
 * corner. The fold is drawn at 50 % alpha to create a subtle crease effect
 * that distinguishes this shape from a plain rectangle.
 *
 * @param color    Fill colour for the page body and fold.
 * @param size     Side length of the square bounding box.
 * @param modifier Modifier forwarded to the [Canvas] (use for test tags, padding, etc.).
 */
@Composable
private fun NotesIndicator(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val fold = w * 0.3f
        val bodyPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w - fold, 0f)
            lineTo(w, fold)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(bodyPath, color)
        val foldPath = Path().apply {
            moveTo(w - fold, 0f)
            lineTo(w - fold, fold)
            lineTo(w, fold)
            close()
        }
        drawPath(foldPath, color, alpha = 0.5f)
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
