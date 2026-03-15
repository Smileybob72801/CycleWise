package com.veleda.cyclewise.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.tracker.HeatmapMetricColors

/**
 * Composable that renders seven color-editing rows (one per heatmap metric),
 * each with a preset color grid, plus a "Reset to Defaults" button.
 *
 * Reuses [PhaseColorRow] and [PresetColorGrid] from [PhaseColorSettings].
 *
 * @param moodHex              Current 6-char hex color for Mood.
 * @param energyHex            Current 6-char hex color for Energy.
 * @param libidoHex            Current 6-char hex color for Libido.
 * @param waterIntakeHex       Current 6-char hex color for Water Intake.
 * @param symptomSeverityHex   Current 6-char hex color for Symptom Severity.
 * @param flowIntensityHex     Current 6-char hex color for Flow Intensity.
 * @param medicationCountHex   Current 6-char hex color for Medication Count.
 * @param onColorChanged       Callback invoked with (metricKey, hex) when the user edits a color.
 * @param onResetDefaults      Callback invoked when the user taps "Reset to Defaults".
 * @param showTitle            When `true` (default), renders a header above the rows.
 */
@Composable
fun HeatmapColorSettings(
    moodHex: String,
    energyHex: String,
    libidoHex: String,
    waterIntakeHex: String,
    symptomSeverityHex: String,
    flowIntensityHex: String,
    medicationCountHex: String,
    onColorChanged: (metricKey: String, hex: String) -> Unit,
    onResetDefaults: () -> Unit,
    showTitle: Boolean = true,
) {
    val dims = LocalDimensions.current

    data class MetricRow(
        val key: String,
        val labelRes: Int,
        val hex: String,
        val defaultColor: androidx.compose.ui.graphics.Color,
    )

    val metrics = listOf(
        MetricRow("mood", R.string.heatmap_color_mood_label, moodHex, HeatmapMetricColors.Mood),
        MetricRow("energy", R.string.heatmap_color_energy_label, energyHex, HeatmapMetricColors.Energy),
        MetricRow("libido", R.string.heatmap_color_libido_label, libidoHex, HeatmapMetricColors.Libido),
        MetricRow("water", R.string.heatmap_color_water_intake_label, waterIntakeHex, HeatmapMetricColors.WaterIntake),
        MetricRow("symptom_severity", R.string.heatmap_color_symptom_severity_label, symptomSeverityHex, HeatmapMetricColors.SymptomSeverity),
        MetricRow("flow", R.string.heatmap_color_flow_intensity_label, flowIntensityHex, HeatmapMetricColors.FlowIntensity),
        MetricRow("medications", R.string.heatmap_color_medication_count_label, medicationCountHex, HeatmapMetricColors.MedicationCount),
    )

    Column {
        if (showTitle) {
            Text(
                stringResource(R.string.heatmap_colors_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(dims.sm))
        }

        metrics.forEachIndexed { index, metric ->
            PhaseColorRow(
                label = stringResource(metric.labelRes),
                hexValue = metric.hex,
                defaultColor = metric.defaultColor,
                onValueChange = { onColorChanged(metric.key, it) },
            )
            PresetColorGrid(
                selectedHex = metric.hex,
                onSelect = { onColorChanged(metric.key, it) },
            )
            if (index < metrics.lastIndex) {
                Spacer(Modifier.height(dims.sm))
            }
        }

        Spacer(Modifier.height(dims.sm))
        TextButton(onClick = onResetDefaults) {
            Text(stringResource(R.string.heatmap_color_reset_defaults))
        }
    }
}
