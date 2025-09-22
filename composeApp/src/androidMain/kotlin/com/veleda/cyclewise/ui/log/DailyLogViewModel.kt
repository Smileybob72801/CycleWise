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
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.CycleRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// Represents the state of the UI, holding the FullDailyLog
data class DailyLogUiState(
    val isLoading: Boolean = true,
    val log: FullDailyLog? = null,
    val error: String? = null,
    val symptomLibrary: List<Symptom> = emptyList(),
    val medicationLibrary: List<Medication> = emptyList(),
)

class DailyLogViewModel(
    private val entryDate: LocalDate,
    private val cycleRepository: CycleRepository,
    private val symptomLibraryProvider: SymptomLibraryProvider,
    private val medicationLibraryProvider: MedicationLibraryProvider
) : ViewModel()
{
    private val _uiState = MutableStateFlow(DailyLogUiState())
    val uiState = _uiState.asStateFlow()

    private val _saveCompleteEvent = MutableSharedFlow<Unit>(replay = 0)
    val saveCompleteEvent: SharedFlow<Unit> = _saveCompleteEvent

    init {
        // 1. Initial data load dispatches a single, comprehensive event when complete.
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val initialSymptoms = symptomLibraryProvider.symptoms.first()
            val initialMedications = medicationLibraryProvider.medications.first()
            val result = cycleRepository.getFullLogForDate(entryDate)
                ?: createNewBlankLog()

            onEvent(DailyLogEvent.LogLoaded(result, initialSymptoms, initialMedications))
        }

        // 2. Subsequent library changes also dispatch events instead of mutating state directly.
        symptomLibraryProvider.symptoms
            .onEach { symptoms -> onEvent(DailyLogEvent.LibraryUpdated(symptoms, _uiState.value.medicationLibrary)) }
            .launchIn(viewModelScope)

        medicationLibraryProvider.medications
            .onEach { medications -> onEvent(DailyLogEvent.LibraryUpdated(_uiState.value.symptomLibrary, medications)) }
            .launchIn(viewModelScope)
    }

    /**
     * The single, public entry point for all UI actions.
     */
    fun onEvent(event: DailyLogEvent) {
        _uiState.update { currentState ->
            reduce(currentState, event)
        }
    }

    /**
     * A pure function that takes the current state and an event, and returns the new state.
     * All state modification logic is centralized here.
     */
    private fun reduce(currentState: DailyLogUiState, event: DailyLogEvent): DailyLogUiState {
        // The log must exist for almost all events.
        val log = currentState.log
            ?: // Return early if the log isn't loaded yet, unless the event is LogLoaded or LibraryUpdated
            return when (event) {
                is DailyLogEvent.LogLoaded -> currentState.copy(
                    isLoading = false,
                    log = event.log,
                    symptomLibrary = event.initialSymptoms,
                    medicationLibrary = event.initialMedications,
                    error = if (event.log == null) "Could not find a parent cycle for this date." else null
                )
                is DailyLogEvent.LibraryUpdated -> currentState.copy(
                    symptomLibrary = event.symptoms,
                    medicationLibrary = event.medications
                )
                else -> currentState
            }

        return when (event) {
            is DailyLogEvent.FlowIntensityChanged -> {
                val updatedEntry = log.entry.copy(flowIntensity = event.intensity)
                currentState.copy(log = log.copy(entry = updatedEntry))
            }
            is DailyLogEvent.MoodScoreChanged -> {
                val updatedEntry = log.entry.copy(moodScore = event.score)
                currentState.copy(log = log.copy(entry = updatedEntry))
            }
            is DailyLogEvent.NoteChanged -> {
                val updatedEntry = log.entry.copy(note = event.text)
                currentState.copy(log = log.copy(entry = updatedEntry))
            }
            is DailyLogEvent.TagAdded -> {
                if (event.tag.isBlank() || log.entry.customTags.contains(event.tag.trim())) {
                    return currentState
                }
                val newTags = log.entry.customTags + event.tag.trim()
                val newEntry = log.entry.copy(customTags = newTags)
                currentState.copy(log = log.copy(entry = newEntry))
            }
            is DailyLogEvent.TagRemoved -> {
                val newTags = log.entry.customTags.filterNot { it == event.tag }
                val newEntry = log.entry.copy(customTags = newTags)
                currentState.copy(log = log.copy(entry = newEntry))
            }
            is DailyLogEvent.SymptomToggled -> {
                val existingLog = log.symptomLogs.find { it.symptomId == event.symptom.id }
                val newLogs = if (existingLog != null) {
                    log.symptomLogs.filterNot { it.id == existingLog.id }
                } else {
                    log.symptomLogs + SymptomLog(uuid4().toString(), log.entry.id, event.symptom.id, 3, Clock.System.now())
                }
                currentState.copy(log = log.copy(symptomLogs = newLogs))
            }
            is DailyLogEvent.CreateAndAddSymptom -> {
                val name = event.name.trim()
                if (name.isNotBlank()) {
                    viewModelScope.launch {
                        val newSymptom = cycleRepository.createOrGetSymptomInLibrary(name)
                        // After creation, dispatch another event to add it to the log state.
                        onEvent(DailyLogEvent.SymptomToggled(newSymptom))
                    }
                }
                currentState // Return current state; the launch block will trigger a new update.
            }
            is DailyLogEvent.MedicationToggled -> {
                val existingLog = log.medicationLogs.find { it.medicationId == event.medication.id }
                val newLogs = if (existingLog != null) {
                    log.medicationLogs.filterNot { it.id == existingLog.id }
                } else {
                    log.medicationLogs + MedicationLog(uuid4().toString(), log.entry.id, event.medication.id, Clock.System.now())
                }
                currentState.copy(log = log.copy(medicationLogs = newLogs))
            }
            is DailyLogEvent.MedicationCreatedAndAdded -> {
                val name = event.name.trim()
                if (name.isNotBlank()) {
                    viewModelScope.launch {
                        val newMedication = cycleRepository.createOrGetMedicationInLibrary(name)
                        // After creation, dispatch another event to add it to the log state.
                        onEvent(DailyLogEvent.MedicationToggled(newMedication))
                    }
                }
                currentState // Return current state; the launch block will trigger a new update.
            }
            is DailyLogEvent.SaveLog -> {
                viewModelScope.launch {
                    cycleRepository.saveFullLog(log)
                    _saveCompleteEvent.emit(Unit)
                }
                currentState // No immediate state change, just side-effect.
            }
            // These events are handled at the top of the function.
            is DailyLogEvent.LogLoaded, is DailyLogEvent.LibraryUpdated, is DailyLogEvent.SymptomNameChanged -> currentState
        }
    }

    private suspend fun createNewBlankLog(): FullDailyLog? {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val parentCycle = cycleRepository.getAllCycles().first().find { entryDate in (it.startDate..(it.endDate ?: today)) }
        return parentCycle?.let {
            val dayInCycle = it.startDate.daysUntil(entryDate) + 1
            val newBlankEntry = DailyEntry(
                id = uuid4().toString(),
                cycleId = it.id,
                entryDate = entryDate,
                dayInCycle = dayInCycle,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            FullDailyLog(entry = newBlankEntry)
        }
    }
}