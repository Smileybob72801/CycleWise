package com.veleda.cyclewise.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
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

// Represents the state of the UI, now holding the FullDailyLog
data class DailyLogUiState(
    val isLoading: Boolean = true,
    val log: FullDailyLog? = null,
    val error: String? = null
)

class DailyLogViewModel(
    private val entryDate: LocalDate,
    private val getOrCreateDailyEntryUseCase: GetOrCreateDailyEntryUseCase,
    private val cycleRepository: CycleRepository
) : ViewModel() {

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
    }

    private fun loadLog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            var result = cycleRepository.getFullLogForDate(entryDate)

            if (result == null) {
                val newEntry = getOrCreateDailyEntryUseCase(entryDate)
                if (newEntry != null) {
                    result = FullDailyLog(entry = newEntry)
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    log = result,
                    error = if (result == null) "Could not find an active cycle for this date." else null
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
            val currentSymptoms = state.log?.symptoms.orEmpty()
            val isSelected = currentSymptoms.any { it.type == symptomType }

            val newSymptoms = if (isSelected) {
                currentSymptoms.filterNot { it.type == symptomType }
            } else {
                currentSymptoms + Symptom(
                    id = uuid4().toString(),
                    entryId = state.log?.entry?.id ?: "",
                    type = symptomType,
                    severity = 3 // Default severity
                )
            }
            state.copy(log = state.log?.copy(symptoms = newSymptoms))
        }
    }

    fun saveLog() {
        val logToSave = _uiState.value.log ?: return
        viewModelScope.launch {
            cycleRepository.saveFullLog(logToSave)
        }
    }
}