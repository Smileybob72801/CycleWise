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

    private val _selectionState = MutableStateFlow<Pair<LocalDate?, LocalDate?>>(Pair(null, null))
    private val _logForSheetState = MutableStateFlow<FullDailyLog?>(null)

    val uiState: StateFlow<TrackerUiState> = combine(
        cycleRepository.getAllCycles(), // Directly use the repository flow
        symptomLibraryProvider.symptoms,
        medicationLibraryProvider.medications,
        _selectionState,
        _logForSheetState
    ) { cycles, symptoms, medications, selection, logForSheet ->
        TrackerUiState(
            cycles = cycles,
            symptomLibrary = symptoms,
            medicationLibrary = medications,
            selectionStartDate = selection.first,
            selectionEndDate = selection.second,
            logForSheet = logForSheet
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TrackerUiState()
    )

    fun onDateClicked(date: LocalDate, cycleForDate: Cycle?) {
        if (cycleForDate != null) {
            showLogSheetForDate(date, cycleForDate)
        } else {
            val currentState = uiState.value
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            if (date > today || currentState.ongoingCycle != null) return
            val currentSelectionStart = currentState.selectionStartDate
            if (currentSelectionStart == null) {
                _selectionState.value = Pair(date, null)
            } else if (date < currentSelectionStart) {
                _selectionState.value = Pair(date, null)
            } else if (date == currentSelectionStart) {
                clearSelection()
            } else {
                _selectionState.value = Pair(currentSelectionStart, date)
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

            _logForSheetState.value = log
        }
    }

    fun onDismissLogSheet() {
        _logForSheetState.value = null
    }

    fun onSaveSelection() {
        val startDate = uiState.value.selectionStartDate ?: return
        val endDate = uiState.value.selectionEndDate ?: startDate
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
            }
            clearSelection()
        }
    }

    fun clearSelection() {
        _selectionState.value = Pair(null, null)
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