package com.veleda.cyclewise.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.InsightEngine
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import com.veleda.cyclewise.domain.insights.SymptomPhasePattern
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.utils.toLocalizedDateString
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
    private val periodRepository: PeriodRepository,
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
            val allCycles = periodRepository.getAllPeriods().first()
            val allLogs = periodRepository.getAllLogs().first()
            val symptomLibrary = periodRepository.getSymptomLibrary().first()
            val topSymptomsCount = appSettings.topSymptomsCount.first()

            // Run the analysis, passing the config value to the engine.
            val rawInsights = insightEngine.generateInsights(
                allPeriods = allCycles,
                allLogs = allLogs,
                symptomLibrary = symptomLibrary,
                topSymptomsCount = topSymptomsCount
            )

            // Apply platform-specific formatting to the insights
            val formattedInsights = rawInsights.map { insight ->
                when (insight) {
                    is NextPeriodPrediction -> {
                        // Format the raw predicted date and create a new instance with the formatted string
                        insight.copy(formattedDateString = insight.predictedDate.toLocalizedDateString())
                    }
                    is SymptomPhasePattern -> {
                        val formattedDate = insight.predictedDate?.toLocalizedDateString()

                        // Create a copy only if the formatted date was successfully generated
                        if (formattedDate != null) {
                            insight.copy(formattedPredictedDateString = formattedDate)
                        } else {
                            insight
                        }
                    }
                    else -> insight
                }
            }

            // Update the state with the results
            _uiState.update {
                it.copy(
                    isLoading = false,
                    insights = formattedInsights
                )
            }
        }
    }
}