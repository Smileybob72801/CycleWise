package com.veleda.cyclewise.ui.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.domain.usecases.AutoCloseOngoingPeriodUseCase
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.math.roundToInt
import kotlin.time.Clock

data class TrackerUiState(
    val periods: List<Period> = emptyList(),
    val logForSheet: FullDailyLog? = null,
    val periodIdForSheet: String? = null,
    val symptomLibrary: List<Symptom> = emptyList(),
    val medicationLibrary: List<Medication> = emptyList(),
    val dayDetails: Map<LocalDate, CalendarDayInfo> = emptyMap(),
    val showDeleteConfirmation: Boolean = false,
    val periodIdToDelete: String? = null,
    val waterCupsForSheet: Int? = null
) {
    val ongoingPeriod: Period? = periods.find { it.endDate == null }
}

/**
 * Calendar/tracker screen ViewModel managing period and log state.
 *
 * Uses a MVI-inspired pattern: [onEvent] dispatches to [reduce] (a pure function that
 * returns updated state). Side effects (navigation, repository writes) are launched
 * inside `reduce` via `viewModelScope`.
 *
 * **Init:** Collects 4 reactive flows — day details, periods, symptom library, and
 * medication library — each updating the corresponding [TrackerUiState] field.
 *
 * **Effects:** One-shot navigation events are emitted via [_effect] ([SharedFlow] with replay = 0).
 *
 * Session-scoped (destroyed on logout/autolock).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModel(
    private val periodRepository: PeriodRepository,
    private val symptomLibraryProvider: SymptomLibraryProvider,
    private  val medicationLibraryProvider: MedicationLibraryProvider,
    private val autoClosePeriodUseCase: AutoCloseOngoingPeriodUseCase,
    private val appSettings: AppSettings
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<TrackerEffect>(replay = 0)
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

    fun onEvent(event: TrackerEvent) {
        _uiState.update { currentState ->
            reduce(currentState, event)
        }
    }

    private fun reduce(currentState: TrackerUiState, event: TrackerEvent): TrackerUiState {
        return when (event) {
            is TrackerEvent.ScreenEntered -> {
                viewModelScope.launch {
                    autoClosePeriodUseCase()
                }
                currentState
            }
            is TrackerEvent.DayTapped -> {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val date = event.date
                viewModelScope.launch {
                    val periodForDate = _uiState.value.periods.find {
                        date in (it.startDate..(it.endDate ?: today))
                    }
                    val isPeriodDay = periodForDate != null
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
                        _effect.emit(TrackerEffect.NavigateToDailyLog(date, isPeriodDay))
                    }
                }
                currentState
            }
            is TrackerEvent.PeriodMarkDay -> {
                viewModelScope.launch {
                    val periodForDate = currentState.periods.find { event.date in (it.startDate..(it.endDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault()))) }

                    if (periodForDate != null) {
                        periodRepository.unLogPeriodDay(event.date)
                    } else {
                        periodRepository.logPeriodDay(event.date)
                    }
                }
                currentState
            }
            is TrackerEvent.DismissLogSheet -> {
                currentState.copy(logForSheet = null, periodIdForSheet = null, waterCupsForSheet = null)
            }
            is TrackerEvent.EditLogClicked -> {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val date = event.date
                val periodForDate = currentState.periods.find {
                    date in (it.startDate..(it.endDate ?: today))
                }
                val isPeriodDay = periodForDate != null
                viewModelScope.launch {
                    _effect.emit(TrackerEffect.NavigateToDailyLog(date, isPeriodDay))
                }
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
                viewModelScope.launch {
                    periodRepository.deletePeriod(event.periodId)
                }
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
}
