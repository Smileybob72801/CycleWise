package com.veleda.cyclewise.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.components.shimmer
import com.veleda.cyclewise.ui.theme.LocalDimensions

/** Number of data readiness skeleton cards. */
private const val DATA_READINESS_COUNT = 2

/** Number of top insight skeleton cards. */
private const val TOP_INSIGHT_COUNT = 2

/** Number of category accordion skeleton rows. */
private const val CATEGORY_HEADER_COUNT = 3

/**
 * Shimmer skeleton loader mimicking the post-overhaul insights layout.
 *
 * Displays placeholder shapes matching the progressive disclosure structure:
 * cycle summary card, data readiness cards, top insight cards, chart card,
 * and category accordion headers.
 */
@Composable
internal fun InsightsSkeletonLoader(modifier: Modifier = Modifier) {
    val dims = LocalDimensions.current
    val loadingDescription = stringResource(R.string.lottie_cd_loading)

    Column(
        modifier = modifier
            .padding(dims.md)
            .semantics { contentDescription = loadingDescription },
        verticalArrangement = Arrangement.spacedBy(dims.md),
    ) {
        // 1. Cycle Summary card skeleton
        SummaryCardSkeleton()

        // 2. Data Readiness section
        SectionHeaderSkeleton()
        repeat(DATA_READINESS_COUNT) {
            DataReadinessCardSkeleton()
        }

        // 3. Top Insights section
        SectionHeaderSkeleton()
        repeat(TOP_INSIGHT_COUNT) {
            InsightCardSkeleton()
        }

        // 4. Chart section
        SectionHeaderSkeleton()
        ChartCardSkeleton()

        // 5. Category accordion headers
        repeat(CATEGORY_HEADER_COUNT) {
            CategoryHeaderSkeleton()
        }
    }
}

/**
 * Skeleton matching [CycleSummaryCard] layout: title, Day N + phase row, description.
 */
@Composable
private fun SummaryCardSkeleton() {
    val dims = LocalDimensions.current
    val pillShape = MaterialTheme.shapes.small

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(dims.md),
            verticalArrangement = Arrangement.spacedBy(dims.sm),
        ) {
            // Title
            ShimmerBar(widthFraction = 0.6f, height = 20.dp)
            // Day N + phase name row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dims.lg),
            ) {
                ShimmerBar(widthFraction = 0.3f, height = 28.dp)
                ShimmerBar(widthFraction = 0.25f, height = 20.dp)
            }
            // Description
            ShimmerBar(widthFraction = 0.8f, height = 14.dp)
        }
    }
}

/**
 * Skeleton for section header text (e.g. "Data Readiness", "Top Insights").
 */
@Composable
private fun SectionHeaderSkeleton() {
    val dims = LocalDimensions.current

    Box(modifier = Modifier.padding(top = dims.sm)) {
        ShimmerBar(widthFraction = 0.4f, height = 16.dp)
    }
}

/**
 * Skeleton matching [DataReadinessCard] layout: label + badge row, progress bar.
 */
@Composable
private fun DataReadinessCardSkeleton() {
    val dims = LocalDimensions.current

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            ) {
                ShimmerBar(widthFraction = 0.5f, height = 14.dp)
                ShimmerBar(widthFraction = 0.2f, height = 12.dp)
            }
            // Progress bar
            ShimmerBar(widthFraction = 1f, height = 4.dp)
        }
    }
}

/**
 * Skeleton matching [AccentedInsightCard] layout: title + two body lines.
 */
@Composable
private fun InsightCardSkeleton() {
    val dims = LocalDimensions.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(dims.md),
            verticalArrangement = Arrangement.spacedBy(dims.sm),
        ) {
            ShimmerBar(widthFraction = 0.6f, height = 20.dp)
            ShimmerBar(widthFraction = 1f, height = 14.dp)
            ShimmerBar(widthFraction = 0.7f, height = 14.dp)
        }
    }
}

/**
 * Skeleton matching [ChartCard] layout: title + 200dp chart area.
 */
@Composable
private fun ChartCardSkeleton() {
    val dims = LocalDimensions.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(dims.md),
            verticalArrangement = Arrangement.spacedBy(dims.sm),
        ) {
            ShimmerBar(widthFraction = 0.5f, height = 16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.small)
                    .shimmer(),
            )
        }
    }
}

/**
 * Skeleton matching [CategoryHeader] layout: label text + chevron-sized box.
 */
@Composable
private fun CategoryHeaderSkeleton() {
    val dims = LocalDimensions.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dims.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ShimmerBar(widthFraction = 0.35f, height = 16.dp)
        Box(
            modifier = Modifier
                .height(24.dp)
                .fillMaxWidth(0.08f)
                .clip(MaterialTheme.shapes.small)
                .shimmer(),
        )
    }
}

/**
 * Reusable shimmer placeholder bar.
 *
 * @param widthFraction Fraction of parent width (0f–1f).
 * @param height        Height of the bar.
 */
@Composable
private fun ShimmerBar(
    widthFraction: Float,
    height: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(MaterialTheme.shapes.small)
            .shimmer(),
    )
}
