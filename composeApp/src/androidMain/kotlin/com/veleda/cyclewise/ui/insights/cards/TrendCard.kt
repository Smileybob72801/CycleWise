package com.veleda.cyclewise.ui.insights.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.insights.CycleLengthTrend
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Card for [CycleLengthTrend] — shows a directional arrow icon with description.
 */
@Composable
internal fun TrendCard(insight: CycleLengthTrend) {
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
