package com.veleda.cyclewise.ui.tracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import com.veleda.cyclewise.R
import androidx.navigation.NavController
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.nav.NavRoute
import com.veleda.cyclewise.ui.theme.CyclePhasePalette
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.theme.buildCyclePhasePalette
import com.veleda.cyclewise.ui.utils.toLocalizedDateString
import com.veleda.cyclewise.ui.utils.toLocalizedMonthYearString
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinLocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import kotlin.time.Clock
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.YearMonth as JavaYearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(navController: NavController) {
    val dims = LocalDimensions.current
    val viewModel: TrackerViewModel = koinInject(scope = getKoin().getScope("session"))
    val uiState by viewModel.uiState.collectAsState()
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val appSettings: AppSettings = getKoin().get()
    val showMood by appSettings.showMoodInSummary.collectAsState(initial = true)
    val showEnergy by appSettings.showEnergyInSummary.collectAsState(initial = true)
    val showLibido by appSettings.showLibidoInSummary.collectAsState(initial = true)

    val showFollicular by appSettings.showFollicularPhase.collectAsState(initial = true)
    val showOvulation by appSettings.showOvulationPhase.collectAsState(initial = true)
    val showLuteal by appSettings.showLutealPhase.collectAsState(initial = true)

    val menstruationHex by appSettings.menstruationColor.collectAsState(initial = CyclePhaseColors.DEFAULT_MENSTRUATION_HEX)
    val follicularHex by appSettings.follicularColor.collectAsState(initial = CyclePhaseColors.DEFAULT_FOLLICULAR_HEX)
    val ovulationHex by appSettings.ovulationColor.collectAsState(initial = CyclePhaseColors.DEFAULT_OVULATION_HEX)
    val lutealHex by appSettings.lutealColor.collectAsState(initial = CyclePhaseColors.DEFAULT_LUTEAL_HEX)

    val darkTheme = isSystemInDarkTheme()
    val customColors: Map<CyclePhase, Color>? = remember(menstruationHex, follicularHex, ovulationHex, lutealHex) {
        val map = buildMap {
            parseHexColor(menstruationHex)?.let { put(CyclePhase.MENSTRUATION, it) }
            parseHexColor(follicularHex)?.let { put(CyclePhase.FOLLICULAR, it) }
            parseHexColor(ovulationHex)?.let { put(CyclePhase.OVULATION, it) }
            parseHexColor(lutealHex)?.let { put(CyclePhase.LUTEAL, it) }
        }
        map.ifEmpty { null }
    }
    val palette = remember(darkTheme, customColors) {
        buildCyclePhasePalette(darkTheme, customColors)
    }

    val phaseVisible: Map<CyclePhase, Boolean> = remember(showFollicular, showOvulation, showLuteal) {
        mapOf(
            CyclePhase.MENSTRUATION to true,
            CyclePhase.FOLLICULAR to showFollicular,
            CyclePhase.OVULATION to showOvulation,
            CyclePhase.LUTEAL to showLuteal
        )
    }

    // Trigger auto-close logic when the screen is first composed/entered.
    LaunchedEffect(Unit) {
        viewModel.onEvent(TrackerEvent.ScreenEntered)
    }

    // Collect one-time effects (navigation).
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TrackerEffect.NavigateToDailyLog -> {
                    navController.navigate(
                        NavRoute.DailyLog.createRoute(effect.date)
                    )
                }
            }
        }
    }

    val currentMonth = remember { JavaYearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val calendarState = rememberCalendarState(startMonth, endMonth, currentMonth, firstDayOfWeek)

    val boundsRegistry = remember { DayBoundsRegistry() }
    var dragAnchor by remember { mutableStateOf<LocalDate?>(null) }
    var dragCurrent by remember { mutableStateOf<LocalDate?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    // Coordinates of the calendar container Box for pointer mapping.
    var calendarBoxCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    if (uiState.logForSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(TrackerEvent.DismissLogSheet) },
            sheetState = sheetState
        ) {
            val sheetPhase = uiState.logForSheet?.let { log ->
                uiState.dayDetails[log.entry.entryDate]?.cyclePhase
            }
            LogSummarySheetContent(
                log = uiState.logForSheet!!,
                periodId = uiState.periodIdForSheet,
                cyclePhase = sheetPhase,
                symptomLibrary = uiState.symptomLibrary,
                medicationLibrary = uiState.medicationLibrary,
                waterCups = uiState.waterCupsForSheet,
                showMood = showMood,
                showEnergy = showEnergy,
                showLibido = showLibido,
                onEditClick = { date -> viewModel.onEvent(TrackerEvent.EditLogClicked(date)) },
                onDeleteClick = { periodId -> viewModel.onEvent(TrackerEvent.DeletePeriodRequested(periodId)) },
                onViewFullLogClick = { date -> viewModel.onEvent(TrackerEvent.EditLogClicked(date)) }
            )
        }
    }

    if (uiState.showDeleteConfirmation && uiState.periodIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(TrackerEvent.DeletePeriodDismissed) },
            title = { Text(stringResource(R.string.tracker_delete_title)) },
            text = { Text(stringResource(R.string.tracker_delete_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(TrackerEvent.DeletePeriodConfirmed(uiState.periodIdToDelete!!))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.tracker_delete_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.onEvent(TrackerEvent.DeletePeriodDismissed) }) {
                    Text(stringResource(R.string.tracker_cancel))
                }
            }
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    coroutineScope.launch {
                        calendarState.animateScrollToMonth(
                            calendarState.firstVisibleMonth.yearMonth.minusMonths(1)
                        )
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.tracker_previous_month)
                    )
                }
                Text(
                    text = calendarState.firstVisibleMonth.yearMonth.toLocalizedMonthYearString(),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = {
                    coroutineScope.launch {
                        calendarState.animateScrollToMonth(
                            calendarState.firstVisibleMonth.yearMonth.plusMonths(1)
                        )
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.tracker_next_month)
                    )
                }
            }

            FilledTonalButton(
                onClick = {
                    coroutineScope.launch {
                        calendarState.animateScrollToMonth(currentMonth)
                    }
                }
            ) {
                Text(stringResource(R.string.tracker_today))
            }

            DaysOfWeekTitle(daysOfWeek = firstDayOfWeek.let {
                val days = JavaDayOfWeek.entries
                days.subList(it.value - 1, days.size) + days.subList(0, it.value - 1)
            })
            PhaseLegend(palette = palette, phaseVisible = phaseVisible)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { calendarBoxCoords = it }
                    .pointerInput(Unit) {
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

                            dragAnchor = anchorDate
                            dragCurrent = anchorDate
                            isDragging = true

                            var dragged = false
                            drag(longPress.id) { change ->
                                dragged = true
                                val dragRootPos = calendarBoxCoords
                                    ?.localToRoot(change.position)
                                if (dragRootPos != null) {
                                    val hoveredDate = boundsRegistry.dateAt(dragRootPos)
                                    if (hoveredDate != null) {
                                        dragCurrent = hoveredDate
                                    }
                                }
                                change.consume()
                            }

                            // Gesture ended — emit the appropriate event.
                            val anchor = dragAnchor
                            val current = dragCurrent
                            if (anchor != null && current != null) {
                                if (dragged && anchor != current) {
                                    viewModel.onEvent(
                                        TrackerEvent.PeriodRangeDragged(anchor, current)
                                    )
                                } else {
                                    viewModel.onEvent(TrackerEvent.PeriodMarkDay(anchor))
                                }
                            }

                            // Reset drag state.
                            dragAnchor = null
                            dragCurrent = null
                            isDragging = false
                        }
                    }
            ) {
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
                            { viewModel.onEvent(TrackerEvent.DayTapped(date)) }
                        }

                        val inSelectionRange = isDragging && dragAnchor != null && dragCurrent != null && run {
                            val selStart = minOf(dragAnchor!!, dragCurrent!!)
                            val selEnd = maxOf(dragAnchor!!, dragCurrent!!)
                            date in selStart..selEnd
                        }

                        CalendarDayCell(
                            day = day,
                            dayInfo = dayInfo,
                            isToday = date == today,
                            isStartDate = cycleForDate?.startDate == date,
                            isEndDate = cycleForDate?.endDate == date,
                            isInExistingRange = cycleForDate != null,
                            isInSelectionRange = inSelectionRange,
                            isDragging = isDragging,
                            isPhaseStart = displayPhase != null && displayPhase != prevDisplayPhase,
                            isPhaseEnd = displayPhase != null && displayPhase != nextDisplayPhase,
                            palette = palette,
                            displayPhase = displayPhase,
                            onTap = handleTap,
                            boundsRegistry = boundsRegistry
                        )
                    }
                )
            }

            Spacer(Modifier.height(dims.md))

            AnimatedVisibility(visible = uiState.ongoingPeriod != null) {
                val ongoingStartDate = uiState.ongoingPeriod?.startDate?.toLocalizedDateString() ?: ""
                Text(
                    stringResource(R.string.tracker_ongoing_period, ongoingStartDate),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(dims.sm),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = uiState.ongoingPeriod == null) {
                Text(
                    stringResource(R.string.tracker_instructions_drag),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(dims.sm)
                )
            }
            Spacer(Modifier.height(dims.md))
        }
    }
}

