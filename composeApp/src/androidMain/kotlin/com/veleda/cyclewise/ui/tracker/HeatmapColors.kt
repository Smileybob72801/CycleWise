package com.veleda.cyclewise.ui.tracker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.veleda.cyclewise.domain.models.HeatmapMetric

/**
 * Maps a [HeatmapMetric] to a color gradient for calendar fill rendering.
 *
 * Each metric has a distinct hue from [HeatmapMetricColors]. Intensity is mapped
 * to alpha interpolation between a 0.3 baseline and a 0.9 maximum.
 *
 * Callers may supply [customColors] (keyed by [HeatmapMetric.key]) to override the
 * defaults — values come from the user's heatmap color preferences in Settings.
 *
 * @param metric       The active heatmap metric.
 * @param intensity    0.0-1.0 value for the day.
 * @param customColors Optional map of metric key → [Color] overrides.
 * @return Color for the calendar cell heatmap fill.
 */
internal fun heatmapColor(
    metric: HeatmapMetric,
    intensity: Float,
    customColors: Map<String, Color> = emptyMap(),
): Color {
    val baseColor = customColors[metric.key] ?: when (metric) {
        is HeatmapMetric.Mood -> HeatmapMetricColors.Mood
        is HeatmapMetric.Energy -> HeatmapMetricColors.Energy
        is HeatmapMetric.Libido -> HeatmapMetricColors.Libido
        is HeatmapMetric.WaterIntake -> HeatmapMetricColors.WaterIntake
        is HeatmapMetric.SymptomSeverity -> HeatmapMetricColors.SymptomSeverity
        is HeatmapMetric.FlowIntensity -> HeatmapMetricColors.FlowIntensity
        is HeatmapMetric.MedicationCount -> HeatmapMetricColors.MedicationCount
    }

    val maxAlpha = 0.9f
    return lerp(baseColor.copy(alpha = 0.3f), baseColor.copy(alpha = maxAlpha), intensity)
}
