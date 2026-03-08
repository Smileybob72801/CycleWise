package com.veleda.cyclewise.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.insights.CrossVariableCorrelation
import com.veleda.cyclewise.domain.insights.CycleLengthAverage
import com.veleda.cyclewise.domain.insights.CycleLengthTrend
import com.veleda.cyclewise.domain.insights.CycleSummary
import com.veleda.cyclewise.domain.insights.EnergyPhasePattern
import com.veleda.cyclewise.domain.insights.FlowPattern
import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.InsightCategory
import com.veleda.cyclewise.domain.insights.LibidoPhasePattern
import com.veleda.cyclewise.domain.insights.MoodPhasePattern
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import com.veleda.cyclewise.domain.insights.SymptomPhasePattern
import com.veleda.cyclewise.domain.insights.SymptomSeverityTrend
import com.veleda.cyclewise.domain.insights.TopSymptomsInsight
import com.veleda.cyclewise.domain.insights.WaterIntakePhasePattern
import com.veleda.cyclewise.ui.components.ContentContainer
import com.veleda.cyclewise.ui.components.MedicalDisclaimer
import com.veleda.cyclewise.ui.insights.cards.CycleLengthCard
import com.veleda.cyclewise.ui.insights.cards.CycleSummaryCard
import com.veleda.cyclewise.ui.insights.cards.DataReadinessCard
import com.veleda.cyclewise.ui.insights.cards.GenericInsightCard
import com.veleda.cyclewise.ui.insights.cards.MoodPatternCard
import com.veleda.cyclewise.ui.insights.cards.PredictionCard
import com.veleda.cyclewise.ui.insights.cards.SymptomPhaseCard
import com.veleda.cyclewise.ui.insights.cards.TopSymptomsCard
import com.veleda.cyclewise.ui.insights.cards.TrendCard
import com.veleda.cyclewise.ui.insights.charts.ChartsSection
import com.veleda.cyclewise.ui.insights.learn.LearnArticleCard
import com.veleda.cyclewise.ui.insights.learn.LearnCategoryChips
import com.veleda.cyclewise.ui.insights.learn.LearnSectionHeader
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

