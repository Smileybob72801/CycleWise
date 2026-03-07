package com.veleda.cyclewise.ui.insights.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.veleda.cyclewise.domain.insights.TopSymptomsInsight
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Card for [TopSymptomsInsight] — displays symptom names as suggestion chips in a flow row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TopSymptomsCard(insight: TopSymptomsInsight) {
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
