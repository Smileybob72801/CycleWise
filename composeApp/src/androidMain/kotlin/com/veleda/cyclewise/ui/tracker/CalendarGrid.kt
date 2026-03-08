package com.veleda.cyclewise.ui.tracker

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.ui.coachmark.ActiveCoachMark
import com.veleda.cyclewise.ui.coachmark.CoachMarkState
import com.veleda.cyclewise.ui.coachmark.HintKey
import com.veleda.cyclewise.ui.coachmark.coachMarkTarget
import com.veleda.cyclewise.ui.theme.CyclePhasePalette
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toKotlinLocalDate

/**
 * Calendar grid with long-press/drag gesture handling for period range selection.
 *
 * Encapsulates the [HorizontalCalendar] widget and the pointer-input gesture
 * detector that drives the drag-to-mark-period interaction.
 *
 * Uses [BoxWithConstraints] to compute a responsive cell aspect ratio. On devices
 * where square cells would exceed the available height (e.g. landscape tablets),
 * the ratio is widened so all six possible calendar rows fit without compression.
 *
 * @param uiState            Current tracker UI state with periods, dayDetails, etc.
 * @param calendarState       Calendar library state for month scrolling.
 * @param palette             Cycle phase colour palette.
 * @param phaseVisible        Map of each [CyclePhase] to its visibility flag.
 * @param today               Today's date.
 * @param boundsRegistry      Registry mapping screen coordinates to calendar dates.
 * @param isDragging          Whether a drag gesture is currently active.
 * @param dragAnchor          The date where the drag started.
 * @param dragCurrent         The date currently under the pointer during drag.
 * @param activeHint          Currently active coach mark hint, if any.
 * @param pendingKey          Pending coach mark hint key, if any.
 * @param coachMarkState      Coach mark system state.
 * @param calendarBoxCoords   Layout coordinates of the calendar container.
 * @param onCalendarBoxPositioned Callback when the calendar container is positioned.
 * @param onDragStateChanged  Callback to update drag state (anchor, current, isDragging).
 * @param onEvent             Callback for tracker events.
 * @param onTutorialAdvance   Callback to advance the tutorial after the user long-presses
 *                            a day during the [HintKey.TRACKER_LONG_PRESS] step.
 * @param modifier            Modifier applied to the outer [Box].
 */