/**
 * Top-level insights screen with Koin-injected ViewModel.
 *
 * Delegates all rendering to [InsightsContent] for testability.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen() {
    val viewModel: InsightsViewModel = koinViewModel(scope = getKoin().getScope("session"))
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.insights_title)) })
        }
    ) { padding ->
        InsightsContent(
            uiState = uiState,
            onRefresh = viewModel::refresh,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }
}

/**
 * Testable content composable that renders the progressive disclosure insights layout.
 *
 * Layout order:
 * 1. Cycle summary card (if active cycle)
 * 2. Data readiness countdown cards (for not-yet-ready categories)
 * 3. "Your Top Insights" section (top 5 scored insights)
 * 4. Categorized accordion sections (collapsed by default)
 * 5. Learn section (educational articles)
 *
 * @param uiState  Current UI state.
 * @param onRefresh Callback invoked when the user pulls to refresh.
 * @param onEvent   Callback for [InsightsEvent] dispatching.
 * @param modifier  Modifier applied to the root container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InsightsContent(
    uiState: InsightsUiState,
    onRefresh: () -> Unit,
    onEvent: (InsightsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = LocalDimensions.current
    val hasContent = uiState.insights.isNotEmpty() || uiState.cycleSummary != null ||
        uiState.dataReadiness.any { !it.isReady } || uiState.charts.isNotEmpty() ||
        uiState.allArticles.isNotEmpty()

    ContentContainer(modifier = modifier) {
        Box(Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    InsightsSkeletonLoader(modifier = Modifier.align(Alignment.TopStart))
                }
                !hasContent -> {
                    InsightsEmptyState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(dims.md),
                            verticalArrangement = Arrangement.spacedBy(dims.md)
                        ) {
                            // 1. Cycle Summary (pinned to top)
                            uiState.cycleSummary?.let { summary ->
                                item(key = "cycle-summary") {
                                    CycleSummaryCard(insight = summary)
                                }
                            }

                            // 2. Data Readiness Countdowns (not-ready categories only)
                            val notReady = uiState.dataReadiness.filter { !it.isReady }
                            if (notReady.isNotEmpty()) {
                                item(key = "readiness-header") {
                                    Text(
                                        text = stringResource(R.string.insights_data_readiness_header),
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(top = dims.sm),
                                    )
                                }
                                items(notReady, key = { "readiness-${it.category.name}" }) { readiness ->
                                    DataReadinessCard(readiness = readiness)
                                }
                            }

                            // 3. Top Insights
                            if (uiState.topInsights.isNotEmpty()) {
                                item(key = "top-header") {
                                    Text(
                                        text = stringResource(R.string.insights_top_section_header),
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(top = dims.sm),
                                    )
                                }
                                items(
                                    uiState.topInsights,
                                    key = { "top-${it.insight.id}" }
                                ) { scored ->
                                    InsightCardDispatcher(insight = scored.insight)
                                }
                            }

                            // 4. Charts Section
                            if (uiState.charts.isNotEmpty()) {
                                item(key = "charts-section") {
                                    ChartsSection(charts = uiState.charts)
                                }
                            }

                            // 5. Categorized Accordion Sections
                            for ((category, scored) in uiState.categorizedInsights) {
                                val isExpanded = category in uiState.expandedCategories
                                item(key = "cat-header-${category.name}") {
                                    CategoryHeader(
                                        category = category,
                                        count = scored.size,
                                        isExpanded = isExpanded,
                                        onToggle = { onEvent(InsightsEvent.ToggleCategory(category)) },
                                    )
                                }
                                if (isExpanded) {
                                    items(
                                        scored,
                                        key = { "cat-${category.name}-${it.insight.id}" }
                                    ) { item ->
                                        InsightCardDispatcher(insight = item.insight)
                                    }
                                }
                            }

                            // 6. Learn Section
                            if (uiState.allArticles.isNotEmpty()) {
                                item(key = "learn-header") {
                                    LearnSectionHeader()
                                }

                                item(key = "learn-chips") {
                                    LearnCategoryChips(
                                        selectedCategory = uiState.selectedCategory,
                                        onCategorySelected = { category ->
                                            onEvent(InsightsEvent.FilterArticles(category))
                                        },
                                    )
                                }

                                items(
                                    uiState.filteredArticles,
                                    key = { "article-${it.id}" }
                                ) { article ->
                                    LearnArticleCard(
                                        article = article,
                                        isExpanded = article.id in uiState.expandedArticleIds,
                                        onToggle = {
                                            onEvent(InsightsEvent.ToggleArticleExpanded(article.id))
                                        },
                                    )
                                }

                                item(key = "learn-disclaimer") {
                                    MedicalDisclaimer(
                                        modifier = Modifier.padding(top = dims.sm)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dispatches each [Insight] subtype to its dedicated card composable.
 */
@Composable
internal fun InsightCardDispatcher(insight: Insight) {
    when (insight) {
        is CycleLengthAverage -> CycleLengthCard(insight)
        is NextPeriodPrediction -> PredictionCard(insight)
        is CycleLengthTrend -> TrendCard(insight)
        is SymptomPhasePattern -> SymptomPhaseCard(insight)
        is MoodPhasePattern -> MoodPatternCard(insight)
        is TopSymptomsInsight -> TopSymptomsCard(insight)
        is CycleSummary -> CycleSummaryCard(insight)
        is EnergyPhasePattern -> GenericInsightCard(insight)
        is LibidoPhasePattern -> GenericInsightCard(insight)
        is WaterIntakePhasePattern -> GenericInsightCard(insight)
        is FlowPattern -> GenericInsightCard(insight)
        is SymptomSeverityTrend -> GenericInsightCard(insight)
        is CrossVariableCorrelation -> GenericInsightCard(insight)
    }
}
