package com.veleda.cyclewise.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.veleda.cyclewise.domain.insights.CycleLengthAverage
import com.veleda.cyclewise.domain.insights.CycleLengthTrend
import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.MoodPhasePattern
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import com.veleda.cyclewise.domain.insights.SymptomPhasePattern
import com.veleda.cyclewise.domain.insights.TopSymptomsInsight
import com.veleda.cyclewise.ui.components.ContentContainer
import com.veleda.cyclewise.ui.components.MedicalDisclaimer
import com.veleda.cyclewise.ui.insights.cards.CycleLengthCard
import com.veleda.cyclewise.ui.insights.cards.MoodPatternCard
import com.veleda.cyclewise.ui.insights.cards.PredictionCard
import com.veleda.cyclewise.ui.insights.cards.SymptomPhaseCard
import com.veleda.cyclewise.ui.insights.cards.TopSymptomsCard
import com.veleda.cyclewise.ui.insights.cards.TrendCard
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
 * Testable content composable that renders the insight list, empty state, or loading indicator.
 *
 * Accepts [InsightsUiState] and an [onRefresh] callback instead of a ViewModel reference
 * so it can be tested in isolation without Koin or coroutine concerns.
 *
 * When educational articles are available, a **Learn** section is appended below the insight
 * cards with category filter chips and expandable article cards.
 *
 * @param uiState  Current UI state (loading, refreshing, or populated).
 * @param onRefresh Callback invoked when the user pulls to refresh.
 * @param onEvent   Callback for [InsightsEvent] dispatching (article filtering, expand/collapse).
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
    val hasContent = uiState.insights.isNotEmpty() || uiState.allArticles.isNotEmpty()

    ContentContainer(modifier = modifier) {
        Box(Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                            items(uiState.insights, key = { it.id }) { insight ->
                                InsightCardDispatcher(insight = insight)
                            }

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
    }
}