@Composable
internal fun CalendarGrid(
    uiState: TrackerUiState,
    calendarState: CalendarState,
    palette: CyclePhasePalette,
    phaseVisible: Map<CyclePhase, Boolean>,
    today: LocalDate,
    boundsRegistry: DayBoundsRegistry,
    isDragging: Boolean,
    dragAnchor: LocalDate?,
    dragCurrent: LocalDate?,
    activeHint: ActiveCoachMark?,
    pendingKey: HintKey?,
    coachMarkState: CoachMarkState,
    calendarBoxCoords: LayoutCoordinates?,
    onCalendarBoxPositioned: (LayoutCoordinates) -> Unit,
    onDragStateChanged: (anchor: LocalDate?, current: LocalDate?, dragging: Boolean) -> Unit,
    onEvent: (TrackerEvent) -> Unit,
    onTutorialAdvance: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val anchorPeriod = if (isDragging && dragAnchor != null) {
        uiState.periods.find { dragAnchor in (it.startDate..(it.endDate ?: today)) }
    } else null

    BoxWithConstraints(
        modifier = modifier
            .coachMarkTarget(HintKey.TRACKER_WELCOME, coachMarkState)
            .coachMarkTarget(HintKey.TRACKER_LONG_PRESS, coachMarkState)
            .coachMarkTarget(HintKey.TRACKER_DRAG, coachMarkState)
            .coachMarkTarget(HintKey.TRACKER_ADJUST, coachMarkState)
            .coachMarkTarget(HintKey.TRACKER_TAP_DAY, coachMarkState)
            .onGloballyPositioned { onCalendarBoxPositioned(it) }
            .pointerInput(activeHint, pendingKey) {
                // Block gestures during walkthrough, except during the LONG_PRESS step.
                val blockGestures = when {
                    activeHint == null && pendingKey == null -> false
                    activeHint?.def?.key == HintKey.TRACKER_LONG_PRESS -> false
                    else -> true
                }
                if (blockGestures) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val rootPos = calendarBoxCoords
                        ?.localToRoot(down.position)
                        ?: return@awaitEachGesture
                    val anchorDate = boundsRegistry.dateAt(rootPos)
                        ?: return@awaitEachGesture

                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) {
                        // Cancelled before long-press threshold — let other gestures handle.
                        return@awaitEachGesture
                    }

                    onDragStateChanged(anchorDate, anchorDate, true)

                    var dragged = false
                    var lastDragDate = anchorDate
                    drag(longPress.id) { change ->
                        dragged = true
                        val dragRootPos = calendarBoxCoords
                            ?.localToRoot(change.position)
                        if (dragRootPos != null) {
                            val hoveredDate = boundsRegistry.dateAt(dragRootPos)
                            if (hoveredDate != null) {
                                lastDragDate = hoveredDate
                                onDragStateChanged(anchorDate, hoveredDate, true)
                            }
                        }
                        change.consume()
                    }

                    // Gesture ended — emit the appropriate event.
                    // Use local lastDragDate — not the stale parameter.
                    if (dragged && anchorDate != lastDragDate) {
                        onEvent(TrackerEvent.PeriodRangeDragged(anchorDate, lastDragDate))
                    } else {
                        onEvent(TrackerEvent.PeriodMarkDay(anchorDate))
                    }

                    // If this was the LONG_PRESS tutorial step, advance the walkthrough.
                    if (activeHint?.def?.key == HintKey.TRACKER_LONG_PRESS) {
                        onTutorialAdvance()
                    }

                    // Reset drag state.
                    onDragStateChanged(null, null, false)
                }
            }
    ) {
        val maxRows = 6
        val cellWidth = maxWidth / 7
        val availableRowHeight = maxHeight / maxRows
        val cellAspectRatio = if (availableRowHeight > 0.dp) {
            (cellWidth / availableRowHeight).coerceAtLeast(1f)
        } else {
            1f
        }

        HorizontalCalendar(
            state = calendarState,
            userScrollEnabled = !isDragging,
            modifier = Modifier
                .fillMaxSize()
                .testTag("calendar-root"),
            dayContent = { day ->
                val date = day.date.toKotlinLocalDate()
                val cycleForDate = uiState.periods.find { date in (it.startDate..(it.endDate ?: today)) }
                val dayInfo = uiState.dayDetails[date]

                // Effective display phase (null for period days — they keep their own color).
                // Also null when the user has toggled the phase off in settings.
                val rawPhase = if (dayInfo?.isPeriodDay == true) null else dayInfo?.cyclePhase
                val displayPhase = rawPhase?.takeIf { phaseVisible[it] != false }
                val prevInfo = uiState.dayDetails[date.minus(1, DateTimeUnit.DAY)]
                val nextInfo = uiState.dayDetails[date.plus(1, DateTimeUnit.DAY)]
                val prevRaw = if (prevInfo?.isPeriodDay == true) null else prevInfo?.cyclePhase
                val nextRaw = if (nextInfo?.isPeriodDay == true) null else nextInfo?.cyclePhase
                val prevDisplayPhase = prevRaw?.takeIf { phaseVisible[it] != false }
                val nextDisplayPhase = nextRaw?.takeIf { phaseVisible[it] != false }

                val dayIsNotTappable = day.position != DayPosition.MonthDate

                val handleTap: (() -> Unit)? = if (dayIsNotTappable) null else {
                    { onEvent(TrackerEvent.DayTapped(date)) }
                }

                val inSelectionRange = isDragging && dragAnchor != null && dragCurrent != null && run {
                    val selStart = minOf(dragAnchor, dragCurrent)
                    val selEnd = maxOf(dragAnchor, dragCurrent)
                    date in selStart..selEnd
                }

                val isInRemovalRange = isDragging && dragAnchor != null && dragCurrent != null
                    && anchorPeriod != null && run {
                    val periodEnd = anchorPeriod.endDate ?: today
                    when {
                        // Shrink from start: anchor at start, dragging right into period
                        dragAnchor == anchorPeriod.startDate
                            && dragAnchor != periodEnd
                            && dragCurrent > dragAnchor
                            && dragCurrent <= periodEnd ->
                            date >= dragAnchor && date < dragCurrent

                        // Shrink from end: anchor at end, dragging left into period
                        dragAnchor == periodEnd
                            && dragAnchor != anchorPeriod.startDate
                            && dragCurrent < dragAnchor
                            && dragCurrent >= anchorPeriod.startDate ->
                            date > dragCurrent && date <= dragAnchor

                        else -> false
                    }
                }

                val heatmapOverlay = uiState.selectedHeatmapMetric?.let { metric ->
                    uiState.heatmapIntensities[date]?.let { intensity ->
                        heatmapColor(metric, intensity)
                    }
                }

                CalendarDayCell(
                    day = day,
                    dayInfo = dayInfo,
                    isToday = date == today,
                    isStartDate = cycleForDate?.startDate == date,
                    isEndDate = cycleForDate?.endDate == date,
                    isInExistingRange = cycleForDate != null,
                    isInSelectionRange = inSelectionRange,
                    isInRemovalRange = isInRemovalRange,
                    isDragging = isDragging,
                    isPhaseStart = displayPhase != null && displayPhase != prevDisplayPhase,
                    isPhaseEnd = displayPhase != null && displayPhase != nextDisplayPhase,
                    palette = palette,
                    displayPhase = displayPhase,
                    onTap = handleTap,
                    boundsRegistry = boundsRegistry,
                    cellAspectRatio = cellAspectRatio,
                    heatmapColor = heatmapOverlay,
                )
            }
        )
    }
}
