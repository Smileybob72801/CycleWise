package com.veleda.cyclewise.ui.insights.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.insights.charts.BarChartData
import com.veleda.cyclewise.domain.insights.charts.ChartData
import com.veleda.cyclewise.domain.insights.charts.LineChartData
import com.veleda.cyclewise.ui.theme.LocalDimensions

private val CHART_HEIGHT = 200.dp
private val DOT_SIZE = 8.dp

/**
 * Horizontal pager of insight charts with page indicator dots.
 *
 * Each page renders a full-width chart card with title. Only charts
 * with sufficient data should be included in [charts].
 *
 * @param charts  Charts to display in the pager.
 * @param modifier Modifier applied to the root layout.
 */
@Composable
internal fun ChartsSection(
    charts: List<ChartData>,
    modifier: Modifier = Modifier,
) {
    if (charts.isEmpty()) return

    val dims = LocalDimensions.current
    val pagerState = rememberPagerState(pageCount = { charts.size })

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dims.sm),
    ) {
        Text(
            text = stringResource(R.string.insights_charts_section_header),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = dims.sm),
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            ChartCard(chart = charts[page])
        }

        if (charts.size > 1) {
            PageIndicator(
                pageCount = charts.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

/**
 * A single chart card rendered in the pager.
 */
@Composable
private fun ChartCard(
    chart: ChartData,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dims.xs),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(dims.md),
            verticalArrangement = Arrangement.spacedBy(dims.sm),
        ) {
            Text(
                text = chart.title,
                style = MaterialTheme.typography.titleSmall,
            )

            Box(modifier = Modifier.height(CHART_HEIGHT).fillMaxWidth()) {
                when (chart) {
                    is LineChartData -> VicoLineChart(
                        data = chart,
                        modifier = Modifier.matchParentSize(),
                    )
                    is BarChartData -> VicoBarChart(
                        data = chart,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }
    }
}

/**
 * Row of small indicator dots for the chart pager.
 */
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DOT_SIZE / 2),
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(DOT_SIZE)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
            )
        }
    }
}
