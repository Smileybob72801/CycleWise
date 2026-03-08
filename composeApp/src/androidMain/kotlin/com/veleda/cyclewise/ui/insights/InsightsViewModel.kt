package com.veleda.cyclewise.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.domain.insights.CycleSummary
import com.veleda.cyclewise.domain.insights.DataReadiness
import com.veleda.cyclewise.domain.insights.DataReadinessCalculator
import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.InsightCategory
import com.veleda.cyclewise.domain.insights.InsightEngine
import com.veleda.cyclewise.domain.insights.charts.ChartData
import com.veleda.cyclewise.domain.insights.charts.ChartDataGenerator
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import com.veleda.cyclewise.domain.insights.ScoredInsight
import com.veleda.cyclewise.domain.insights.SymptomPhasePattern
import com.veleda.cyclewise.domain.models.ArticleCategory
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.domain.providers.EducationalContentProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.utils.toLocalizedDateString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Insights screen.
 *
 * @property isLoading               True during the initial insight generation pass.
 * @property isRefreshing            True during a pull-to-refresh reload (existing content stays visible).
 * @property cycleSummary            The cycle summary insight (pinned to top), or null.
 * @property topInsights             Top 5 scored insights (excluding summary).
 * @property categorizedInsights     All insights grouped by category.
 * @property expandedCategories      Categories whose accordion section is currently expanded.
 * @property dataReadiness           Data readiness status for each insight category.
 * @property allArticles             Complete list of educational articles.
 * @property filteredArticles        Articles filtered by the currently selected category.
 * @property selectedCategory        The active category filter, or `null` for "All".
 * @property expandedArticleIds      IDs of articles whose body content is currently expanded.
 * @property charts                 Chart data objects with sufficient data for rendering.
 */
data class InsightsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val cycleSummary: CycleSummary? = null,
    val topInsights: List<ScoredInsight> = emptyList(),
    val categorizedInsights: Map<InsightCategory, List<ScoredInsight>> = emptyMap(),
    val expandedCategories: Set<InsightCategory> = emptySet(),
    val dataReadiness: List<DataReadiness> = emptyList(),
    val charts: List<ChartData> = emptyList(),
    val allArticles: List<EducationalArticle> = emptyList(),
    val filteredArticles: List<EducationalArticle> = emptyList(),
    val selectedCategory: ArticleCategory? = null,
    val expandedArticleIds: Set<String> = emptySet(),
) {
    /** All non-summary insights for backward compatibility. */
    val insights: List<Insight>
        get() = topInsights.map { it.insight }
}

/**
 * Generates and formats cycle insights on init, with pull-to-refresh support.
 *
 * Fetches all periods, logs, symptom library, water intakes, and medication library
 * in one shot, runs the [InsightEngine], scores and groups results, and computes
 * data readiness.
 *
 * Uses a pure [reduce] function for state transitions, with side effects launched
 * separately in [onEvent].
 *
 * Session-scoped (destroyed on logout/autolock).
 */
class InsightsViewModel(
    private val periodRepository: PeriodRepository,
    private val insightEngine: InsightEngine,
    private val appSettings: AppSettings,
    private val educationalContentProvider: EducationalContentProvider,
    private val dataReadinessCalculator: DataReadinessCalculator = DataReadinessCalculator(),
    private val chartDataGenerator: ChartDataGenerator = ChartDataGenerator(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights(isRefresh = false)
        loadArticles()
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

            val allCycles = periodRepository.getAllPeriods().first()
            val allLogs = periodRepository.getAllLogs().first()
            val symptomLibrary = periodRepository.getSymptomLibrary().first()
            val topSymptomsCount = appSettings.topSymptomsCount.first()
            val waterIntakes = periodRepository.getAllWaterIntakes().first()
            val medicationLibrary = periodRepository.getMedicationLibrary().first()

            val scoredInsights = insightEngine.generateInsights(
                allPeriods = allCycles,
                allLogs = allLogs,
                symptomLibrary = symptomLibrary,
                topSymptomsCount = topSymptomsCount,
                waterIntakes = waterIntakes,
                medicationLibrary = medicationLibrary,
            )

            val formatted = scoredInsights.map { scored ->
                when (val insight = scored.insight) {
                    is NextPeriodPrediction -> scored.copy(
                        insight = insight.copy(
                            formattedDateString = insight.predictedDate.toLocalizedDateString()
                        )
                    )
                    is SymptomPhasePattern -> {
                        val formattedDate = insight.predictedDate?.toLocalizedDateString()
                        if (formattedDate != null) {
                            scored.copy(
                                insight = insight.copy(formattedPredictedDateString = formattedDate)
                            )
                        } else {
                            scored
                        }
                    }
                    else -> scored
                }
            }

            val summary = formatted
                .map { it.insight }
                .filterIsInstance<CycleSummary>()
                .firstOrNull()

            val nonSummary = formatted.filter { it.insight !is CycleSummary }
            val top5 = nonSummary.take(TOP_INSIGHTS_COUNT)
            val categorized = nonSummary.groupBy { it.insight.category }

            val readiness = dataReadinessCalculator.calculate(allCycles)
            val charts = chartDataGenerator.generateAll(allCycles, allLogs, waterIntakes)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    cycleSummary = summary,
                    topInsights = top5,
                    categorizedInsights = categorized,
                    dataReadiness = readiness,
                    charts = charts,
                )
            }
        }
    }

    /**
     * Single entry-point for all screen-level user interactions.
     *
     * Applies a pure state update via [reduce], then launches side effects
     * (content provider queries) when needed.
     */
    fun onEvent(event: InsightsEvent) {
        _uiState.update { reduce(it, event) }

        when (event) {
            is InsightsEvent.FilterArticles -> {
                val filtered = if (event.category == null) {
                    _uiState.value.allArticles
                } else {
                    educationalContentProvider.getByCategory(event.category)
                }
                _uiState.update { it.copy(filteredArticles = filtered) }
            }
            is InsightsEvent.ToggleArticleExpanded -> { /* state-only */ }
            is InsightsEvent.ToggleCategory -> { /* state-only */ }
        }
    }

    /**
     * Pure function that returns the new [InsightsUiState] for a given event.
     *
     * Contains no side effects — content provider queries are handled in [onEvent]
     * after the state has been updated.
     */
    private fun reduce(state: InsightsUiState, event: InsightsEvent): InsightsUiState {
        return when (event) {
            is InsightsEvent.FilterArticles -> state.copy(selectedCategory = event.category)
            is InsightsEvent.ToggleArticleExpanded -> {
                val ids = state.expandedArticleIds.toMutableSet()
                if (event.articleId in ids) ids.remove(event.articleId) else ids.add(event.articleId)
                state.copy(expandedArticleIds = ids)
            }
            is InsightsEvent.ToggleCategory -> {
                val cats = state.expandedCategories.toMutableSet()
                if (event.category in cats) cats.remove(event.category) else cats.add(event.category)
                state.copy(expandedCategories = cats)
            }
        }
    }

    /**
     * Populates the article lists from [EducationalContentProvider].
     *
     * Called once during init. Articles are sorted by [EducationalArticle.sortOrder]
     * (handled by the provider).
     */
    private fun loadArticles() {
        val articles = educationalContentProvider.articles
        _uiState.update {
            it.copy(
                allArticles = articles,
                filteredArticles = articles,
            )
        }
    }

    companion object {
        private const val TOP_INSIGHTS_COUNT = 5
    }
}
