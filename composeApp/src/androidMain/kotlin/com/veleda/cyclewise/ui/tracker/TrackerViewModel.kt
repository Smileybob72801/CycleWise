package com.veleda.cyclewise.ui.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.providers.EducationalContentProvider
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.domain.usecases.AutoCloseOngoingPeriodUseCase
import com.veleda.cyclewise.settings.AppSettings
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.math.roundToInt
import kotlin.time.Clock

/**
 * UI state for the Tracker (calendar) screen.
 *
 * @property periods             All saved periods, used for calendar range highlighting.
 * @property logForSheet         The full daily log to display in the bottom sheet, or null when the sheet is hidden.
 * @property periodIdForSheet    The id of the period that contains the date shown in the bottom sheet, if any.
 * @property symptomLibrary      Complete symptom library for rendering log summaries.
 * @property medicationLibrary   Complete medication library for rendering log summaries.
 * @property dayDetails          Per-date calendar annotations (period, symptoms, medications, notes, phase).
 * @property showDeleteConfirmation Whether the delete-period confirmation dialog is visible.
 * @property periodIdToDelete    The id of the period the user has requested to delete.
 * @property waterCupsForSheet      Water intake cup count for the date shown in the bottom sheet.
 * @property educationalArticles   Articles to display in the educational bottom sheet, or null when the sheet is hidden.
 */
data class TrackerUiState(
    val periods: List<Period> = emptyList(),
    val logForSheet: FullDailyLog? = null,
    val periodIdForSheet: String? = null,
    val symptomLibrary: List<Symptom> = emptyList(),
    val medicationLibrary: List<Medication> = emptyList(),
    val dayDetails: Map<LocalDate, CalendarDayInfo> = emptyMap(),
    val showDeleteConfirmation: Boolean = false,
    val periodIdToDelete: String? = null,
    val waterCupsForSheet: Int? = null,
    val educationalArticles: List<EducationalArticle>? = null,
) {
    val ongoingPeriod: Period? = periods.find { it.endDate == null }
}

