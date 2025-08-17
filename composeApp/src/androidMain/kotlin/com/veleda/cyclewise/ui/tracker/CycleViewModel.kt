package com.veleda.cyclewise.ui.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.repository.CycleRepository
import com.veleda.cyclewise.domain.usecases.EndCycleUseCase
import com.veleda.cyclewise.domain.usecases.StartNewCycleUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.todayIn
import kotlin.time.Clock

// The new UI state for the TrackerScreen
data class TrackerUiState(
    val calendarDays: Map<LocalDate, CalendarDayInfo> = emptyMap()
)

class CycleViewModel(
    private val cycleRepository: CycleRepository,
    private val startNewCycleUseCase: StartNewCycleUseCase,
    private val endCycleUseCase: EndCycleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState = _uiState.asStateFlow()

    // Keep track of which months we've already loaded to avoid redundant fetches
    private val loadedMonths = mutableSetOf<YearMonth>()

    fun loadDataForMonth(yearMonth: YearMonth) {
        // Only fetch if we haven't loaded this month before
        if (loadedMonths.contains(yearMonth)) return

        loadedMonths.add(yearMonth)

        viewModelScope.launch {
            val logs = cycleRepository.getLogsForMonth(yearMonth)

            val newCalendarDays = logs.associate { log ->
                val date = log.entry.entryDate
                val info = CalendarDayInfo(
                    isPeriodDay = log.entry.flowIntensity != null,
                    hasSymptoms = log.symptoms.isNotEmpty()
                )
                date to info
            }

            _uiState.update { currentState ->
                // Add the new days to the existing map
                currentState.copy(
                    calendarDays = currentState.calendarDays + newCalendarDays
                )
            }
        }
    }

    fun onStartNewCycleClicked() {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            // Check if there is already an entry for today. If so, do nothing.
            // This prevents creating duplicate cycles on the same day.
            if (_uiState.value.calendarDays[today]?.isPeriodDay == true) {
                return@launch
            }

            // --- The Instant Feedback Logic ---
            // 1. Immediately update the local UI state.
            _uiState.update { currentState ->
                val newDayInfo = currentState.calendarDays[today]?.copy(isPeriodDay = true)
                    ?: CalendarDayInfo(isPeriodDay = true)

                currentState.copy(
                    calendarDays = currentState.calendarDays + (today to newDayInfo)
                )
            }

            // 2. Then, tell the repository to save the change in the background.
            //    The UI is already updated, so this feels instant to the user.
            startNewCycleUseCase(today)
        }
    }

    fun onEndCycleClicked() {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            endCycleUseCase(endDate = today)
            // Refresh the current month's data to show the change
            loadDataForMonth(YearMonth(today.year, today.month))
        }
    }
}