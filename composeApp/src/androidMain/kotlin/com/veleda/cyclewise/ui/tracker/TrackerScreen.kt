package com.veleda.cyclewise.ui.tracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.ui.nav.NavRoute
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinLocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import kotlin.time.Clock
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.YearMonth as JavaYearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(navController: NavController) {
    val viewModel: TrackerViewModel = koinInject(scope = getKoin().getScope("session"))
    val uiState by viewModel.uiState.collectAsState()
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val onEditClick: (LocalDate) -> Unit = remember(navController, sheetState) {
        { date ->
            coroutineScope.launch {
                sheetState.hide()
                viewModel.onEvent(TrackerEvent.DismissLogSheet)
                navController.navigate(NavRoute.DailyLog.createRoute(date))
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
            LogSummarySheetContent(
                log = uiState.logForSheet!!,
                symptomLibrary = uiState.symptomLibrary,
                onEditClick = onEditClick
            )
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = calendarState.firstVisibleMonth.yearMonth.toString(),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                textAlign = TextAlign.Center
            )
            DaysOfWeekTitle(daysOfWeek = firstDayOfWeek.let {
                val days = JavaDayOfWeek.entries
                days.subList(it.value - 1, days.size) + days.subList(0, it.value - 1)
            })
            HorizontalCalendar(
                state = calendarState,
                modifier = Modifier
                    .weight(1f)
                    .testTag("calendar-root"),
                dayContent = { day ->
                    val date = day.date.toKotlinLocalDate()
                    val cycleForDate = uiState.periods.find { date in (it.startDate..(it.endDate ?: today)) }
                    val selectionRange = remember(uiState.selectionStartDate, uiState.selectionEndDate) {
                        val start = uiState.selectionStartDate
                        val end = uiState.selectionEndDate
                        if (start != null && end != null) start..end else if (start != null) start..start else null
                    }
                    val dayInfo = uiState.dayDetails[date]

                    Day(
                        day = day,
                        dayInfo = dayInfo,
                        isSelected = date == uiState.selectionStartDate || date == uiState.selectionEndDate,
                        isStartDate = cycleForDate?.startDate == date,
                        isEndDate = cycleForDate?.endDate == date,
                        isInExistingRange = cycleForDate != null,
                        isInSelectionRange = selectionRange?.contains(date) ?: false,
                        onClick = { viewModel.onEvent(TrackerEvent.DateClicked(date, cycleForDate)) }
                    )
                }
            )

            Spacer(Modifier.height(16.dp))
            AnimatedVisibility(visible = uiState.isSelectingRange) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.onEvent(TrackerEvent.ClearSelectionClicked) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { viewModel.onEvent(TrackerEvent.SaveSelectionClicked) },
                        modifier = Modifier.testTag("save-cycle-button")
                    ) {
                        Text("Save Period")
                    }
                }
            }
            AnimatedVisibility(visible = !uiState.isSelectingRange && uiState.ongoingPeriod != null) {
                Button(
                    onClick = { viewModel.onEvent(TrackerEvent.EndPeriodClicked) },
                    modifier = Modifier.testTag("end-cycle-button")
                ) {
                    Text("End Period Today")
                }
            }
            AnimatedVisibility(visible = !uiState.isSelectingRange && uiState.ongoingPeriod == null) {
                Text(
                    "Tap a date to log a new cycle",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LogSummarySheetContent(
    log: FullDailyLog,
    symptomLibrary: List<Symptom>,
    onEditClick: (LocalDate) -> Unit) {
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
                text = "Log for ${log.entry.entryDate}",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = { onEditClick(log.entry.entryDate) },
                modifier = Modifier.testTag("edit-log-button")
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Log")
            }
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        log.entry.flowIntensity?.let {
            InfoRow(icon = Icons.Default.Build, title = "Flow", value = it.name)
        }
        log.entry.moodScore?.let {
            InfoRow(icon = Icons.Outlined.Star, title = "Mood", value = "$it / 5")
        }

        if (log.symptomLogs.isNotEmpty()) {
            Text("Symptoms", style = MaterialTheme.typography.titleMedium)
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

@Composable
private fun Day(
    day: CalendarDay,
    dayInfo: CalendarDayInfo?,
    isSelected: Boolean,
    isStartDate: Boolean,
    isEndDate: Boolean,
    isInExistingRange: Boolean,
    isInSelectionRange: Boolean,
    onClick: () -> Unit
) {
    val date = day.date.toKotlinLocalDate()
    val inRange = isInExistingRange || isInSelectionRange
    val shape = when {
        isStartDate && isEndDate -> CircleShape
        isStartDate -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
        isEndDate -> RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
        inRange -> RoundedCornerShape(0)
        else -> CircleShape
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(vertical = if (inRange && !isStartDate && !isEndDate) 0.dp else 4.dp)
            .testTag("day-$date")
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .background(
                color = if (dayInfo?.isPeriodDay == true) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = shape
            )
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = onClick
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