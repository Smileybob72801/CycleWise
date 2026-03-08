package com.veleda.cyclewise.ui.insights.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.veleda.cyclewise.domain.insights.charts.LineChartData

/**
 * Renders a [LineChartData] model using Vico.
 *
 * Shows one line per [LineChartData.series] with Material 3 theming.
 *
 * @param data     The chart data to render.
 * @param modifier Modifier applied to the chart host.
 */
@Composable
internal fun VicoLineChart(
    data: LineChartData,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember(data.key) { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            lineSeries {
                data.series.forEach { chartSeries ->
                    series(chartSeries.points.map { it.y.toDouble() })
                }
            }
        }
    }

    CartesianChartHost(
        rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        ),
        modelProducer,
        modifier = modifier,
    )
}
