package com.veleda.cyclewise.ui.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class TrackerUiState(
    val periods: List<Period> = emptyList(),
    val selectionStartDate: LocalDate? = null,
    val selectionEndDate: LocalDate? = null,
    val logForSheet: FullDailyLog? = null,
    val symptomLibrary: List<Symptom> = emptyList(),
    val medicationLibrary: List<Medication> = emptyList(),
    val dayDetails: Map<LocalDate, CalendarDayInfo> = emptyMap()
) {
    val ongoingPeriod: Period? = periods.find { it.endDate == null }
    val isSelectingRange: Boolean = selectionStartDate != null
}

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModel(
    private val periodRepository: PeriodRepository,
    private val symptomLibraryProvider: SymptomLibraryProvider,
    private  val medicationLibraryProvider: MedicationLibraryProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            periodRepository.observeDayDetails()
                .map { domainDetailsMap ->
                    domainDetailsMap.mapValues { (_, domainDetails) ->
                        CalendarDayInfo(
                            isPeriodDay = domainDetails.isPeriodDay,
                            hasSymptoms = domainDetails.hasLoggedSymptoms,
                            hasMedications = domainDetails.hasLoggedMedications
                        )
                    }
                }
                .collect { uiReadyDetailsMap ->
                    _uiState.update { it.copy(dayDetails = uiReadyDetailsMap) }
                }
        }

        viewModelScope.launch {
            periodRepository.getAllPeriods().collect { periods ->
                _uiState.update { it.copy(periods = periods) }
            }
        }

        viewModelScope.launch {
            symptomLibraryProvider.symptoms.collect { symptoms ->
                _uiState.update { it.copy(symptomLibrary = symptoms) }
            }
        }

        viewModelScope.launch {
            medicationLibraryProvider.medications.collect { medications ->
                _uiState.update { it.copy(medicationLibrary = medications) }
            }
        }
    }

    fun onEvent(event: TrackerEvent) {
        _uiState.update { currentState ->
            reduce(currentState, event)
        }
    }

    private fun reduce(currentState: TrackerUiState, event: TrackerEvent): TrackerUiState {
        return when (event) {
            is TrackerEvent.DateClicked -> {
                if (event.periodForDate != null) {
                    showLogSheetForDate(event.date, event.periodForDate)
                    return currentState // No immediate state change, handled by side-effect
                }

                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                if (event.date > today || currentState.ongoingPeriod != null) {
                    return currentState // Invalid selection, do nothing
                }

                val currentSelectionStart = currentState.selectionStartDate
                when {
                    currentSelectionStart == null -> currentState.copy(selectionStartDate = event.date, selectionEndDate = null)
                    event.date < currentSelectionStart -> currentState.copy(selectionStartDate = event.date, selectionEndDate = null)
                    event.date == currentSelectionStart -> currentState.copy(selectionStartDate = null, selectionEndDate = null) // Deselect
                    else -> currentState.copy(selectionEndDate = event.date)
                }
            }
            is TrackerEvent.SaveSelectionClicked -> {
                viewModelScope.launch {
                    val startDate = currentState.selectionStartDate ?: return@launch
                    val endDate = currentState.selectionEndDate ?: startDate
                    if (periodRepository.isDateRangeAvailable(startDate, endDate)) {
                        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                        if (startDate == today) {
                            periodRepository.startNewPeriod(startDate)
                        } else {
                            periodRepository.createCompletedPeriod(startDate, endDate)
                        }
                    }
                    onEvent(TrackerEvent.ClearSelectionClicked) // Clear selection after saving
                }
                currentState // Return immediately; the launch block will trigger another event
            }
            is TrackerEvent.EndPeriodClicked -> {
                currentState.ongoingPeriod?.let {
                    viewModelScope.launch {
                        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                        periodRepository.endPeriod(it.id, today)
                    }
                }
                currentState
            }
            is TrackerEvent.ClearSelectionClicked -> {
                currentState.copy(selectionStartDate = null, selectionEndDate = null)
            }
            is TrackerEvent.DismissLogSheet -> {
                currentState.copy(logForSheet = null)
            }
        }
    }

    private fun showLogSheetForDate(date: LocalDate, periodForDate: Period) {
        viewModelScope.launch {
            val log = periodRepository.getFullLogForDate(date) ?: run {
                val dayInCycle = periodForDate.startDate.daysUntil(date) + 1
                val newBlankEntry = DailyEntry(
                    id = uuid4().toString(),
                    entryDate = date,
                    dayInCycle = dayInCycle,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
                FullDailyLog(entry = newBlankEntry)
            }
            _uiState.update { it.copy(logForSheet = log) }
        }
    }
}