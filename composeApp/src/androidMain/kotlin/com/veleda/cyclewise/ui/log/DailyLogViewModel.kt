package com.veleda.cyclewise.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.repository.CycleRepository
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
    val error: String? = null,
    val medicationLibrary: List<Medication> = emptyList()
)

class DailyLogViewModel(
    private val entryDate: LocalDate,
    private val cycleRepository: CycleRepository
) : ViewModel()
{
    private val _uiState = MutableStateFlow(DailyLogUiState())
    val uiState = _uiState.asStateFlow()

    // A predefined list of common symptoms for the user to select from.
    // TODO: get this from config file or settings.
    val commonSymptoms = listOf(
        "CRAMPS",
        "HEADACHE",
        "FATIGUE",
        "BLOATING",
        "ACNE",
        "ANXIETY",
        "BREAST TENDERNESS",
        "MUSCLE ACHES",
        "INCREASED APPETITE",
        "DECREASED APPETITE",
        "INSOMNIA",
        "MOOD SWINGS",
        "JOINT PAIN",
        "DIARRHEA",
        "CONSTIPATION",
        "BACK PAIN",
    )

    init {
        loadLog()

        viewModelScope.launch {
            cycleRepository.getMedicationLibrary().collect { meds ->
                _uiState.update { it.copy(medicationLibrary = meds) }
            }
        }
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

    /**
     * Toggles a medication's presence in the daily log.
     * If it's already logged, it's removed. If not, it's added.
     */
    fun onToggleMedication(medication: Medication) {
        _uiState.update { state ->
            val currentLog = state.log ?: return@update state
            val existingLog = currentLog.medicationLogs.find { it.medicationId == medication.id }

            val newLogs = if (existingLog != null) {
                // It exists, so remove it.
                currentLog.medicationLogs.filterNot { it.id == existingLog.id }
            } else {
                // It doesn't exist, so add it.
                val newLog = MedicationLog(
                    id = uuid4().toString(),
                    entryId = currentLog.entry.id,
                    medicationId = medication.id,
                    createdAt = Clock.System.now()
                )
                currentLog.medicationLogs + newLog
            }
            state.copy(log = currentLog.copy(medicationLogs = newLogs))
        }
    }

    /**
     * Creates a new medication in the library and immediately adds it to the current day's log.
     */
    fun onCreateAndAddMedication(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            // This repository method is smart: it either creates a new med or returns the existing one.
            val newMed = cycleRepository.createOrGetMedicationInLibrary(name.trim())
            // Now that the med is guaranteed to be in the library, toggle it into the log.
            onToggleMedication(newMed)
        }
    }

    fun onAddTag(tag: String) {
        if (tag.isBlank()) return
        _uiState.update { state ->
            val currentLog = state.log ?: return@update state
            // Prevent duplicate tags
            if (currentLog.entry.customTags.contains(tag.trim())) return@update state

            val newTags = currentLog.entry.customTags + tag.trim()
            val newEntry = currentLog.entry.copy(customTags = newTags)
            state.copy(log = currentLog.copy(entry = newEntry))
        }
    }

    fun onRemoveTag(tag: String) {
        _uiState.update { state ->
            val currentLog = state.log ?: return@update state
            val newTags = currentLog.entry.customTags.filterNot { it == tag }
            val newEntry = currentLog.entry.copy(customTags = newTags)
            state.copy(log = currentLog.copy(entry = newEntry))
        }
    }

    fun onNoteChanged(newText: String) {
        _uiState.update { state ->
            state.copy(log = state.log?.copy(entry = state.log.entry.copy(note = newText)))
        }
    }
}