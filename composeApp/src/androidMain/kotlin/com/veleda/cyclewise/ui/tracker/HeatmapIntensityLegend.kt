package com.veleda.cyclewise.ui.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * A compact horizontal gradient bar showing "Low -> High" intensity for the
 * active heatmap metric.
 *
 * Renders a horizontal gradient from the metric's low-alpha color (0.3) to
 * high-alpha color (0.9), bookended by "Low" and "High" labels. Appears below
 * the [HeatmapSelector] when a heatmap metric is active so users can interpret
 * the fill intensity on the calendar.
 *
 * @param metricColor The base color for the active heatmap metric.
 * @param modifier    Modifier applied to the outer [Row].
 */
@Composable
internal fun HeatmapIntensityLegend(
    metricColor: Color,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("heatmap-intensity-legend"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.xs),
    ) {
        Text(
            text = stringResource(R.string.heatmap_intensity_low),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(dims.sm)
                .clip(RoundedCornerShape(dims.xs))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            metricColor.copy(alpha = 0.3f),
                            metricColor.copy(alpha = 0.9f),
                        )
                    )
                ),
        )
        Text(
            text = stringResource(R.string.heatmap_intensity_high),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
