package com.veleda.cyclewise.ui.insights.learn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.domain.models.ArticleCategory
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.ui.components.MarkdownText
import com.veleda.cyclewise.ui.components.SourceAttribution
import com.veleda.cyclewise.ui.insights.cards.AccentedInsightCard
import com.veleda.cyclewise.ui.theme.LocalDimensions

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
internal fun LearnArticleCard(
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
