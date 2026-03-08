package com.veleda.cyclewise.ui.insights.cards

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.veleda.cyclewise.domain.insights.Insight

/**
 * A generic insight card used as a fallback for insight subtypes that don't yet
 * have a dedicated card composable.
 *
 * Displays the insight's [Insight.title] and [Insight.description] using the
 * standard [AccentedInsightCard] wrapper with the tertiary color accent.
 */
@Composable
internal fun GenericInsightCard(insight: Insight) {
    AccentedInsightCard(accentColor = MaterialTheme.colorScheme.tertiary) {
        Text(
            text = insight.title,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = insight.description,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