@Composable
private fun DaysOfWeekTitle(daysOfWeek: List<JavaDayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Horizontal legend showing cycle-phase colours as compact chip-style entries.
 *
 * Placed between the day-of-week header and the calendar grid so users
 * can identify what each background tint represents. Only visible phases
 * (as configured in settings) are shown; Period is always displayed.
 *
 * @param palette      The current [CyclePhasePalette] providing per-phase dot colors.
 * @param phaseVisible Map of each [CyclePhase] to its visibility flag.
 */
@Composable
private fun PhaseLegend(
    palette: CyclePhasePalette,
    phaseVisible: Map<CyclePhase, Boolean> = emptyMap()
) {
    val dims = LocalDimensions.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.md, vertical = dims.xs),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendChip(
            color = palette.menstruation.dot,
            label = stringResource(R.string.phase_color_period_label)
        )
        if (phaseVisible[CyclePhase.FOLLICULAR] != false) {
            LegendChip(
                color = palette.follicular.dot,
                label = stringResource(R.string.phase_color_follicular_label)
            )
        }
        if (phaseVisible[CyclePhase.OVULATION] != false) {
            LegendChip(
                color = palette.ovulation.dot,
                label = stringResource(R.string.phase_color_ovulation_label)
            )
        }
        if (phaseVisible[CyclePhase.LUTEAL] != false) {
            LegendChip(
                color = palette.luteal.dot,
                label = stringResource(R.string.phase_color_luteal_label)
            )
        }
    }
}

/**
 * Single legend entry rendered as a compact chip: a small coloured swatch
 * followed by a label, wrapped in a [Surface] with `surfaceVariant` background.
 *
 * @param color The fill colour for the swatch.
 * @param label The text displayed next to the swatch.
 */
@Composable
private fun LegendChip(color: Color, label: String) {
    val dims = LocalDimensions.current

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dims.sm, vertical = dims.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.xs)
        ) {
            Box(
                modifier = Modifier
                    .size(dims.sm)
                    .background(color, RoundedCornerShape(dims.xxs))
            )
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
