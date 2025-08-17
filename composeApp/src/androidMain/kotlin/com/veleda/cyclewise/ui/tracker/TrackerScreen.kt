package com.veleda.cyclewise.ui.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlinx.datetime.toKotlinLocalDate
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.compose.getKoin
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.YearMonth as JavaYearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(navController: NavController) {

    val sessionScope = getKoin().getScope("session")

    val viewModel: CycleViewModel = koinViewModel(scope = sessionScope)

    val uiState by viewModel.uiState.collectAsState()

    val currentMonth = remember { JavaYearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    // This is the correct way to handle month scrolls.
    // It triggers whenever the first visible month changes.
    LaunchedEffect(state.firstVisibleMonth) {
        val visibleMonth = state.firstVisibleMonth.yearMonth
        val kotlinYearMonth = YearMonth(visibleMonth.year, visibleMonth.monthValue)
        viewModel.loadDataForMonth(kotlinYearMonth)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.firstVisibleMonth.yearMonth.toString()) },
                actions = {
                    CycleActionsMenu(
                        onStartCycle = { viewModel.onStartNewCycleClicked() },
                        onEndCycle = { viewModel.onEndCycleClicked() }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DaysOfWeekTitle(daysOfWeek = firstDayOfWeek.let {
                val days = JavaDayOfWeek.entries
                days.subList(it.value - 1, days.size) + days.subList(0, it.value - 1)
            })
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    val kotlinDate = day.date.toKotlinLocalDate()
                    Day(
                        day = day,
                        dayInfo = uiState.calendarDays[kotlinDate],
                        isToday = kotlinDate == today,
                        isEnabled = kotlinDate <= today,
                        onClick = { date ->
                            navController.navigate(NavRoute.DailyLog.createRoute(date = date))
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun CycleActionsMenu(
    onStartCycle: () -> Unit,
    onEndCycle: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More Actions")
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Start New Cycle") },
                onClick = {
                    onStartCycle()
                    menuExpanded = false
                },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("End Current Cycle") },
                onClick = {
                    onEndCycle()
                    menuExpanded = false
                },
                leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    dayInfo: CalendarDayInfo?,
    isToday: Boolean,
    isEnabled: Boolean,
    onClick: (LocalDate) -> Unit
) {
    // Determine the alpha for disabled dates
    val alpha = if (isEnabled) 1f else 0.5f

    Box(
        modifier = Modifier
            .aspectRatio(1f) // This is important for square-ish appearance
            .clip(CircleShape)
            .background(color = if (dayInfo?.isPeriodDay == true) MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha) else Color.Transparent)
            .clickable(
                // 3. Use the isEnabled flag to control clickability
                enabled = day.position == DayPosition.MonthDate && isEnabled,
                onClick = { onClick(day.date.toKotlinLocalDate()) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Add a visual indicator for "today"
            val textColor = if (day.position == DayPosition.MonthDate) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }

            Text(
                text = day.date.dayOfMonth.toString(),
                color = textColor.copy(alpha = alpha),
                // Add an underline or different style for today
                style = if (isToday) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
            )

            // Add a dot if there are symptoms
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
