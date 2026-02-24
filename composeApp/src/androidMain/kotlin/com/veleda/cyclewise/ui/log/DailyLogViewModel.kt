package com.veleda.cyclewise.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.domain.models.PeriodLog
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.domain.models.WaterIntake
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.domain.providers.EducationalContentProvider
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.domain.usecases.GetOrCreateDailyLogUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * UI state for the daily log editing screen.
 *
 * @property isLoading          True during the initial data fetch.
 * @property log                The [FullDailyLog] being edited, or null if load failed.
 * @property error              Error message to display (e.g., no parent period found).
 * @property symptomLibrary     Current symptom library for the toggle list.
 * @property medicationLibrary  Current medication library for the toggle list.
 * @property isPeriodDay           Whether the date being edited falls within a period.
 * @property waterCups             Water intake count for this date.
 * @property educationalArticles   Articles to display in the educational bottom sheet, or null when the sheet is hidden.
 */
data class DailyLogUiState(
    val isLoading: Boolean = true,
    val log: FullDailyLog? = null,
    val error: String? = null,
    val symptomLibrary: List<Symptom> = emptyList(),
    val medicationLibrary: List<Medication> = emptyList(),
    val isPeriodDay: Boolean = false,
    val waterCups: Int = 0,
    val educationalArticles: List<EducationalArticle>? = null,
)

/**
 * Daily log entry editor ViewModel with auto-save and period toggle support.
 *
 * Uses a pure-reducer MVI pattern: [onEvent] dispatches to [reduce] (a pure function
 * that returns updated state without side effects), then launches async operations
 * (auto-save, library creation, period toggle, water persistence) in `viewModelScope`.
 *
 * **Auto-save:** Every user interaction persists immediately via [autoSave]. The
 * [NoteChanged] event is debounced by [NOTE_DEBOUNCE_MS] to avoid excessive writes
 * during typing.
 *
 * **Period toggle:** The ViewModel self-determines [DailyLogUiState.isPeriodDay] by
 * querying the repository during init. The [DailyLogEvent.PeriodToggled] event calls
 * [PeriodRepository.logPeriodDay] or [PeriodRepository.unLogPeriodDay] — the same
 * repository methods used by the Tracker's long-press mark/unmark, ensuring consistent
 * period-splitting and merging behavior across both screens.
 *
 * **Two-phase init:**
 * 1. Fetches the initial log, symptom library, medication library, water intake, and
 *    determines isPeriodDay from existing periods.
 * 2. Subscribes to library changes for live updates during editing.
 *
 * Session-scoped (destroyed on logout/autolock).
 */
