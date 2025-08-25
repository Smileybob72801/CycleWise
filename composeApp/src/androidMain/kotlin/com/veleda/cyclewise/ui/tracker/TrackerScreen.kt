package com.veleda.cyclewise.ui.tracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.veleda.cyclewise.di.SESSION_SCOPE
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.ui.nav.NavRoute
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(navController: NavController) {
    val viewModel: CycleViewModel = koinViewModel(scope = getKoin().getScope("session"))
    val uiState by viewModel.uiState.collectAsState()
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    val onEditClick: (LocalDate) -> Unit = remember(navController, sheetState, viewModel) {
        { date ->
            // Launch a single coroutine using the captured scope
            coroutineScope.launch {
                sheetState.hide()
                viewModel.onDismissLogSheet()
                navController.navigate(NavRoute.DailyLog.createRoute(date))
            }
        }
    }

    val currentMonth = remember { JavaYearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val calendarState = rememberCalendarState(startMonth, endMonth, currentMonth, firstDayOfWeek)

    // --- BOTTOM SHEET ---
    if (uiState.logForSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissLogSheet() },
            sheetState = sheetState
        ) {
            // Pass the stable lambda we created
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
                modifier = Modifier.weight(1f),
                dayContent = { day ->
                    val date = day.date.toKotlinLocalDate()

                    val cycleForDate = uiState.cycles.find { date in (it.startDate..(it.endDate ?: today)) }

                    val selectionRange = remember(uiState.selectionStartDate, uiState.selectionEndDate) {
                        val start = uiState.selectionStartDate
                        val end = uiState.selectionEndDate
                        if (start != null && end != null) start..end else if (start != null) start..start else null
                    }

                    Day(
                        day = day,
                        isSelected = date == uiState.selectionStartDate || date == uiState.selectionEndDate,
                        isStartDate = cycleForDate?.startDate == date,
                        isEndDate = cycleForDate?.endDate == date,
                        isInExistingRange = cycleForDate != null,
                        isInSelectionRange = selectionRange?.contains(date) ?: false,
                        onClick = { viewModel.onDateClicked(date, cycleForDate) }
                    )
                }
            )

            // --- DYNAMIC ACTION BUTTONS ---
            Spacer(Modifier.height(16.dp))
            AnimatedVisibility(visible = uiState.isSelectingRange) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.clearSelection() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Cancel")
                    }
                    Button(onClick = { viewModel.onSaveSelection() }) {
                        Text("Save Cycle")
                    }
                }
            }
            AnimatedVisibility(visible = !uiState.isSelectingRange && uiState.ongoingCycle != null) {
                Button(onClick = { viewModel.onEndCurrentCycle() }) {
                    Text("End Cycle Today")
                }
            }
            AnimatedVisibility(visible = !uiState.isSelectingRange && uiState.ongoingCycle == null) {
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
            IconButton(onClick = { onEditClick(log.entry.entryDate) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Log")
            }
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        // Flow
        log.entry.flowIntensity?.let {
            InfoRow(icon = Icons.Default.Build, title = "Flow", value = it.name)
        }
        // Mood
        log.entry.moodScore?.let {
            InfoRow(icon = Icons.Outlined.Star, title = "Mood", value = "$it / 5")
        }

        // Symptoms
        if (log.symptomLogs.isNotEmpty()) {
            Text("Symptoms", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(log.symptomLogs) { symptomLog ->
                    // For each log, find the corresponding symptom from the library
                    val symptomInfo = symptomLibrary.find { it.id == symptomLog.symptomId }
                    if (symptomInfo != null) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(symptomInfo.name) } // Display the name
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp)) // Padding at the bottom
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
    isSelected: Boolean,
    isStartDate: Boolean,
    isEndDate: Boolean,
    isInExistingRange: Boolean,
    isInSelectionRange: Boolean,
    onClick: () -> Unit
) {
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
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .background(
                color = if (inRange) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = shape
            )
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = if (day.position == DayPosition.MonthDate) MaterialTheme.colorScheme.onSurface else Color.Gray
        )
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