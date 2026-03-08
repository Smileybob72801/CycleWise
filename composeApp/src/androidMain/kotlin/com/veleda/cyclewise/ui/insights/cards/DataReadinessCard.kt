package com.veleda.cyclewise.ui.insights.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.insights.DataReadiness
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Shows data collection progress for a single insight category.
 *
 * Displays a progress bar and countdown when not enough data is available,
 * or a "Ready" badge when the threshold is met.
 */
@Composable
internal fun DataReadinessCard(
    readiness: DataReadiness,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current
    val progress = if (readiness.periodsRequired > 0) {
        readiness.completedPeriods.toFloat() / readiness.periodsRequired
    } else {
        1f
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(dims.md),
            verticalArrangement = Arrangement.spacedBy(dims.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = readiness.label,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (readiness.isReady) {
                        stringResource(R.string.insights_data_readiness_ready)
                    } else {
                        stringResource(
                            R.string.insights_data_readiness_periods_needed,
                            readiness.periodsRemaining,
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (readiness.isReady) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (!readiness.isReady) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }
    }
}
