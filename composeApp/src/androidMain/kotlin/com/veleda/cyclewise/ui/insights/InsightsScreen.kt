package com.veleda.cyclewise.ui.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.insights.CycleLengthAverage
import com.veleda.cyclewise.domain.insights.CycleLengthTrend
import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.MoodPhasePattern
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import com.veleda.cyclewise.domain.insights.SymptomPhasePattern
import com.veleda.cyclewise.domain.insights.TopSymptomsInsight
import com.veleda.cyclewise.domain.models.ArticleCategory
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.ui.components.MarkdownText
import com.veleda.cyclewise.ui.components.MedicalDisclaimer
import com.veleda.cyclewise.ui.components.SourceAttribution
import com.veleda.cyclewise.ui.theme.LocalCyclePhasePalette
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import kotlin.math.abs
import kotlin.math.roundToInt

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

    Box(
        modifier = modifier.fillMaxSize()
    ) {
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

/**
 * Rich empty state displayed when no insights have been generated yet.
 *
 * Shows a large [Icons.Outlined.Insights] icon, a heading, and explanatory body text
 * to guide the user toward tracking their cycle.
 */
@Composable
private fun InsightsEmptyState(modifier: Modifier = Modifier) {
    val dims = LocalDimensions.current

    Column(
        modifier = modifier.padding(horizontal = dims.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dims.sm)
    ) {
        Icon(
            imageVector = Icons.Outlined.Insights,
            contentDescription = stringResource(R.string.insights_content_description_empty_icon),
            modifier = Modifier.size(dims.iconLg),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.insights_empty_heading),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.insights_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Dispatches each [Insight] subtype to its dedicated card composable.
 */
@Composable
private fun InsightCardDispatcher(insight: Insight) {
    when (insight) {
        is CycleLengthAverage -> CycleLengthCard(insight)
        is NextPeriodPrediction -> PredictionCard(insight)
        is CycleLengthTrend -> TrendCard(insight)
        is SymptomPhasePattern -> SymptomPhaseCard(insight)
        is MoodPhasePattern -> MoodPatternCard(insight)
        is TopSymptomsInsight -> TopSymptomsCard(insight)
    }
}

/**
 * Shared card wrapper with a colored left-border accent for visual differentiation.
 *
 * All insight cards use this wrapper for consistent styling: medium-rounded card shape,
 * `surfaceVariant` background, and a 4.dp accent bar drawn on the left edge.
 *
 * @param accentColor Color for the left-border accent.
 * @param modifier    Modifier applied to the outer [Card].
 * @param content     Card body content.
 */
@Composable
private fun AccentedInsightCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dims = LocalDimensions.current
    val accentWidth = dims.xs

    Card(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = accentColor,
                    topLeft = Offset.Zero,
                    size = Size(accentWidth.toPx(), size.height)
                )
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(
                start = dims.md + dims.sm + dims.xs,
                top = dims.md,
                end = dims.md,
                bottom = dims.md
            ),
            verticalArrangement = Arrangement.spacedBy(dims.sm)
        ) {
            content()
        }
    }
}

/**
 * Card for [CycleLengthAverage] — displays the average cycle length as a large number.
 */
@Composable
private fun CycleLengthCard(insight: CycleLengthAverage) {
    AccentedInsightCard(accentColor = MaterialTheme.colorScheme.primary) {
        Text(
            text = insight.title,
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.xs)
        ) {
            Text(
                text = insight.averageDays.roundToInt().toString(),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.insights_cycle_length_days_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = LocalDimensions.current.sm)
            )
        }
        Text(
            text = insight.description,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Card for [NextPeriodPrediction] — shows a calendar icon and countdown text.
 */
@Composable
private fun PredictionCard(insight: NextPeriodPrediction) {
    val dims = LocalDimensions.current
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    AccentedInsightCard(accentColor = tertiaryColor) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.sm)
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = stringResource(R.string.insights_content_description_prediction_icon),
                modifier = Modifier.size(dims.iconSm),
                tint = tertiaryColor
            )
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = when {
                insight.daysUntilPrediction == 0 -> stringResource(R.string.insights_prediction_today)
                insight.daysUntilPrediction > 0 -> stringResource(
                    R.string.insights_prediction_in_days,
                    insight.daysUntilPrediction
                )
                else -> stringResource(
                    R.string.insights_prediction_overdue,
                    abs(insight.daysUntilPrediction)
                )
            },
            style = MaterialTheme.typography.headlineMedium,
            color = tertiaryColor
        )
        Text(
            text = insight.description,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Card for [CycleLengthTrend] — shows a directional arrow icon with description.
 */
@Composable
private fun TrendCard(insight: CycleLengthTrend) {
    val dims = LocalDimensions.current
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val (icon, tint, contentDesc) = when {
        insight.changeInDays > 0 -> Triple(
            Icons.AutoMirrored.Filled.TrendingUp,
            tertiaryColor,
            stringResource(R.string.insights_content_description_trend_up)
        )
        insight.changeInDays < 0 -> Triple(
            Icons.AutoMirrored.Filled.TrendingDown,
            secondaryColor,
            stringResource(R.string.insights_content_description_trend_down)
        )
        else -> Triple(
            Icons.AutoMirrored.Filled.TrendingFlat,
            secondaryColor,
            stringResource(R.string.insights_content_description_trend_stable)
        )
    }

    AccentedInsightCard(accentColor = secondaryColor) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.sm)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                modifier = Modifier.size(dims.iconSm),
                tint = tint
            )
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = insight.description,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Card for [SymptomPhasePattern] — phase-color accented with symptom name and recurrence rate.
 *
 * The accent color is derived from the [SymptomPhasePattern.phaseDescription] text,
 * mapping to the appropriate cycle-phase border color from [LocalCyclePhasePalette].
 */
