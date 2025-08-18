package com.veleda.cyclewise.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.repository.CycleRepository
import com.veleda.cyclewise.domain.usecases.GetOrCreateDailyEntryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Clock

// Represents the state of the UI, holding the FullDailyLog
data class DailyLogUiState(
    val isLoading: Boolean = true,
    val log: FullDailyLog? = null,
    val error: String? = null
)

class DailyLogViewModel(
    private val entryDate: LocalDate,
    private val cycleRepository: CycleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyLogUiState())
    val uiState = _uiState.asStateFlow()

    val commonSymptoms = listOf("CRAMPS", "HEADACHE", "FATIGUE", "BLOATING", "ACNE", "ANXIETY")

    init {
        loadLog()
    }

    private fun loadLog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // First, try to fetch an existing log.
            var result = cycleRepository.getFullLogForDate(entryDate)

            if (result == null) {
                // If no log exists, we must create a new, blank one.
                // To do this, we need to find which cycle this date belongs to.
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val allCycles = cycleRepository.getAllCycles()
                val parentCycle = allCycles.find { entryDate in (it.startDate..(it.endDate ?: today)) }

                if (parentCycle != null) {
                    // We found the parent cycle, so we can create a new blank entry.
                    val dayInCycle = parentCycle.startDate.daysUntil(entryDate) + 1
                    val newBlankEntry = DailyEntry(
                        id = uuid4().toString(),
                        cycleId = parentCycle.id,
                        entryDate = entryDate,
                        dayInCycle = dayInCycle,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )
                    result = FullDailyLog(entry = newBlankEntry)
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    log = result,
                    error = if (result == null) "Could not find a parent cycle for this date." else null
                )
            }
        }
    }

    // --- UI Event Handlers ---

    fun setFlowIntensity(intensity: FlowIntensity?) {
        _uiState.update { state ->
            state.copy(log = state.log?.copy(entry = state.log.entry.copy(flowIntensity = intensity)))
        }
    }

    fun setMoodScore(score: Int) {
        _uiState.update { state ->
            state.copy(log = state.log?.copy(entry = state.log.entry.copy(moodScore = score)))
        }
    }

    fun toggleSymptom(symptomType: String) {
        _uiState.update { state ->
            val currentLog = state.log ?: return@update state
            val currentSymptoms = currentLog.symptoms
            val isSelected = currentSymptoms.any { it.type == symptomType }

            val newSymptoms = if (isSelected) {
                currentSymptoms.filterNot { it.type == symptomType }
            } else {
                currentSymptoms + Symptom(
                    id = uuid4().toString(),
                    entryId = currentLog.entry.id,
                    type = symptomType,
                    severity = 3 // Default severity
                )
            }
            state.copy(log = currentLog.copy(symptoms = newSymptoms))
        }
    }

    fun saveLog() {
        val logToSave = _uiState.value.log ?: return
        viewModelScope.launch {
            // Call the correct, refactored repository method
            cycleRepository.saveFullLog(logToSave)
        }
    }
}