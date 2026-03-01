package com.veleda.cyclewise.ui.insights.cards

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.insights.SymptomPhasePattern
import com.veleda.cyclewise.ui.theme.LocalCyclePhasePalette

/**
 * Card for [SymptomPhasePattern] — phase-color accented with symptom name and recurrence rate.
 *
 * The accent color is derived from the [SymptomPhasePattern.phaseDescription] text,
 * mapping to the appropriate cycle-phase border color from [LocalCyclePhasePalette].
 */
@Composable
internal fun SymptomPhaseCard(insight: SymptomPhasePattern) {
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
