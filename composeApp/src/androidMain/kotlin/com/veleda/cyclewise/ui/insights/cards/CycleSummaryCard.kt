package com.veleda.cyclewise.ui.insights.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.veleda.cyclewise.domain.insights.CycleSummary
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Prominent card showing the current cycle day, phase, and countdown.
 */
@Composable
internal fun CycleSummaryCard(insight: CycleSummary) {
    val dims = LocalDimensions.current

    AccentedInsightCard(accentColor = MaterialTheme.colorScheme.primary) {
        Text(
            text = insight.title,
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dims.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Day ${insight.cycleDay}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = insight.phaseName.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = insight.description,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
