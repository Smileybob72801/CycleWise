package com.veleda.cyclewise.domain.insights.charts

/**
 * Platform-agnostic chart data models for insight visualizations.
 *
 * Each subtype carries the pre-computed data series and axis labels.
 * Compose UI composables in `composeApp` render these using Vico.
 */
sealed interface ChartData {
    /** Short title displayed above the chart. */
    val title: String

    /** Unique key for stable list identity. */
    val key: String
}

/**
 * A line chart with one or more data series.
 *
 * @property series     Ordered data points per series.
 * @property xAxisLabel Label for the horizontal axis.
 * @property yAxisLabel Label for the vertical axis.
 */
data class LineChartData(
    override val title: String,
    override val key: String,
    val series: List<ChartSeries>,
    val xAxisLabel: String,
    val yAxisLabel: String,
) : ChartData

/**
 * A bar chart with categorized values.
 *
 * @property bars       Ordered bars with labels and values.
 * @property xAxisLabel Label for the horizontal axis.
 * @property yAxisLabel Label for the vertical axis.
 */
data class BarChartData(
    override val title: String,
    override val key: String,
    val bars: List<ChartBar>,
    val xAxisLabel: String,
    val yAxisLabel: String,
) : ChartData

/**
 * A single named data series for [LineChartData].
 *
 * @property label  Series name (e.g., "Mood", "Energy").
 * @property points Ordered (x, y) pairs.
 */
data class ChartSeries(
    val label: String,
    val points: List<ChartPoint>,
)

/**
 * A single point in a data series.
 *
 * @property x Horizontal value (e.g., cycle number or day offset).
 * @property y Vertical value (e.g., average score).
 * @property label Optional display label for this point.
 */
data class ChartPoint(
    val x: Float,
    val y: Float,
    val label: String? = null,
)

/**
 * A single bar in a [BarChartData].
 *
 * @property label Display label (e.g., phase name).
 * @property value Bar height value.
 */
data class ChartBar(
    val label: String,
    val value: Float,
)
