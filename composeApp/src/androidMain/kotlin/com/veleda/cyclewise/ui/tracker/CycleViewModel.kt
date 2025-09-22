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

/**
 * UI State for the interactive TrackerScreen.
 */
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
    // We expose it as an immutable, public state flow.
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    init {
        // Collector 1: Handles all calendar day decorations.
        viewModelScope.launch {
            cycleRepository.observeDayDetails()
                .map { domainDetailsMap ->
                    // transform the repository's data into the specific data class for UI
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

        // Collector 2: Handles the list of cycles for business logic (e.g., finding ongoing cycle).
        viewModelScope.launch {
            cycleRepository.getAllCycles().collect { cycles ->
                _uiState.update { it.copy(cycles = cycles) }
            }
        }

        // Collector 3 & 4: Handle library data needed for the summary sheet.
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

    fun onDateClicked(date: LocalDate, cycleForDate: Cycle?) {
        if (cycleForDate != null) {
            showLogSheetForDate(date, cycleForDate)
        } else {
            val currentState = _uiState.value // Read the latest value
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            if (date > today || currentState.ongoingCycle != null) return
            val currentSelectionStart = currentState.selectionStartDate
            if (currentSelectionStart == null) {
                _uiState.update { it.copy(selectionStartDate = date, selectionEndDate = null) }
            } else if (date < currentSelectionStart) {
                _uiState.update { it.copy(selectionStartDate = date, selectionEndDate = null) }
            } else if (date == currentSelectionStart) {
                clearSelection()
            } else {
                _uiState.update { it.copy(selectionEndDate = date) }
            }
        }
    }

    private fun showLogSheetForDate(date: LocalDate, cycleForDate: Cycle) {
        viewModelScope.launch {
            var log = cycleRepository.getFullLogForDate(date)

            if (log == null) {
                // If no log exists, create a new, blank one in memory.
                // We use the cycleForDate that the UI passed to us, which is guaranteed to be correct.
                val dayInCycle = cycleForDate.startDate.daysUntil(date) + 1
                val newBlankEntry = DailyEntry(
                    id = uuid4().toString(),
                    cycleId = cycleForDate.id,
                    entryDate = date,
                    dayInCycle = dayInCycle,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )

                log = FullDailyLog(entry = newBlankEntry)
            }

            _uiState.update { it.copy(logForSheet = log) }
        }
    }

    fun onDismissLogSheet() {
        _uiState.update { it.copy(logForSheet = null) }
    }

    fun onSaveSelection() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val startDate = currentState.selectionStartDate ?: return@launch
            val endDate = currentState.selectionEndDate ?: startDate
            if (cycleRepository.isDateRangeAvailable(startDate, endDate)) {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                if (startDate == today) {
                    // If starting today, it's an ongoing cycle.
                    cycleRepository.startNewCycle(startDate)
                } else {
                    // If starting in the past, it's a completed cycle.
                    cycleRepository.createCompletedCycle(startDate, endDate)
                }
            }
            clearSelection()
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectionStartDate = null, selectionEndDate = null) }
    }

    fun onEndCurrentCycle() {
        val ongoingCycle = uiState.value.ongoingCycle ?: return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        viewModelScope.launch {
            cycleRepository.endCycle(ongoingCycle.id, today)
            // No manual refresh needed!
        }
    }
}