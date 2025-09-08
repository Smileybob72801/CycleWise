package com.veleda.cyclewise.ui.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.CycleRepository
import com.veleda.cyclewise.domain.usecases.EndCycleUseCase
import com.veleda.cyclewise.domain.usecases.GetOrCreateDailyEntryUseCase
import com.veleda.cyclewise.domain.usecases.StartNewCycleUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * UI State for the interactive TrackerScreen.
 */
data class TrackerUiState(
    val cycles: List<Cycle> = emptyList(),
    val selectionStartDate: LocalDate? = null,
    val selectionEndDate: LocalDate? = null,

    // This holds the data for the bottom sheet.
    // When it's not null, the sheet will be visible.
    val logForSheet: FullDailyLog? = null,

    val symptomLibrary: List<Symptom> = emptyList(),
    val medicationLibrary: List<Medication> = emptyList(),
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
    val uiState = _uiState.asStateFlow()
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    init {
        viewModelScope.launch {
            refreshTrigger.flatMapLatest {
                cycleRepository.getAllCycles()
            }.collect { cycles ->
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

        refreshData()
    }

    private fun refreshData() {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    // --- MAIN UI EVENT HANDLER ---
    fun onDateClicked(date: LocalDate, cycleForDate: Cycle?) {

        if (cycleForDate != null) {
            // --- INTENTION B: VIEWING MODE ---
            // If the tapped date is part of an existing cycle, show the bottom sheet.
            showLogSheetForDate(date, cycleForDate)
        } else {
            // --- INTENTION A: CREATION MODE ---
            // Otherwise, handle the tap-tap-confirm selection logic.
            val currentState = _uiState.value
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            if (date > today || currentState.ongoingCycle != null) return

            if (currentState.selectionStartDate == null) {
                _uiState.update { it.copy(selectionStartDate = date) }
            } else if (date < currentState.selectionStartDate) {
                _uiState.update { it.copy(selectionStartDate = date, selectionEndDate = null) }
            } else if (date == currentState.selectionStartDate) {
                clearSelection()
            } else {
                _uiState.update { it.copy(selectionEndDate = date) }
            }
        }
    }

    // --- BOTTOM SHEET ACTIONS ---
    private fun showLogSheetForDate(date: LocalDate, cycleForDate: Cycle) {
        viewModelScope.launch {
            // First, try to fetch an existing log.
            var log = cycleRepository.getFullLogForDate(date)

            if (log == null) {
                // If no log exists, create a new, blank one in memory.
                // We use the cycleForDate that the UI passed to us, which is guaranteed to be correct.
                val dayInCycle = cycleForDate.startDate.daysUntil(date) + 1
                val newBlankEntry = DailyEntry(
                    id = uuid4().toString(),
                    cycleId = cycleForDate.id, // The correct cycle ID!
                    entryDate = date,
                    dayInCycle = dayInCycle,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
                log = FullDailyLog(entry = newBlankEntry)
            }

            // Now, `log` will either be the existing log or our new blank one.
            // Update the state to show the sheet.
            _uiState.update { it.copy(logForSheet = log) }
        }
    }

    fun onDismissLogSheet() {
        _uiState.update { it.copy(logForSheet = null) }
    }

    fun onSaveSelection() {
        val currentState = _uiState.value
        val startDate = currentState.selectionStartDate ?: return
        // If no end date is explicitly selected, the cycle is for a single day.
        val endDate = currentState.selectionEndDate ?: startDate

        viewModelScope.launch {
            if (cycleRepository.isDateRangeAvailable(startDate, endDate)) {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                if (startDate == today) {
                    // If starting today, it's an ongoing cycle.
                    cycleRepository.startNewCycle(startDate)
                } else {
                    // If starting in the past, it's a completed cycle.
                    cycleRepository.createCompletedCycle(startDate, endDate)
                }
                clearSelection()
                refreshData()
            } else {
                println("Error: Cycle overlaps with an existing one.")
                // TODO: Show a user-facing error (e.g., via a Toast or Snackbar).
            }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectionStartDate = null, selectionEndDate = null) }
    }

    fun onEndCurrentCycle() {
        val ongoingCycle = _uiState.value.ongoingCycle ?: return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        viewModelScope.launch {
            cycleRepository.endCycle(ongoingCycle.id, today)
            refreshData()
        }
    }
}