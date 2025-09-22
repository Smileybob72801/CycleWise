package com.veleda.cyclewise.ui.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.CycleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class TrackerUiState(
    val cycles: List<Cycle> = emptyList(),
    val selectionStartDate: LocalDate? = null,
    val selectionEndDate: LocalDate? = null,
    val logForSheet: FullDailyLog? = null,
    val symptomLibrary: List<Symptom> = emptyList(),
    val medicationLibrary: List<Medication> = emptyList(),
    val dayDetails: Map<LocalDate, CalendarDayInfo> = emptyMap()
) {
    val ongoingCycle: Cycle? = cycles.find { it.endDate == null }
    val isSelectingRange: Boolean = selectionStartDate != null
}

@OptIn(ExperimentalCoroutinesApi::class)
class CycleViewModel(
    private val cycleRepository: CycleRepository,
    private val symptomLibraryProvider: SymptomLibraryProvider,
    private  val medicationLibraryProvider: MedicationLibraryProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cycleRepository.observeDayDetails()
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
            cycleRepository.getAllCycles().collect { cycles ->
                _uiState.update { it.copy(cycles = cycles) }
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
                if (event.cycleForDate != null) {
                    showLogSheetForDate(event.date, event.cycleForDate)
                    return currentState // No immediate state change, handled by side-effect
                }

                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                if (event.date > today || currentState.ongoingCycle != null) {
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
                    if (cycleRepository.isDateRangeAvailable(startDate, endDate)) {
                        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                        if (startDate == today) {
                            cycleRepository.startNewCycle(startDate)
                        } else {
                            cycleRepository.createCompletedCycle(startDate, endDate)
                        }
                    }
                    onEvent(TrackerEvent.ClearSelectionClicked) // Clear selection after saving
                }
                currentState // Return immediately; the launch block will trigger another event
            }
            is TrackerEvent.EndCycleClicked -> {
                currentState.ongoingCycle?.let {
                    viewModelScope.launch {
                        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                        cycleRepository.endCycle(it.id, today)
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

    private fun showLogSheetForDate(date: LocalDate, cycleForDate: Cycle) {
        viewModelScope.launch {
            val log = cycleRepository.getFullLogForDate(date) ?: run {
                val dayInCycle = cycleForDate.startDate.daysUntil(date) + 1
                val newBlankEntry = DailyEntry(
                    id = uuid4().toString(),
                    cycleId = cycleForDate.id,
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