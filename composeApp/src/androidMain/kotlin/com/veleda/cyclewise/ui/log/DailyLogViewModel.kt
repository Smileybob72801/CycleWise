package com.veleda.cyclewise.ui.log

import android.util.Log
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
import com.veleda.cyclewise.domain.usecases.DeleteMedicationUseCase
import com.veleda.cyclewise.domain.usecases.DeleteSymptomUseCase
import com.veleda.cyclewise.domain.usecases.GetOrCreateDailyLogUseCase
import com.veleda.cyclewise.domain.usecases.RenameMedicationUseCase
import com.veleda.cyclewise.domain.usecases.RenameResult
import com.veleda.cyclewise.domain.usecases.RenameSymptomUseCase
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
 * @property errorMessage              Transient error for Snackbar display (e.g. failed library save). Cleared by [DailyLogEvent.ErrorDismissed].
 * @property symptomForContextMenu     Symptom whose context menu (Rename/Delete) is shown, or null.
 * @property symptomRenaming           Symptom currently being renamed (rename dialog open), or null.
 * @property symptomToDelete           Symptom pending deletion confirmation, or null.
 * @property symptomDeleteLogCount     Number of logs referencing the symptom pending deletion (shown in warning).
 * @property medicationForContextMenu  Medication whose context menu is shown, or null.
 * @property medicationRenaming        Medication currently being renamed, or null.
 * @property medicationToDelete        Medication pending deletion confirmation, or null.
 * @property medicationDeleteLogCount  Number of logs referencing the medication pending deletion.
 * @property renameError               Inline validation error for the active rename dialog, or null.
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
    val errorMessage: String? = null,
    val symptomForContextMenu: Symptom? = null,
    val symptomRenaming: Symptom? = null,
    val symptomToDelete: Symptom? = null,
    val symptomDeleteLogCount: Int = 0,
    val medicationForContextMenu: Medication? = null,
    val medicationRenaming: Medication? = null,
    val medicationToDelete: Medication? = null,
    val medicationDeleteLogCount: Int = 0,
    val renameError: String? = null,
    val showUnmarkPeriodConfirmation: Boolean = false,
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
 * querying the repository during init. The [DailyLogEvent.PeriodToggled] event
 * creates or deletes the [PeriodLog] in the reducer, then calls
 * [PeriodRepository.logPeriodDay] or [PeriodRepository.unLogPeriodDay] as a side
 * effect — the same repository methods used by the Tracker's long-press mark/unmark,
 * ensuring consistent period-splitting and merging behavior across both screens.
 * [DailyLogEvent.FlowIntensityChanged] only updates the flow field on an existing
 * PeriodLog — it no longer creates or deletes one.
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
    private val renameSymptomUseCase: RenameSymptomUseCase,
    private val deleteSymptomUseCase: DeleteSymptomUseCase,
    private val renameMedicationUseCase: RenameMedicationUseCase,
    private val deleteMedicationUseCase: DeleteMedicationUseCase,
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

            // Backfill a PeriodLog if the day is a period day but no PeriodLog exists
            // (covers tracker-marked days that were never opened in the daily log editor).
            val loadedLog = if (isPeriodDay && result.periodLog == null) {
                val now = Clock.System.now()
                val backfilledPeriodLog = PeriodLog(
                    id = uuid4().toString(),
                    entryId = result.entry.id,
                    createdAt = now,
                    updatedAt = now
                )
                result.copy(periodLog = backfilledPeriodLog)
            } else {
                result
            }

            onEvent(DailyLogEvent.LogLoaded(loadedLog, initialSymptoms, initialMedications))
            _uiState.update { it.copy(waterCups = waterIntake?.cups ?: 0, isPeriodDay = isPeriodDay) }

            // Persist the backfilled PeriodLog immediately.
            if (isPeriodDay && result.periodLog == null) {
                autoSave()
            }
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
     *
     * [DailyLogEvent.CreateAndAddSymptom] and [DailyLogEvent.MedicationCreatedAndAdded]
     * wrap their repository calls in try-catch so that DB failures surface as a
     * transient [DailyLogUiState.errorMessage] instead of propagating silently.
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
                    try {
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
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to create symptom '$name'", e)
                        _uiState.update { it.copy(errorMessage = "Failed to save symptom. Please try again.") }
                    }
                }
            }

            is DailyLogEvent.MedicationCreatedAndAdded -> {
                val name = event.name.trim()
                if (name.isBlank()) return
                viewModelScope.launch {
                    try {
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
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to create medication '$name'", e)
                        _uiState.update { it.copy(errorMessage = "Failed to save medication. Please try again.") }
                    }
                }
            }

            is DailyLogEvent.PeriodToggled -> {
                if (event.isOnPeriod) {
                    viewModelScope.launch {
                        periodRepository.logPeriodDay(entryDate)
                        _uiState.update { it.copy(isPeriodDay = true) }
                    }
                    autoSave()
                } else {
                    // If the period log has data, show confirmation instead of proceeding
                    val hasData = _uiState.value.log?.periodLog?.hasData() == true
                    if (!hasData) {
                        viewModelScope.launch {
                            periodRepository.unLogPeriodDay(entryDate)
                            _uiState.update { it.copy(isPeriodDay = false) }
                        }
                        autoSave()
                    }
                    // If hasData, the reduce function already set showUnmarkPeriodConfirmation = true
                }
            }

            is DailyLogEvent.UnmarkPeriodConfirmed -> {
                viewModelScope.launch {
                    periodRepository.unLogPeriodDay(entryDate)
                    _uiState.update { it.copy(isPeriodDay = false) }
                }
                autoSave()
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

            // ── Symptom library edit/delete side effects ──────────────
            is DailyLogEvent.RenameSymptomConfirmed -> {
                viewModelScope.launch {
                    val result = renameSymptomUseCase(
                        event.symptomId,
                        event.newName,
                        _uiState.value.symptomLibrary,
                    )
                    _uiState.update {
                        when (result) {
                            is RenameResult.Success -> it.copy(
                                symptomRenaming = null,
                                renameError = null,
                            )
                            is RenameResult.BlankName -> it.copy(
                                renameError = "Name cannot be blank",
                            )
                            is RenameResult.NameAlreadyExists -> it.copy(
                                renameError = "A symptom with this name already exists",
                            )
                        }
                    }
                }
            }

            is DailyLogEvent.DeleteSymptomClicked -> {
                viewModelScope.launch {
                    val count = deleteSymptomUseCase.getLogCount(event.symptom.id)
                    _uiState.update {
                        it.copy(
                            symptomForContextMenu = null,
                            symptomToDelete = event.symptom,
                            symptomDeleteLogCount = count,
                        )
                    }
                }
            }

            is DailyLogEvent.DeleteSymptomConfirmed -> {
                viewModelScope.launch {
                    deleteSymptomUseCase(event.symptomId)
                    _uiState.update {
                        val filteredLogs = it.log?.let { log ->
                            log.copy(symptomLogs = log.symptomLogs.filterNot { sl -> sl.symptomId == event.symptomId })
                        }
                        it.copy(
                            log = filteredLogs ?: it.log,
                            symptomToDelete = null,
                            symptomDeleteLogCount = 0,
                        )
                    }
                }
            }

            // ── Medication library edit/delete side effects ─────────────
            is DailyLogEvent.RenameMedicationConfirmed -> {
                viewModelScope.launch {
                    val result = renameMedicationUseCase(
                        event.medicationId,
                        event.newName,
                        _uiState.value.medicationLibrary,
                    )
                    _uiState.update {
                        when (result) {
                            is RenameResult.Success -> it.copy(
                                medicationRenaming = null,
                                renameError = null,
                            )
                            is RenameResult.BlankName -> it.copy(
                                renameError = "Name cannot be blank",
                            )
                            is RenameResult.NameAlreadyExists -> it.copy(
                                renameError = "A medication with this name already exists",
                            )
                        }
                    }
                }
            }

            is DailyLogEvent.DeleteMedicationClicked -> {
                viewModelScope.launch {
                    val count = deleteMedicationUseCase.getLogCount(event.medication.id)
                    _uiState.update {
                        it.copy(
                            medicationForContextMenu = null,
                            medicationToDelete = event.medication,
                            medicationDeleteLogCount = count,
                        )
                    }
                }
            }

            is DailyLogEvent.DeleteMedicationConfirmed -> {
                viewModelScope.launch {
                    deleteMedicationUseCase(event.medicationId)
                    _uiState.update {
                        val filteredLogs = it.log?.let { log ->
                            log.copy(medicationLogs = log.medicationLogs.filterNot { ml -> ml.medicationId == event.medicationId })
                        }
                        it.copy(
                            log = filteredLogs ?: it.log,
                            medicationToDelete = null,
                            medicationDeleteLogCount = 0,
                        )
                    }
                }
            }

            // No side effects for state-only events.
            is DailyLogEvent.UnmarkPeriodDismissed,
            is DailyLogEvent.LogLoaded,
            is DailyLogEvent.LibraryUpdated,
            is DailyLogEvent.SymptomNameChanged,
            is DailyLogEvent.DismissEducationalSheet,
            is DailyLogEvent.ErrorDismissed,
            is DailyLogEvent.SymptomLongPressed,
            is DailyLogEvent.RenameSymptomClicked,
            is DailyLogEvent.SymptomEditDismissed,
            is DailyLogEvent.MedicationLongPressed,
            is DailyLogEvent.RenameMedicationClicked,
            is DailyLogEvent.MedicationEditDismissed -> { /* state-only */ }
        }
    }

    /**
     * Pure function that returns the new [DailyLogUiState] for a given event.
     *
     * Contains no side effects — all repository writes, library creation, and water
     * persistence are handled in [onEvent] after the state has been updated.
     *
     * [DailyLogEvent.PeriodToggled] creates or deletes the [PeriodLog] (with null
     * [FlowIntensity]). [DailyLogEvent.FlowIntensityChanged] only updates the flow
     * field on an existing PeriodLog — it does not create or delete one.
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
                    error = null
                )
                is DailyLogEvent.LibraryUpdated -> currentState.copy(
                    symptomLibrary = event.symptoms,
                    medicationLibrary = event.medications
                )
                else -> currentState
            }

        return when (event) {
            is DailyLogEvent.FlowIntensityChanged -> {
                val existingLog = log.periodLog ?: return currentState
                val now = Clock.System.now()
                currentState.copy(
                    log = log.copy(
                        periodLog = existingLog.copy(
                            flowIntensity = event.intensity,
                            updatedAt = now
                        )
                    )
                )
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
            is DailyLogEvent.PeriodToggled -> {
                if (event.isOnPeriod) {
                    val now = Clock.System.now()
                    val newPeriodLog = log.periodLog ?: PeriodLog(
                        id = uuid4().toString(),
                        entryId = log.entry.id,
                        createdAt = now,
                        updatedAt = now
                    )
                    currentState.copy(log = log.copy(periodLog = newPeriodLog))
                } else {
                    if (log.periodLog?.hasData() == true) {
                        currentState.copy(showUnmarkPeriodConfirmation = true)
                    } else {
                        currentState.copy(log = log.copy(periodLog = null))
                    }
                }
            }
            is DailyLogEvent.UnmarkPeriodConfirmed -> {
                currentState.copy(
                    log = log.copy(periodLog = null),
                    showUnmarkPeriodConfirmation = false,
                )
            }
            is DailyLogEvent.UnmarkPeriodDismissed -> {
                currentState.copy(showUnmarkPeriodConfirmation = false)
            }
            is DailyLogEvent.WaterIncrement -> {
                currentState.copy(waterCups = currentState.waterCups + 1)
            }
            is DailyLogEvent.WaterDecrement -> {
                if (currentState.waterCups <= 0) return currentState
                currentState.copy(waterCups = currentState.waterCups - 1)
            }
            is DailyLogEvent.DismissEducationalSheet -> currentState.copy(educationalArticles = null)
            is DailyLogEvent.ErrorDismissed -> currentState.copy(errorMessage = null)
            is DailyLogEvent.ShowEducationalSheet -> currentState

            // ── Symptom library edit/delete reduce ──────────────────
            is DailyLogEvent.SymptomLongPressed -> currentState.copy(
                symptomForContextMenu = event.symptom,
            )
            is DailyLogEvent.RenameSymptomClicked -> currentState.copy(
                symptomForContextMenu = null,
                symptomRenaming = event.symptom,
                renameError = null,
            )
            is DailyLogEvent.SymptomEditDismissed -> currentState.copy(
                symptomForContextMenu = null,
                symptomRenaming = null,
                symptomToDelete = null,
                symptomDeleteLogCount = 0,
                renameError = null,
            )
            // Side-effect-only events — no state change in reduce.
            is DailyLogEvent.RenameSymptomConfirmed,
            is DailyLogEvent.DeleteSymptomClicked,
            is DailyLogEvent.DeleteSymptomConfirmed -> currentState

            // ── Medication library edit/delete reduce ────────────────
            is DailyLogEvent.MedicationLongPressed -> currentState.copy(
                medicationForContextMenu = event.medication,
            )
            is DailyLogEvent.RenameMedicationClicked -> currentState.copy(
                medicationForContextMenu = null,
                medicationRenaming = event.medication,
                renameError = null,
            )
            is DailyLogEvent.MedicationEditDismissed -> currentState.copy(
                medicationForContextMenu = null,
                medicationRenaming = null,
                medicationToDelete = null,
                medicationDeleteLogCount = 0,
                renameError = null,
            )
            // Side-effect-only events — no state change in reduce.
            is DailyLogEvent.RenameMedicationConfirmed,
            is DailyLogEvent.DeleteMedicationClicked,
            is DailyLogEvent.DeleteMedicationConfirmed -> currentState

            is DailyLogEvent.LibraryUpdated -> currentState.copy(
                symptomLibrary = event.symptoms,
                medicationLibrary = event.medications,
            )
            is DailyLogEvent.LogLoaded, is DailyLogEvent.SymptomNameChanged -> currentState
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
        private const val TAG = "DailyLogViewModel"

        /** Debounce delay for note auto-save in milliseconds. */
        const val NOTE_DEBOUNCE_MS = 500L
    }
}
