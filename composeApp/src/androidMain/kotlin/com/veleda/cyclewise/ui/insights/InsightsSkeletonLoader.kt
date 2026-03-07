package com.veleda.cyclewise.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

/** Number of skeleton cards to display. */
private const val SKELETON_CARD_COUNT = 3

/**
 * Shimmer skeleton loader mimicking the layout of insight cards.
 *
 * Displays placeholder cards with animated shimmer while insights are loading.
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
        repeat(SKELETON_CARD_COUNT) {
            SkeletonCard()
        }
    }
}

@Composable
private fun SkeletonCard() {
    val dims = LocalDimensions.current
    val cardShape = RoundedCornerShape(dims.sm)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .padding(dims.md),
    ) {
        // Title placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(20.dp)
                .clip(RoundedCornerShape(dims.xs))
                .shimmer(),
        )
        Spacer(Modifier.height(dims.sm))
        // Body line 1
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(dims.xs))
                .shimmer(),
        )
        Spacer(Modifier.height(dims.xs))
        // Body line 2
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .clip(RoundedCornerShape(dims.xs))
                .shimmer(),
        )
        Spacer(Modifier.height(dims.xs))
        // Body line 3
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(14.dp)
                .clip(RoundedCornerShape(dims.xs))
                .shimmer(),
        )
    }
}