@Composable
private fun SymptomPhaseCard(insight: SymptomPhasePattern) {
    val palette = LocalCyclePhasePalette.current
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val accentColor = when {
        insight.phaseDescription.contains("period", ignoreCase = true) -> palette.menstruation.border
        insight.phaseDescription.contains("before", ignoreCase = true) -> palette.luteal.border
        insight.phaseDescription.contains("after", ignoreCase = true) -> palette.follicular.border
        else -> secondaryColor
    }

    AccentedInsightCard(accentColor = accentColor) {
        Text(
            text = insight.title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = insight.symptomName,
            style = MaterialTheme.typography.titleLarge,
            color = accentColor
        )
        Text(
            text = stringResource(R.string.insights_recurrence_rate, insight.recurrenceRate),
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = insight.description,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Card for [MoodPhasePattern] — shows a mood-specific icon and description.
 */
@Composable
private fun MoodPatternCard(insight: MoodPhasePattern) {
    val dims = LocalDimensions.current
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val icon = when {
        insight.moodType.contains("low", ignoreCase = true) -> Icons.Filled.SentimentDissatisfied
        insight.moodType.contains("high", ignoreCase = true) -> Icons.Filled.SentimentVerySatisfied
        else -> Icons.Filled.Mood
    }

    AccentedInsightCard(accentColor = tertiaryColor) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.sm)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = insight.moodType,
                modifier = Modifier.size(dims.iconSm),
                tint = tertiaryColor
            )
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = insight.description,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Card for [TopSymptomsInsight] — displays symptom names as suggestion chips in a flow row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopSymptomsCard(insight: TopSymptomsInsight) {
    val dims = LocalDimensions.current

    AccentedInsightCard(accentColor = MaterialTheme.colorScheme.secondary) {
        Text(
            text = insight.title,
            style = MaterialTheme.typography.titleMedium
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dims.sm),
            verticalArrangement = Arrangement.spacedBy(dims.sm)
        ) {
            insight.topSymptoms.forEach { symptomName ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(symptomName) }
                )
            }
        }
    }
}

// ── Learn Section ────────────────────────────────────────────────────────

/**
 * Header for the Learn section with title and subtitle.
 *
 * Wrapped in a [Surface] with background color so it visually separates
 * from the insight cards above during scrolling.
 */
@Composable
private fun LearnSectionHeader(modifier: Modifier = Modifier) {
    val dims = LocalDimensions.current

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = dims.sm),
            verticalArrangement = Arrangement.spacedBy(dims.xs)
        ) {
            Text(
                text = stringResource(R.string.insights_learn_header),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.insights_learn_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Horizontal row of [FilterChip]s for selecting an article category.
 *
 * Includes an "All" chip plus one chip per [ArticleCategory]. The currently
 * selected chip is highlighted.
 *
 * @param selectedCategory The currently active filter, or `null` for "All".
 * @param onCategorySelected Callback when a chip is tapped.
 */
@Composable
private fun LearnCategoryChips(
    selectedCategory: ArticleCategory?,
    onCategorySelected: (ArticleCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.sm),
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(R.string.article_category_all)) }
            )
        }
        items(ArticleCategory.entries.toList()) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(stringResource(categoryStringRes(category))) }
            )
        }
    }
}

/**
 * Expandable article card for the Learn section.
 *
 * When collapsed, shows the title and category label. When expanded, reveals
 * the article body and [SourceAttribution] with an expand/collapse animation.
 *
 * @param article    The [EducationalArticle] to display.
 * @param isExpanded Whether the body content is currently visible.
 * @param onToggle   Callback to toggle expanded state.
 */
@Composable
private fun LearnArticleCard(
    article: EducationalArticle,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current

    val accentColor = when (article.category) {
        ArticleCategory.CYCLE_BASICS -> MaterialTheme.colorScheme.primary
        ArticleCategory.SYMPTOMS -> MaterialTheme.colorScheme.secondary
        ArticleCategory.WELLNESS -> MaterialTheme.colorScheme.tertiary
        ArticleCategory.WHEN_TO_SEE_A_DOCTOR -> MaterialTheme.colorScheme.error
    }

    AccentedInsightCard(
        accentColor = accentColor,
        modifier = modifier.clickable(onClick = onToggle),
    ) {
        Text(
            text = article.title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(categoryStringRes(article.category)),
            style = MaterialTheme.typography.labelMedium,
            color = accentColor
        )
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(dims.sm)
            ) {
                Spacer(modifier = Modifier.height(dims.xs))
                MarkdownText(
                    text = article.body,
                    style = MaterialTheme.typography.bodyMedium,
                )
                SourceAttribution(sourceName = article.sourceName)
            }
        }
    }
}

/**
 * Maps an [ArticleCategory] to its string resource ID.
 */
private fun categoryStringRes(category: ArticleCategory): Int = when (category) {
    ArticleCategory.CYCLE_BASICS -> R.string.article_category_cycle_basics
    ArticleCategory.SYMPTOMS -> R.string.article_category_symptoms
    ArticleCategory.WELLNESS -> R.string.article_category_wellness
    ArticleCategory.WHEN_TO_SEE_A_DOCTOR -> R.string.article_category_when_to_see_doctor
}
