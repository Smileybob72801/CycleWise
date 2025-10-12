package com.veleda.cyclewise.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.InsightEngine
import com.veleda.cyclewise.domain.repository.CycleRepository
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsightsUiState(
    val isLoading: Boolean = true,
    val insights: List<Insight> = emptyList()
)

class InsightsViewModel(
    private val cycleRepository: CycleRepository,
    private val insightEngine: InsightEngine,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        generateInsights()
    }

    private fun generateInsights() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Fetch all necessary data in one go
            val allCycles = cycleRepository.getAllCycles().first()
            val allLogs = cycleRepository.getAllLogs().first()
            val symptomLibrary = cycleRepository.getSymptomLibrary().first()
            val topSymptomsCount = appSettings.topSymptomsCount.first()

            // Run the analysis, passing the config value to the engine.
            val generatedInsights = insightEngine.generateInsights(
                allCycles = allCycles,
                allLogs = allLogs,
                symptomLibrary = symptomLibrary,
                topSymptomsCount = topSymptomsCount
            )

            // Update the state with the results
            _uiState.update {
                it.copy(
                    isLoading = false,
                    insights = generatedInsights
                )
            }
        }
    }
}