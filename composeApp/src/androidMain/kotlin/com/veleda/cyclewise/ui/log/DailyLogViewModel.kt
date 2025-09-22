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
        // 1. Initial data load dispatches a single, comprehensive event when complete.
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val initialSymptoms = symptomLibraryProvider.symptoms.first()
            val initialMedications = medicationLibraryProvider.medications.first()
            val result = cycleRepository.getFullLogForDate(entryDate)
                ?: createNewBlankLog(initialSymptoms, initialMedications)

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

    // 3. The SINGLE entry point for all state modifications.
    fun onEvent(event: DailyLogEvent) {
        _uiState.update { currentState ->
            reduce(currentState, event)
        }
    }

    // 4. The REDUCER: A pure function that takes the current state and an event, and returns the new state.
    private fun reduce(currentState: DailyLogUiState, event: DailyLogEvent): DailyLogUiState {
        return when (event) {
            is DailyLogEvent.SaveLog -> {
                currentState.log?.let { logToSave ->
                    viewModelScope.launch {
                        cycleRepository.saveFullLog(logToSave)
                        _saveCompleteEvent.emit(Unit)
                    }
                }
                currentState
            }

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

            is DailyLogEvent.CreateAndAddSymptom -> {
                val name = event.name.trim()
                if (name.isBlank() || currentState.log == null) {
                    return currentState
                }

                // Launch a coroutine to handle the database interaction and subsequent state update.
                viewModelScope.launch {
                    // Step 1: Persist the new symptom to the library, getting back the canonical object.
                    val newSymptom = cycleRepository.createOrGetSymptomInLibrary(name)

                    // Step 2: Create the log entry to mark this symptom as active for today.
                    val newLogEntry = SymptomLog(
                        id = uuid4().toString(),
                        entryId = currentState.log.entry.id,
                        symptomId = newSymptom.id,
                        severity = 3, // A reasonable default severity.
                        createdAt = Clock.System.now()
                    )

                    // Step 3: Perform a single, atomic state update that includes EVERYTHING.
                    _uiState.update {
                        val updatedLogs = it.log!!.symptomLogs + newLogEntry

                        val updatedLibrary = if (it.symptomLibrary.any { s -> s.id == newSymptom.id }) {
                            it.symptomLibrary
                        } else {
                            it.symptomLibrary + newSymptom
                        }

                        it.copy(
                            log = it.log.copy(symptomLogs = updatedLogs),
                            symptomLibrary = updatedLibrary
                        )
                    }
                }
                return currentState
            }

            is DailyLogEvent.SymptomToggled -> {
                val log = currentState.log ?: return currentState
                val existingLog = log.symptomLogs.find { it.symptomId == event.symptom.id }
                val newLogs = if (existingLog != null) {
                    log.symptomLogs.filterNot { it.id == existingLog.id }
                } else {
                    log.symptomLogs + SymptomLog(uuid4().toString(), log.entry.id, event.symptom.id, 3, Clock.System.now())
                }
                currentState.copy(log = log.copy(symptomLogs = newLogs))
            }

            is DailyLogEvent.SaveLog -> {
                currentState.log?.let { logToSave ->
                    viewModelScope.launch { cycleRepository.saveFullLog(logToSave) }
                }
                currentState
            }

            is DailyLogEvent.MedicationToggled -> {
                val log = currentState.log ?: return currentState
                val existingLog = log.medicationLogs.find { it.medicationId == event.medication.id }

                val newLogs = if (existingLog != null) {
                    // It exists, so remove it.
                    log.medicationLogs.filterNot { it.id == existingLog.id }
                } else {
                    // It doesn't exist, so add it.
                    val newLog = MedicationLog(
                        id = uuid4().toString(),
                        entryId = log.entry.id,
                        medicationId = event.medication.id,
                        createdAt = Clock.System.now()
                    )
                    log.medicationLogs + newLog
                }
                currentState.copy(log = log.copy(medicationLogs = newLogs))
            }

            is DailyLogEvent.MedicationCreatedAndAdded -> {
                val name = event.name.trim()
                if (name.isBlank()) return currentState

                viewModelScope.launch {
                    // This suspend call persists the new medication to the database
                    val newMedication = cycleRepository.createOrGetMedicationInLibrary(name)

                    val newLogEntry = MedicationLog(
                        id = uuid4().toString(),
                        entryId = _uiState.value.log!!.entry.id,
                        medicationId = newMedication.id,
                        createdAt = Clock.System.now()
                    )

                    // Atomically update the state with the new log AND the new library item
                    _uiState.update {
                        val updatedLibrary = if (it.medicationLibrary.any { m -> m.id == newMedication.id }) {
                            it.medicationLibrary
                        } else {
                            it.medicationLibrary + newMedication
                        }

                        it.copy(
                            log = it.log?.copy(medicationLogs = it.log.medicationLogs + newLogEntry),
                            medicationLibrary = updatedLibrary
                        )
                    }
                }

                // Immediately return the current state. The launch block will update it again.
                currentState
            }

            // ... handle other events like NoteChanged, MoodScoreChanged, etc.
            else -> currentState
        }
    }

    private suspend fun createNewBlankLog(symptoms: List<Symptom>, medications: List<Medication>): FullDailyLog? {
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

    fun onToggleSymptom(symptom: Symptom, severity: Int = 3) {
        _uiState.update { state ->
            val log = state.log ?: return@update state
            val existingLog = log.symptomLogs.find { it.symptomId == symptom.id }
            val newLogs = if (existingLog != null) {
                log.symptomLogs.filterNot { it.id == existingLog.id }
            } else {
                val newLog = SymptomLog(
                    id = uuid4().toString(),
                    entryId = log.entry.id,
                    symptomId = symptom.id,
                    severity = severity,
                    createdAt = Clock.System.now()
                )
                log.symptomLogs + newLog
            }
            state.copy(log = log.copy(symptomLogs = newLogs))
        }
    }
}