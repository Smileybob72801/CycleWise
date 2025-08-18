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

/**
 * UI State for the TrackerScreen.
 * @param calendarDays Map of dates to their display information.
 * @param isCycleOngoing Controls the state of the primary action button.
 */
data class TrackerUiState(
    val calendarDays: Map<LocalDate, CalendarDayInfo> = emptyMap(),
    val isCycleOngoing: Boolean = false
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

    init {
        // Check for an ongoing cycle when the ViewModel is created to set the initial button state.
        viewModelScope.launch {
            val ongoingCycle = cycleRepository.getCurrentlyOngoingCycle()
            _uiState.update { it.copy(isCycleOngoing = ongoingCycle != null) }
        }
    }

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

    fun onStartCycleToday() {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val newCycle = startNewCycleUseCase(today)
            if (newCycle != null) {
                // Instantly update the UI state for immediate feedback
                _uiState.update { it.copy(isCycleOngoing = true) }
                // Clear loaded months to force a refresh of the new data
                loadedMonths.clear()
                loadDataForMonth(YearMonth(today.year, today.month))
            }
        }
    }

    fun onEndCurrentCycle() {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val endedCycle = endCycleUseCase(endDate = today)
            if (endedCycle != null) {
                // Instantly update the UI state
                _uiState.update { it.copy(isCycleOngoing = false) }
                // Refresh the calendar
                loadedMonths.clear()
                loadDataForMonth(YearMonth(today.year, today.month))
            }
        }
    }

    fun onLogPastCycle(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            // No need to check for ongoing cycle here, as the UI should prevent this.
            cycleRepository.createCompletedCycle(startDate, endDate)
            // Refresh the calendar for the affected months
            loadedMonths.clear()
            loadDataForMonth(YearMonth(startDate.year, startDate.month))
            if (startDate.month != endDate.month) {
                loadDataForMonth(YearMonth(endDate.year, endDate.month))
            }
        }
    }
}