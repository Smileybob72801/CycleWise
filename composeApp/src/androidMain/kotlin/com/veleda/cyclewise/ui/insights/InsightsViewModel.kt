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
    val isRefreshing: Boolean = false,
    val insights: List<Insight> = emptyList()
)

/**
 * Generates and formats cycle insights on init, with pull-to-refresh support.
 *
 * Fetches all periods, logs, and the symptom library in one shot, runs the [InsightEngine],
 * and applies platform-specific localized date formatting to [NextPeriodPrediction] and
 * [SymptomPhasePattern] insights before emitting the final list to [uiState].
 *
 * Initial load sets [InsightsUiState.isLoading]; pull-to-refresh sets
 * [InsightsUiState.isRefreshing] so existing content stays visible during reload.
 *
 * Session-scoped (destroyed on logout/autolock).
 */
class InsightsViewModel(
    private val periodRepository: PeriodRepository,
    private val insightEngine: InsightEngine,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights(isRefresh = false)
    }

    /**
     * Triggers a pull-to-refresh reload of insights.
     *
     * Sets [InsightsUiState.isRefreshing] instead of [InsightsUiState.isLoading]
     * so the existing insight list remains visible while new data loads.
     */
    fun refresh() {
        loadInsights(isRefresh = true)
    }

    /**
     * Loads insights from the repository and insight engine.
     *
     * @param isRefresh When `false` (initial load), sets [InsightsUiState.isLoading].
     *                  When `true` (pull-to-refresh), sets [InsightsUiState.isRefreshing]
     *                  to keep existing content visible during reload.
     */
    private fun loadInsights(isRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true)
                else it.copy(isLoading = true)
            }

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
                        insight.copy(formattedDateString = insight.predictedDate.toLocalizedDateString())
                    }
                    is SymptomPhasePattern -> {
                        val formattedDate = insight.predictedDate?.toLocalizedDateString()
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
                    isRefreshing = false,
                    insights = formattedInsights
                )
            }
        }
    }
}