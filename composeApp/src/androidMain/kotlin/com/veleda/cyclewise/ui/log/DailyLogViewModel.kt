package com.veleda.cyclewise.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.domain.models.PeriodLog
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
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
    val isPeriodDay: Boolean = false
)

class DailyLogViewModel(
    private val entryDate: LocalDate,
    private val periodRepository: PeriodRepository,
    private val symptomLibraryProvider: SymptomLibraryProvider,
    private val medicationLibraryProvider: MedicationLibraryProvider,
    private val isPeriodDay: Boolean
) : ViewModel()
{
    private val _uiState = MutableStateFlow(DailyLogUiState(isPeriodDay = isPeriodDay))
    val uiState = _uiState.asStateFlow()

    private val _saveCompleteEvent = MutableSharedFlow<Unit>(replay = 0)
    val saveCompleteEvent: SharedFlow<Unit> = _saveCompleteEvent

    init {
        // 1. Initial data load dispatches a single, comprehensive event when complete.
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val initialSymptoms = symptomLibraryProvider.symptoms.first()
            val initialMedications = medicationLibraryProvider.medications.first()
            val result = periodRepository.getFullLogForDate(entryDate)
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
                    isPeriodDay = isPeriodDay,
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
                val newPeriodLog = if (event.intensity != null) {
                    // Create/Update the PeriodLog
                    val existingLog = log.periodLog
                    val now = Clock.System.now()
                    existingLog?.copy(flowIntensity = event.intensity, updatedAt = now)
                        ?: PeriodLog(
                            id = uuid4().toString(),
                            entryId = log.entry.id,
                            flowIntensity = event.intensity,
                            createdAt = now,
                            updatedAt = now
                        )
                } else {
                    // Remove PeriodLog
                    null
                }
                currentState.copy(log = log.copy(periodLog = newPeriodLog))
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
                if (name.isBlank()) {
                    return currentState
                }
                viewModelScope.launch {
                    val newSymptom = periodRepository.createOrGetSymptomInLibrary(name)
                    val newLogEntry = SymptomLog(
                        id = uuid4().toString(),
                        entryId = currentState.log.entry.id,
                        symptomId = newSymptom.id,
                        severity = 3,
                        createdAt = Clock.System.now()
                    )
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
                currentState
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
                if (name.isBlank()) {
                    return currentState
                }
                viewModelScope.launch {
                    val newMedication = periodRepository.createOrGetMedicationInLibrary(name)
                    val newLogEntry = MedicationLog(
                        id = uuid4().toString(),
                        entryId = currentState.log.entry.id,
                        medicationId = newMedication.id,
                        createdAt = Clock.System.now()
                    )
                    _uiState.update {
                        val updatedLogs = it.log!!.medicationLogs + newLogEntry
                        val updatedLibrary = if (it.medicationLibrary.any { m -> m.id == newMedication.id }) {
                            it.medicationLibrary
                        } else {
                            it.medicationLibrary + newMedication
                        }
                        it.copy(
                            log = it.log.copy(medicationLogs = updatedLogs),
                            medicationLibrary = updatedLibrary
                        )
                    }
                }
                currentState
            }
            is DailyLogEvent.SaveLog -> {
                viewModelScope.launch {
                    if (isLogEmpty(log)) {
                        // If log is empty, skip saving and just dismiss the screen
                        _saveCompleteEvent.emit(Unit)
                        return@launch
                    }
                    periodRepository.saveFullLog(log)
                    _saveCompleteEvent.emit(Unit)
                }
                currentState
            }
            is DailyLogEvent.LogLoaded, is DailyLogEvent.LibraryUpdated, is DailyLogEvent.SymptomNameChanged -> currentState
        }
    }

    /**
     * Determines if a FullDailyLog contains no user-entered data.
     */
    private fun isLogEmpty(log: FullDailyLog): Boolean {
        val entry = log.entry
        return log.periodLog == null &&
                log.symptomLogs.isEmpty() &&
                log.medicationLogs.isEmpty() &&
                entry.moodScore == null &&
                entry.energyLevel == null &&
                entry.libidoLevel == null &&
                entry.customTags.isEmpty() &&
                entry.note.isNullOrBlank()
    }

    private suspend fun createNewBlankLog(): FullDailyLog {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // 1: Find the *last completed* period to calculate the cycle day accurately.
        // We look for the Period whose start date is closest to, but before, the entryDate.
        // The repository returns periods newest-first.
        val periods = periodRepository.getAllPeriods().first()
        val logicalParentPeriod = periods
            .filter { it.startDate <= entryDate }
            .maxByOrNull { it.startDate } // Find the chronologically latest period whose start is before or on today.

        // 2: Calculate dayInCycle or default
        val dayInCycleValue = if (logicalParentPeriod != null) {
            // Day 1 of cycle is the start of the logical parent period.
            logicalParentPeriod.startDate.daysUntil(entryDate) + 1
        } else {
            // No historical cycle found. Use 1 as a sentinel value for the very first log.
            1
        }

        val newBlankEntry = DailyEntry(
            id = uuid4().toString(),
            entryDate = entryDate,
            dayInCycle = dayInCycleValue, // Now guaranteed to be an Int
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        // PeriodLog is initialized as null.
        return FullDailyLog(entry = newBlankEntry, periodLog = null)
    }
}