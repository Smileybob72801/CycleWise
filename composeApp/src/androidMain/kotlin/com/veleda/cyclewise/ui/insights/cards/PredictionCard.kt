package com.veleda.cyclewise.ui.insights.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.insights.NextPeriodPrediction
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlin.math.abs

/**
 * Card for [NextPeriodPrediction] — shows a calendar icon and countdown text.
 */
@Composable
internal fun PredictionCard(insight: NextPeriodPrediction) {
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
