package com.veleda.cyclewise.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.repository.CycleRepository
import com.veleda.cyclewise.domain.usecases.GetOrCreateDailyEntryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.compose.getKoin

// Represents the state of the UI
data class DailyLogUiState(
    val isLoading: Boolean = true,
    val entry: DailyEntry? = null,
    val error: String? = null
)

class DailyLogViewModel(
    private val entryDate: LocalDate,
    private val getOrCreateDailyEntryUseCase: GetOrCreateDailyEntryUseCase,
    private val cycleRepository: CycleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyLogUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getOrCreateDailyEntryUseCase(entryDate)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    entry = result,
                    error = if (result == null) "Could not find an active cycle for this date." else null
                )
            }
        }
    }

    fun setFlowIntensity(intensity: String?) {
        _uiState.update {
            it.copy(entry = it.entry?.copy(flowIntensity = intensity))
        }
    }

    fun setMoodScore(score: Int) {
        _uiState.update {
            it.copy(entry = it.entry?.copy(moodScore = score))
        }
    }

    fun saveEntry() {
        val entryToSave = _uiState.value.entry ?: return
        viewModelScope.launch {
            cycleRepository.saveEntry(entryToSave)
            // Optionally add logic here to navigate back or show a "Saved" confirmation
        }
    }
}