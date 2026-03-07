package com.veleda.cyclewise.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.InsightEngine
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
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
 * @property isLoading          True during the initial insight generation pass.
 * @property isRefreshing       True during a pull-to-refresh reload (existing content stays visible).
 * @property insights           The generated list of [Insight] cards to display.
 * @property allArticles        Complete list of educational articles (sorted by [EducationalArticle.sortOrder]).
 * @property filteredArticles   Articles filtered by the currently selected category (or all if no filter).
 * @property selectedCategory   The active category filter, or `null` for "All".
 * @property expandedArticleIds IDs of articles whose body content is currently expanded.
 */
data class InsightsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val insights: List<Insight> = emptyList(),
    val allArticles: List<EducationalArticle> = emptyList(),
    val filteredArticles: List<EducationalArticle> = emptyList(),
    val selectedCategory: ArticleCategory? = null,
    val expandedArticleIds: Set<String> = emptySet(),
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
 * The Learn section is populated from [EducationalContentProvider] (singleton-scoped,
 * static content) with category filtering and expandable article cards.
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

    /**
     * Single entry-point for all screen-level user interactions.
     *
     * Applies a pure state update via [reduce], then launches side effects
     * (content provider queries) when needed.
     */
    fun onEvent(event: InsightsEvent) {
        _uiState.update { reduce(it, event) }

        // Side effects — launched after state update
        when (event) {
            is InsightsEvent.FilterArticles -> {
                val filtered = if (event.category == null) {
                    _uiState.value.allArticles
                } else {
                    educationalContentProvider.getByCategory(event.category)
                }
                _uiState.update { it.copy(filteredArticles = filtered) }
            }

            // State-only events — no side effects needed.
            is InsightsEvent.ToggleArticleExpanded -> { /* state-only */ }
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
}
