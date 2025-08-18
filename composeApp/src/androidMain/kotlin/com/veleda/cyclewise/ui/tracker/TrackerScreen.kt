package com.veleda.cyclewise.ui.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.veleda.cyclewise.di.SESSION_SCOPE
import com.veleda.cyclewise.ui.nav.NavRoute
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.toKotlinLocalDate
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.compose.getKoin
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.YearMonth as JavaYearMonth

@Composable
fun TrackerScreen(navController: NavController) {
    var showPastCycleDialog by remember { mutableStateOf(false) }
    var selectedPastDate by remember { mutableStateOf<LocalDate?>(null) }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    val viewModel: CycleViewModel = koinViewModel(scope = getKoin().getScope("session"))
    val uiState by viewModel.uiState.collectAsState()

    val currentMonth = remember { JavaYearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val state = rememberCalendarState(startMonth, endMonth, currentMonth, firstDayOfWeek)

    LaunchedEffect(state.firstVisibleMonth) {
        val visibleMonth = state.firstVisibleMonth.yearMonth
        viewModel.loadDataForMonth(YearMonth(visibleMonth.year, visibleMonth.monthValue))
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.firstVisibleMonth.yearMonth.toString(),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
            DaysOfWeekTitle(daysOfWeek = firstDayOfWeek.let {
                val days = JavaDayOfWeek.entries
                days.subList(it.value - 1, days.size) + days.subList(0, it.value - 1)
            })
            HorizontalCalendar(
                state = state,
                modifier = Modifier.weight(1f),
                dayContent = { day ->
                    val kotlinDate = day.date.toKotlinLocalDate()
                    Day(
                        day = day,
                        dayInfo = uiState.calendarDays[kotlinDate],
                        isToday = kotlinDate == today,
                        isEnabled = kotlinDate <= today,
                        onLongClick = { date ->
                            // Only allow logging a past cycle if no cycle is currently ongoing.
                            if (!uiState.isCycleOngoing && date <= today) {
                                selectedPastDate = date
                                showPastCycleDialog = true
                            }
                        },
                        onClick = { date ->
                            navController.navigate(NavRoute.DailyLog.createRoute(date))
                        }
                    )
                }
            )

            Spacer(Modifier.height(16.dp))
            if (uiState.isCycleOngoing) {
                Button(onClick = { viewModel.onEndCurrentCycle() }) {
                    Text("End Current Cycle")
                }
            } else {
                Button(onClick = { viewModel.onStartCycleToday() }) {
                    Text("Start New Cycle Today")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showPastCycleDialog && selectedPastDate != null) {
        // In a production app, this would be a more advanced dialog with two date pickers.
        // For now, this AlertDialog demonstrates the complete workflow.
        AlertDialog(
            onDismissRequest = { showPastCycleDialog = false },
            title = { Text("Log a Past Cycle") },
            text = { Text("Set this date as the start of a new, completed cycle?\n\nStart: $selectedPastDate\n(End date will be set 4 days later for this demo).") },
            confirmButton = {
                Button(onClick = {
                    val startDate = selectedPastDate!!
                    val endDate = startDate.plus(4, DateTimeUnit.DAY) // Simulate a 5-day period
                    viewModel.onLogPastCycle(startDate, endDate)
                    showPastCycleDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showPastCycleDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    dayInfo: CalendarDayInfo?,
    isToday: Boolean,
    isEnabled: Boolean,
    onLongClick: (LocalDate) -> Unit,
    onClick: (LocalDate) -> Unit
) {
    val alpha = if (isEnabled) 1f else 0.5f
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color = if (dayInfo?.isPeriodDay == true) MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha) else Color.Transparent)
            .combinedClickable(
                enabled = day.position == DayPosition.MonthDate && isEnabled,
                onClick = { onClick(day.date.toKotlinLocalDate()) },
                onLongClick = { onLongClick(day.date.toKotlinLocalDate()) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val textColor = if (day.position == DayPosition.MonthDate) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }
            Text(
                text = day.date.dayOfMonth.toString(),
                color = textColor.copy(alpha = alpha),
                style = if (isToday) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
            )
            if (dayInfo?.hasSymptoms == true && !dayInfo.isPeriodDay) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = alpha))
                )
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
