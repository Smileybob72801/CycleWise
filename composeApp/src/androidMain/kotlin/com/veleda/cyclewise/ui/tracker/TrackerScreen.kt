package com.veleda.cyclewise.ui.tracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import androidx.navigation.NavController
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.nav.NavRoute
import com.veleda.cyclewise.ui.theme.CyclePhasePalette
import com.veleda.cyclewise.ui.theme.buildCyclePhasePalette
import com.veleda.cyclewise.ui.utils.toLocalizedDateString
import com.veleda.cyclewise.ui.utils.toLocalizedMonthYearString
import kotlinx.datetime.toKotlinLocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import kotlin.time.Clock
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.YearMonth as JavaYearMonth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrackerScreen(navController: NavController) {
    val viewModel: TrackerViewModel = koinInject(scope = getKoin().getScope("session"))
    val uiState by viewModel.uiState.collectAsState()
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                        NavRoute.DailyLog.createRoute(effect.date, effect.isPeriodDay)
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
                onDeleteClick = { periodId -> viewModel.onEvent(TrackerEvent.DeletePeriodRequested(periodId)) }
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
            Text(
                text = calendarState.firstVisibleMonth.yearMonth.toLocalizedMonthYearString(),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                textAlign = TextAlign.Center
            )
            DaysOfWeekTitle(daysOfWeek = firstDayOfWeek.let {
                val days = JavaDayOfWeek.entries
                days.subList(it.value - 1, days.size) + days.subList(0, it.value - 1)
            })
            PhaseLegend(palette = palette, phaseVisible = phaseVisible)
            HorizontalCalendar(
                state = calendarState,
                modifier = Modifier
                    .weight(1f)
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

                    val handleLongPress: (() -> Unit)? = if (dayIsNotTappable) null else {
                        { viewModel.onEvent(TrackerEvent.PeriodMarkDay(date)) }
                    }

                    Day(
                        day = day,
                        dayInfo = dayInfo,
                        isSelected = false,
                        isStartDate = cycleForDate?.startDate == date,
                        isEndDate = cycleForDate?.endDate == date,
                        isInExistingRange = cycleForDate != null,
                        isInSelectionRange = false,
                        isPhaseStart = displayPhase != null && displayPhase != prevDisplayPhase,
                        isPhaseEnd = displayPhase != null && displayPhase != nextDisplayPhase,
                        palette = palette,
                        displayPhase = displayPhase,
                        onTap = handleTap,
                        onLongPress = handleLongPress
                    )
                }
            )

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(visible = uiState.ongoingPeriod != null) {
                Text(
                    stringResource(R.string.tracker_ongoing_period, uiState.ongoingPeriod!!.startDate.toLocalizedDateString()),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = uiState.ongoingPeriod == null) {
                Text(
                    stringResource(R.string.tracker_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Bottom-sheet content summarising a single day's log.
 *
 * @param log         The full daily log to display.
 * @param periodId    Associated period ID, or null if the day is not a period day.
 * @param cyclePhase  Computed cycle phase for this date, or null if not determinable.
 * @param symptomLibrary  Library of all symptoms for name resolution.
 * @param medicationLibrary  Library of all medications for name resolution.
 * @param waterCups   Number of water cups logged, or null.
 * @param showMood    Whether to display the mood score row (controlled by user setting).
 * @param showEnergy  Whether to display the energy level row (controlled by user setting).
 * @param showLibido  Whether to display the libido score row (controlled by user setting).
 * @param onEditClick Callback when the user taps the edit button.
 * @param onDeleteClick Callback when the user taps the delete button.
 */
@Composable
private fun LogSummarySheetContent(
    log: FullDailyLog,
    periodId: String?,
    cyclePhase: CyclePhase? = null,
    symptomLibrary: List<Symptom>,
    medicationLibrary: List<Medication>,
    waterCups: Int?,
    showMood: Boolean,
    showEnergy: Boolean,
    showLibido: Boolean,
    onEditClick: (LocalDate) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.tracker_log_for, log.entry.entryDate.toLocalizedDateString()),
                style = MaterialTheme.typography.titleLarge
            )
            Row { // Group Edit and Delete buttons
                IconButton(onClick = { onEditClick(log.entry.entryDate) },
                    modifier = Modifier.testTag("edit-log-button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.tracker_edit_log))
                }
                // Show Delete button only if a period is associated with the log day
                if (periodId != null) {
                    IconButton(
                        onClick = { onDeleteClick(periodId) },
                        modifier = Modifier.testTag("delete-period-button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.tracker_delete_period))
                    }
                }
            }
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        cyclePhase?.let { phase ->
            InfoRow(
                icon = Icons.Outlined.Star,
                title = stringResource(R.string.tracker_phase_label),
                value = when (phase) {
                    CyclePhase.MENSTRUATION -> stringResource(R.string.phase_color_period_label)
                    CyclePhase.FOLLICULAR -> stringResource(R.string.phase_color_follicular_label)
                    CyclePhase.OVULATION -> stringResource(R.string.phase_color_ovulation_label)
                    CyclePhase.LUTEAL -> stringResource(R.string.phase_color_luteal_label)
                }
            )
        }

        log.periodLog?.flowIntensity?.let {
            InfoRow(icon = Icons.Default.Build, title = stringResource(R.string.tracker_flow_label), value = it.name)
        }

        if (showMood) {
            log.entry.moodScore?.let {
                InfoRow(icon = Icons.Outlined.Star, title = stringResource(R.string.tracker_mood_label), value = "$it / 5")
            }
        }

        if (showEnergy) {
            log.entry.energyLevel?.let {
                InfoRow(icon = Icons.Outlined.Star, title = stringResource(R.string.tracker_energy_label), value = "$it / 5")
            }
        }

        if (showLibido) {
            log.entry.libidoScore?.let {
                InfoRow(icon = Icons.Outlined.Star, title = stringResource(R.string.tracker_libido_label), value = "$it / 5")
            }
        }

        waterCups?.let {
            if (it > 0) InfoRow(icon = Icons.Default.Build, title = stringResource(R.string.tracker_water_label), value = stringResource(R.string.tracker_water_cups, it))
        }

        if (log.symptomLogs.isNotEmpty()) {
            Text(stringResource(R.string.tracker_symptoms_label), style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(log.symptomLogs) { symptomLog ->
                    val symptomInfo = symptomLibrary.find { it.id == symptomLog.symptomId }
                    if (symptomInfo != null) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(symptomInfo.name) }
                        )
                    }
                }
            }
        }

        if (log.medicationLogs.isNotEmpty()) {
            Text(stringResource(R.string.tracker_medications_label), style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(log.medicationLogs) { medicationLog ->
                    val medicationInfo = medicationLibrary.find { it.id == medicationLog.medicationId }
                    if (medicationInfo != null) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(medicationInfo.name) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Text(text = "$title:", style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Day(
    day: CalendarDay,
    dayInfo: CalendarDayInfo?,
    isSelected: Boolean,
    isStartDate: Boolean,
    isEndDate: Boolean,
    isInExistingRange: Boolean,
    isInSelectionRange: Boolean,
    isPhaseStart: Boolean = true,
    isPhaseEnd: Boolean = true,
    palette: CyclePhasePalette,
    displayPhase: CyclePhase? = null,
    onTap: (() -> Unit)?,
    onLongPress: (() -> Unit)?
) {
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
        hasDisplayPhase -> phaseShape
        else -> CircleShape
    }

    val isPhaseMiddle = hasDisplayPhase && !isPhaseStart && !isPhaseEnd

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(vertical = when {
                inRange && !isStartDate && !isEndDate -> 0.dp
                isPhaseMiddle -> 0.dp
                else -> 4.dp
            })
            .testTag("day-$date")
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .background(
                color = when {
                    dayInfo?.isPeriodDay == true ->
                        palette.menstruation.fill
                    hasDisplayPhase ->
                        palette.forPhase(displayPhase!!).fillSubtle
                    else -> Color.Transparent
                },
                shape = bgShape
            )
            .combinedClickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = { onTap?.invoke() },
                onLongClick = { onLongPress?.invoke() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (day.position == DayPosition.MonthDate) MaterialTheme.colorScheme.onSurface else Color.Gray
            )

            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (dayInfo?.hasSymptoms == true) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                            .testTag("symptom-dot-$date")
                    )
                }
                if (dayInfo?.hasMedications == true) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                            .testTag("medication-dot-$date")
                    )
                }
            }
            if (dayInfo?.isPeriodDay == true) {
                Box(modifier = Modifier.testTag("period-day-$date"))
            }
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
 * Horizontal legend showing cycle-phase colours with labels.
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(
            color = palette.menstruation.dot,
            label = stringResource(R.string.phase_color_period_label)
        )
        if (phaseVisible[CyclePhase.FOLLICULAR] != false) {
            LegendItem(
                color = palette.follicular.dot,
                label = stringResource(R.string.phase_color_follicular_label)
            )
        }
        if (phaseVisible[CyclePhase.OVULATION] != false) {
            LegendItem(
                color = palette.ovulation.dot,
                label = stringResource(R.string.phase_color_ovulation_label)
            )
        }
        if (phaseVisible[CyclePhase.LUTEAL] != false) {
            LegendItem(
                color = palette.luteal.dot,
                label = stringResource(R.string.phase_color_luteal_label)
            )
        }
    }
}

/**
 * Single legend entry: a small coloured dot followed by a label.
 *
 * @param color The fill colour for the dot.
 * @param label The text displayed next to the dot.
 */
@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
