package com.veleda.cyclewise.ui.insights.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.insights.CycleLengthAverage
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlin.math.roundToInt

/**
 * Card for [CycleLengthAverage] — displays the average cycle length as a large number.
 */
@Composable
internal fun CycleLengthCard(insight: CycleLengthAverage) {
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
