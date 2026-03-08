package com.veleda.cyclewise.ui.tracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.veleda.cyclewise.domain.models.HeatmapMetric

/**
 * Maps a [HeatmapMetric] to a color gradient for calendar overlay rendering.
 *
 * Each metric has a distinct hue. Intensity is mapped to alpha and color
 * interpolation between a light baseline and the metric's primary color.
 *
 * @param metric    The active heatmap metric.
 * @param intensity 0.0-1.0 value for the day.
 * @return Semi-transparent color for the calendar cell overlay.
 */
@Composable
internal fun heatmapColor(metric: HeatmapMetric, intensity: Float): Color {
    val baseColor = when (metric) {
        is HeatmapMetric.Mood -> MaterialTheme.colorScheme.primary
        is HeatmapMetric.Energy -> MaterialTheme.colorScheme.tertiary
        is HeatmapMetric.Libido -> MaterialTheme.colorScheme.secondary
        is HeatmapMetric.WaterIntake -> Color(0xFF2196F3) // Blue
        is HeatmapMetric.SymptomSeverity -> MaterialTheme.colorScheme.error
        is HeatmapMetric.FlowIntensity -> Color(0xFFE91E63) // Pink
        is HeatmapMetric.MedicationCount -> Color(0xFF4CAF50) // Green
    }

    val maxAlpha = 0.6f
    return lerp(baseColor.copy(alpha = 0.1f), baseColor.copy(alpha = maxAlpha), intensity)
}
