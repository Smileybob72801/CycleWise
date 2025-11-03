package com.veleda.cyclewise.ui.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.domain.usecases.AutoCloseOngoingPeriodUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class TrackerUiState(
    val periods: List<Period> = emptyList(),
    val logForSheet: FullDailyLog? = null,
    val periodIdForSheet: String? = null,
    val symptomLibrary: List<Symptom> = emptyList(),
    val medicationLibrary: List<Medication> = emptyList(),
    val dayDetails: Map<LocalDate, CalendarDayInfo> = emptyMap()
) {
    val ongoingPeriod: Period? = periods.find { it.endDate == null }
    val isSelectingRange: Boolean = false // Always false now that range selection is removed
}

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModel(
    private val periodRepository: PeriodRepository,
    private val symptomLibraryProvider: SymptomLibraryProvider,
    private  val medicationLibraryProvider: MedicationLibraryProvider,
    private val autoClosePeriodUseCase: AutoCloseOngoingPeriodUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            periodRepository.observeDayDetails()
                .map { domainDetailsMap ->
                    domainDetailsMap.mapValues { (_, domainDetails) ->
                        CalendarDayInfo(
                            isPeriodDay = domainDetails.isPeriodDay,
                            hasSymptoms = domainDetails.hasLoggedSymptoms,
                            hasMedications = domainDetails.hasLoggedMedications
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

    // Public helper for the UI to check existence of a log (used by onSingleTap)
    suspend fun getFullLogForDate(date: LocalDate): FullDailyLog? {
        return periodRepository.getFullLogForDate(date)
    }

    // Public method to manually show the log sheet (used by onSingleTap after log is fetched)
    fun showLogSheet(date: LocalDate, periodForDate: Period?) {
        viewModelScope.launch {
            // We expect the calling UI to ensure a log exists here.
            // If it doesn't, fetching a log will return null, and this function will return.
            val log = periodRepository.getFullLogForDate(date) ?: return@launch

            // If the log is shown for a non-period day, periodIdForSheet will be null,
            // which correctly disables the Delete Period button in the sheet.
            val periodId = periodForDate?.id

            _uiState.update { it.copy(logForSheet = log, periodIdForSheet = periodId) }
        }
    }

    private fun reduce(currentState: TrackerUiState, event: TrackerEvent): TrackerUiState {
        return when (event) {
            is TrackerEvent.ScreenEntered -> {
                // CRITICAL: Check for period closure immediately upon screen entry.
                viewModelScope.launch {
                    autoClosePeriodUseCase()
                }
                currentState
            }
            is TrackerEvent.PeriodMarkDay -> {
                viewModelScope.launch {
                    val periodForDate = currentState.periods.find { event.date in (it.startDate..(it.endDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault()))) }

                    if (periodForDate != null) {
                        // Unmark / Split Period
                        periodRepository.unLogPeriodDay(event.date)
                    } else {
                        // Mark / Merge Period
                        periodRepository.logPeriodDay(event.date)
                    }
                }
                currentState
            }
            is TrackerEvent.DismissLogSheet -> {
                currentState.copy(logForSheet = null, periodIdForSheet = null)
            }
            is TrackerEvent.DeletePeriodClicked -> {
                viewModelScope.launch {
                    periodRepository.deletePeriod(event.periodId)
                }
                // Clear the sheet, as the underlying log/period is now gone
                currentState.copy(logForSheet = null, periodIdForSheet = null)
            }
            else -> currentState
        }
    }
}