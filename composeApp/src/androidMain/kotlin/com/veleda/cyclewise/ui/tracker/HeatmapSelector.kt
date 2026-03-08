package com.veleda.cyclewise.ui.tracker

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.HeatmapMetric
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Horizontal scrollable row of filter chips for selecting a calendar heatmap metric.
 *
 * Includes an "Off" chip to disable the overlay, plus one chip per [HeatmapMetric].
 *
 * @param selectedMetric The currently active metric, or null for "Off".
 * @param onMetricSelected Callback when a metric is selected or deselected.
 * @param modifier Modifier applied to the row.
 */
@Composable
internal fun HeatmapSelector(
    selectedMetric: HeatmapMetric?,
    onMetricSelected: (HeatmapMetric?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current
    val metrics = listOf(
        HeatmapMetric.Mood,
        HeatmapMetric.Energy,
        HeatmapMetric.Libido,
        HeatmapMetric.WaterIntake,
        HeatmapMetric.SymptomSeverity,
        HeatmapMetric.FlowIntensity,
        HeatmapMetric.MedicationCount,
    )

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = dims.md),
        horizontalArrangement = Arrangement.spacedBy(dims.sm),
    ) {
        FilterChip(
            selected = selectedMetric == null,
            onClick = { onMetricSelected(null) },
            label = { Text(stringResource(R.string.heatmap_off)) },
        )

        metrics.forEach { metric ->
            FilterChip(
                selected = selectedMetric == metric,
                onClick = {
                    onMetricSelected(if (selectedMetric == metric) null else metric)
                },
                label = { Text(metric.label) },
            )
        }
    }
}