class DailyLogViewModel(
    private val entryDate: LocalDate,
    private val periodRepository: PeriodRepository,
    private val getOrCreateDailyLog: GetOrCreateDailyLogUseCase,
    private val symptomLibraryProvider: SymptomLibraryProvider,
    private val medicationLibraryProvider: MedicationLibraryProvider,
    private val educationalContentProvider: EducationalContentProvider,
) : ViewModel()
{
    private val _uiState = MutableStateFlow(DailyLogUiState())
    val uiState: StateFlow<DailyLogUiState> = _uiState.asStateFlow()

    /** Active debounce job for [DailyLogEvent.NoteChanged] auto-save. */
    private var noteDebounceJob: Job? = null

    init {
        // 1. Initial data load dispatches a single, comprehensive event when complete.
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val initialSymptoms = symptomLibraryProvider.symptoms.first()
            val initialMedications = medicationLibraryProvider.medications.first()
            val result = getOrCreateDailyLog(entryDate)
            val waterIntake = periodRepository.getWaterIntakeForDates(listOf(entryDate)).firstOrNull()

            // Self-determine isPeriodDay from existing periods
            val periods = periodRepository.getAllPeriods().first()
            val isPeriodDay = periods.any { period ->
                val end = period.endDate
                entryDate >= period.startDate && (end == null || entryDate <= end)
            }

            onEvent(DailyLogEvent.LogLoaded(result, initialSymptoms, initialMedications))
            _uiState.update { it.copy(waterCups = waterIntake?.cups ?: 0, isPeriodDay = isPeriodDay) }
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
     *
     * State is updated synchronously via [reduce], then side effects (auto-save,
     * repository writes, library creation) are launched asynchronously.
     */
    fun onEvent(event: DailyLogEvent) {
        _uiState.update { currentState ->
            reduce(currentState, event)
        }

        // Side effects: repository writes, library creation, water persistence.
        when (event) {
            is DailyLogEvent.CreateAndAddSymptom -> {
                val name = event.name.trim()
                if (name.isBlank()) return
                viewModelScope.launch {
                    val newSymptom = periodRepository.createOrGetSymptomInLibrary(name)
                    val newLogEntry = SymptomLog(
                        id = uuid4().toString(),
                        entryId = _uiState.value.log!!.entry.id,
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
                    autoSave()
                }
            }

            is DailyLogEvent.MedicationCreatedAndAdded -> {
                val name = event.name.trim()
                if (name.isBlank()) return
                viewModelScope.launch {
                    val newMedication = periodRepository.createOrGetMedicationInLibrary(name)
                    val newLogEntry = MedicationLog(
                        id = uuid4().toString(),
                        entryId = _uiState.value.log!!.entry.id,
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
                    autoSave()
                }
            }

            is DailyLogEvent.PeriodToggled -> viewModelScope.launch {
                if (event.isOnPeriod) {
                    periodRepository.logPeriodDay(entryDate)
                } else {
                    periodRepository.unLogPeriodDay(entryDate)
                }
                _uiState.update { it.copy(isPeriodDay = event.isOnPeriod) }
            }

            is DailyLogEvent.WaterIncrement -> viewModelScope.launch {
                periodRepository.upsertWaterIntake(
                    WaterIntake(
                        date = entryDate,
                        cups = _uiState.value.waterCups,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )
                )
            }

            is DailyLogEvent.WaterDecrement -> viewModelScope.launch {
                if (_uiState.value.waterCups > 0) {
                    periodRepository.upsertWaterIntake(
                        WaterIntake(
                            date = entryDate,
                            cups = _uiState.value.waterCups,
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now()
                        )
                    )
                }
            }

            // Auto-save after state update for data-changing user events.
            is DailyLogEvent.MoodScoreChanged,
            is DailyLogEvent.EnergyLevelChanged,
            is DailyLogEvent.LibidoScoreChanged,
            is DailyLogEvent.FlowIntensityChanged,
            is DailyLogEvent.PeriodColorChanged,
            is DailyLogEvent.PeriodConsistencyChanged,
            is DailyLogEvent.SymptomToggled,
            is DailyLogEvent.MedicationToggled,
            is DailyLogEvent.TagAdded,
            is DailyLogEvent.TagRemoved -> autoSave()

            is DailyLogEvent.NoteChanged -> debouncedAutoSave()

            is DailyLogEvent.ShowEducationalSheet -> {
                val articles = educationalContentProvider.getByTag(event.contentTag)
                _uiState.update { it.copy(educationalArticles = articles.ifEmpty { null }) }
            }

            // No side effects for load/library/symptomName/dismiss events.
            is DailyLogEvent.LogLoaded,
            is DailyLogEvent.LibraryUpdated,
            is DailyLogEvent.SymptomNameChanged,
            is DailyLogEvent.DismissEducationalSheet -> { /* state-only */ }
        }
    }

    /**
     * Pure function that returns the new [DailyLogUiState] for a given event.
     *
     * Contains no side effects — all repository writes, library creation, and water
     * persistence are handled in [onEvent] after the state has been updated.
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
                val newPeriodLog = if (event.intensity != null) {
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
                    null
                }
                currentState.copy(log = log.copy(periodLog = newPeriodLog))
            }
            is DailyLogEvent.MoodScoreChanged -> {
                val updatedEntry = log.entry.copy(moodScore = event.score)
                currentState.copy(log = log.copy(entry = updatedEntry))
            }
            is DailyLogEvent.EnergyLevelChanged -> {
                val updatedEntry = log.entry.copy(energyLevel = event.level)
                currentState.copy(log = log.copy(entry = updatedEntry))
            }
            is DailyLogEvent.LibidoScoreChanged -> {
                val updatedEntry = log.entry.copy(libidoScore = event.score)
                currentState.copy(log = log.copy(entry = updatedEntry))
            }
            is DailyLogEvent.PeriodColorChanged -> {
                val updatedPeriodLog = log.periodLog?.copy(periodColor = event.color)
                    ?: return currentState
                currentState.copy(log = log.copy(periodLog = updatedPeriodLog))
            }
            is DailyLogEvent.PeriodConsistencyChanged -> {
                val updatedPeriodLog = log.periodLog?.copy(periodConsistency = event.consistency)
                    ?: return currentState
                currentState.copy(log = log.copy(periodLog = updatedPeriodLog))
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
            is DailyLogEvent.CreateAndAddSymptom -> currentState
            is DailyLogEvent.MedicationToggled -> {
                val existingLog = log.medicationLogs.find { it.medicationId == event.medication.id }
                val newLogs = if (existingLog != null) {
                    log.medicationLogs.filterNot { it.id == existingLog.id }
                } else {
                    log.medicationLogs + MedicationLog(uuid4().toString(), log.entry.id, event.medication.id, Clock.System.now())
                }
                currentState.copy(log = log.copy(medicationLogs = newLogs))
            }
            is DailyLogEvent.MedicationCreatedAndAdded -> currentState
            is DailyLogEvent.PeriodToggled -> currentState
            is DailyLogEvent.WaterIncrement -> {
                currentState.copy(waterCups = currentState.waterCups + 1)
            }
            is DailyLogEvent.WaterDecrement -> {
                if (currentState.waterCups <= 0) return currentState
                currentState.copy(waterCups = currentState.waterCups - 1)
            }
            is DailyLogEvent.DismissEducationalSheet -> currentState.copy(educationalArticles = null)
            is DailyLogEvent.ShowEducationalSheet -> currentState
            is DailyLogEvent.LogLoaded, is DailyLogEvent.LibraryUpdated, is DailyLogEvent.SymptomNameChanged -> currentState
        }
    }

    /**
     * Persists the current log state to the repository if the log contains any user data.
     *
     * Called after every state-changing user interaction (except water, which has its own
     * persistence path, and notes, which use [debouncedAutoSave]).
     */
    private fun autoSave() {
        viewModelScope.launch {
            val currentLog = _uiState.value.log ?: return@launch
            if (!isLogEmpty(currentLog)) {
                periodRepository.saveFullLog(currentLog)
            }
        }
    }

    /**
     * Debounced variant of [autoSave] for [DailyLogEvent.NoteChanged].
     *
     * Cancels any pending save and schedules a new one after [NOTE_DEBOUNCE_MS].
     * This avoids excessive repository writes on every keystroke.
     */
    private fun debouncedAutoSave() {
        noteDebounceJob?.cancel()
        noteDebounceJob = viewModelScope.launch {
            delay(NOTE_DEBOUNCE_MS)
            autoSave()
        }
    }

    /**
     * Determines if a [FullDailyLog] contains no user-entered data.
     */
    private fun isLogEmpty(log: FullDailyLog): Boolean {
        val entry = log.entry
        return log.periodLog == null &&
                log.symptomLogs.isEmpty() &&
                log.medicationLogs.isEmpty() &&
                entry.moodScore == null &&
                entry.energyLevel == null &&
                entry.libidoScore == null &&
                entry.customTags.isEmpty() &&
                entry.note.isNullOrBlank()
    }

    companion object {
        /** Debounce delay for note auto-save in milliseconds. */
        const val NOTE_DEBOUNCE_MS = 500L
    }
}