/**
 * Calendar/tracker screen ViewModel managing period and log state.
 *
 * Uses a MVI-inspired pattern: [onEvent] applies a pure [reduce] for state updates,
 * then launches side effects (navigation, repository writes) in `viewModelScope`.
 *
 * **Init:** Collects 4 reactive flows — day details, periods, symptom library, and
 * medication library — each updating the corresponding [TrackerUiState] field.
 *
 * **Effects:** One-shot navigation events are emitted via [_effect] ([SharedFlow] with
 * replay = 0, `extraBufferCapacity = 1`). The buffer capacity allows [tryEmit] to
 * succeed even when no collector is active, preventing dropped events or unexpected
 * [CancellationException] propagation from [emit] during scope cancellation.
 *
 * Session-scoped (destroyed on logout/autolock).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModel(
    private val periodRepository: PeriodRepository,
    private val symptomLibraryProvider: SymptomLibraryProvider,
    private val medicationLibraryProvider: MedicationLibraryProvider,
    private val autoClosePeriodUseCase: AutoCloseOngoingPeriodUseCase,
    private val appSettings: AppSettings,
    private val educationalContentProvider: EducationalContentProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<TrackerEffect>(replay = 0, extraBufferCapacity = 1)
    val effect: SharedFlow<TrackerEffect> = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            periodRepository.observeDayDetails()
                .map { domainDetailsMap ->
                    domainDetailsMap.mapValues { (_, domainDetails) ->
                        CalendarDayInfo(
                            isPeriodDay = domainDetails.isPeriodDay,
                            hasSymptoms = domainDetails.hasLoggedSymptoms,
                            hasMedications = domainDetails.hasLoggedMedications,
                            hasNotes = domainDetails.hasNotes,
                            cyclePhase = domainDetails.cyclePhase
                        )
                    }
                }
                .collect { uiReadyDetailsMap ->
                    _uiState.update { it.copy(dayDetails = uiReadyDetailsMap) }
                }
        }

        viewModelScope.launch {
            periodRepository.getAllPeriods().collect { periods ->
                _uiState.update { it.copy(periods = periods) }
                updatePredictionCache(periods)
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
    }

    /**
     * The single public entry point for all UI interactions.
     *
     * Updates state synchronously via [reduce], then launches side effects
     * (repository writes, navigation effects) asynchronously in [viewModelScope].
     */
    fun onEvent(event: TrackerEvent) {
        _uiState.update { currentState ->
            reduce(currentState, event)
        }

        // Side effects: repository calls, navigation, auto-close.
        when (event) {
            is TrackerEvent.ScreenEntered -> viewModelScope.launch {
                autoClosePeriodUseCase()
            }

            is TrackerEvent.DayTapped -> viewModelScope.launch {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val date = event.date
                val periodForDate = _uiState.value.periods.find {
                    date in (it.startDate..(it.endDate ?: today))
                }
                val existingLog = periodRepository.getFullLogForDate(date)

                if (existingLog != null) {
                    val waterIntake = periodRepository.getWaterIntakeForDates(listOf(date)).firstOrNull()
                    _uiState.update {
                        it.copy(
                            logForSheet = existingLog,
                            periodIdForSheet = periodForDate?.id,
                            waterCupsForSheet = waterIntake?.cups
                        )
                    }
                } else {
                    if (!_effect.tryEmit(TrackerEffect.NavigateToDailyLog(date))) {
                        Log.w(TAG, "Failed to emit NavigateToDailyLog effect for $date")
                    }
                }
            }

            is TrackerEvent.PeriodMarkDay -> viewModelScope.launch {
                val periodForDate = _uiState.value.periods.find {
                    event.date in (it.startDate..(it.endDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())))
                }
                if (periodForDate != null) {
                    periodRepository.unLogPeriodDay(event.date)
                } else {
                    periodRepository.logPeriodDay(event.date)
                }
            }

            is TrackerEvent.PeriodRangeDragged -> viewModelScope.launch {
                val anchor = event.anchorDate
                val release = event.releaseDate
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val rangeStart = minOf(anchor, release)
                val rangeEnd = maxOf(anchor, release)

                val anchorPeriod = _uiState.value.periods.find {
                    anchor in (it.startDate..(it.endDate ?: today))
                }

                when {
                    // Shrink from start: anchor is start of period, release is later (inside period).
                    anchorPeriod != null
                        && anchor == anchorPeriod.startDate
                        && anchor != (anchorPeriod.endDate ?: today)
                        && release > anchor
                        && release <= (anchorPeriod.endDate ?: today) -> {
                        var d = anchor
                        while (d < release) {
                            periodRepository.unLogPeriodDay(d)
                            d = d.plus(1, DateTimeUnit.DAY)
                        }
                    }
                    // Shrink from end: anchor is end of period, release is earlier (inside period).
                    anchorPeriod != null
                        && anchor == (anchorPeriod.endDate ?: today)
                        && anchor != anchorPeriod.startDate
                        && release < anchor
                        && release >= anchorPeriod.startDate -> {
                        var d = anchor
                        while (d > release) {
                            periodRepository.unLogPeriodDay(d)
                            d = d.minus(1, DateTimeUnit.DAY)
                        }
                    }
                    // Default: mark all days in range as period days.
                    else -> {
                        var d = rangeStart
                        while (d <= rangeEnd) {
                            periodRepository.logPeriodDay(d)
                            d = d.plus(1, DateTimeUnit.DAY)
                        }
                    }
                }
            }

            is TrackerEvent.EditLogClicked -> viewModelScope.launch {
                if (!_effect.tryEmit(TrackerEffect.NavigateToDailyLog(event.date))) {
                    Log.w(TAG, "Failed to emit NavigateToDailyLog effect for ${event.date}")
                }
            }

            is TrackerEvent.DeletePeriodConfirmed -> viewModelScope.launch {
                periodRepository.deletePeriod(event.periodId)
            }

            is TrackerEvent.ShowEducationalSheet -> {
                val articles = educationalContentProvider.getByTag(event.contentTag)
                _uiState.update { it.copy(educationalArticles = articles.ifEmpty { null }) }
            }

            // State-only events — no side effects needed.
            is TrackerEvent.DismissLogSheet,
            is TrackerEvent.DeletePeriodRequested,
            is TrackerEvent.DeletePeriodDismissed,
            is TrackerEvent.DismissEducationalSheet -> { /* state-only */ }
        }
    }

    /**
     * Pure function that returns the new [TrackerUiState] for a given event.
     *
     * Contains no side effects — all repository writes, use-case calls, and
     * effect emissions are handled in [onEvent] after the state has been updated.
     */
    private fun reduce(currentState: TrackerUiState, event: TrackerEvent): TrackerUiState {
        return when (event) {
            is TrackerEvent.ScreenEntered -> currentState
            is TrackerEvent.DayTapped -> currentState
            is TrackerEvent.PeriodMarkDay -> currentState
            is TrackerEvent.PeriodRangeDragged -> currentState
            is TrackerEvent.DismissLogSheet -> {
                currentState.copy(logForSheet = null, periodIdForSheet = null, waterCupsForSheet = null)
            }
            is TrackerEvent.EditLogClicked -> {
                currentState.copy(logForSheet = null, periodIdForSheet = null, waterCupsForSheet = null)
            }
            is TrackerEvent.DeletePeriodRequested -> {
                currentState.copy(
                    showDeleteConfirmation = true,
                    periodIdToDelete = event.periodId,
                    logForSheet = null,
                    periodIdForSheet = null
                )
            }
            is TrackerEvent.DeletePeriodConfirmed -> {
                currentState.copy(
                    showDeleteConfirmation = false,
                    periodIdToDelete = null
                )
            }
            is TrackerEvent.DeletePeriodDismissed -> {
                currentState.copy(
                    showDeleteConfirmation = false,
                    periodIdToDelete = null
                )
            }
            is TrackerEvent.ShowEducationalSheet -> currentState
            is TrackerEvent.DismissEducationalSheet -> currentState.copy(educationalArticles = null)
        }
    }

    /**
     * Caches the predicted next period date in [AppSettings] (plaintext DataStore)
     * so the [PeriodPredictionWorker][com.veleda.cyclewise.reminders.workers.PeriodPredictionWorker]
     * can access it without unlocking the encrypted database.
     *
     * Uses the same average-cycle-length algorithm as [InsightEngine]: computes the mean
     * cycle length from completed periods and projects from the latest period's start date.
     * Requires at least 2 completed periods. Clears the cache when insufficient data exists.
     */
    private suspend fun updatePredictionCache(periods: List<Period>) {
        val completed = periods.filter { it.endDate != null }.sortedBy { it.startDate }
        if (completed.size < 2) {
            appSettings.setCachedPredictedPeriodDate("")
            return
        }
        val cycleLengths = completed.zipWithNext { current, next ->
            current.startDate.daysUntil(next.startDate).toDouble()
        }
        val avgDays = cycleLengths.average().roundToInt()
        val latest = periods.maxBy { it.startDate }
        val predicted = latest.startDate.plus(avgDays, DateTimeUnit.DAY)
        appSettings.setCachedPredictedPeriodDate(predicted.toString())
    }

    companion object {
        private const val TAG = "TrackerViewModel"
    }
}
