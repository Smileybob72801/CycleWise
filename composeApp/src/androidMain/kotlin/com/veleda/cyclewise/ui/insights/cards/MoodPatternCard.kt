package com.veleda.cyclewise.ui.insights.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.veleda.cyclewise.domain.insights.MoodPhasePattern
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Card for [MoodPhasePattern] — shows a mood-specific icon and description.
 */
@Composable
internal fun MoodPatternCard(insight: MoodPhasePattern) {
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
