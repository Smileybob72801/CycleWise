package com.veleda.cyclewise.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

/** Number of tab placeholders to display. */
private const val TAB_COUNT = 5

/** Number of skeleton content cards below the tabs. */
private const val CONTENT_CARD_COUNT = 3

/**
 * Shimmer skeleton loader mimicking the daily log layout.
 *
 * Displays tab bar placeholders and content card skeletons with animated shimmer
 * while the daily log is loading.
 */
@Composable
internal fun DailyLogSkeletonLoader(modifier: Modifier = Modifier) {
    val dims = LocalDimensions.current
    val loadingDescription = stringResource(R.string.lottie_cd_loading)

    Column(
        modifier = modifier
            .padding(dims.md)
            .semantics { contentDescription = loadingDescription },
    ) {
        // Tab bar placeholders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dims.sm),
        ) {
            repeat(TAB_COUNT) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(dims.xs))
                        .shimmer(),
                )
            }
        }
        Spacer(Modifier.height(dims.lg))
        // Content card skeletons
        repeat(CONTENT_CARD_COUNT) {
            SkeletonContentCard()
            Spacer(Modifier.height(dims.md))
        }
    }
}

@Composable
private fun SkeletonContentCard() {
    val dims = LocalDimensions.current
    val cardShape = RoundedCornerShape(dims.sm)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .padding(dims.md),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(18.dp)
                .clip(RoundedCornerShape(dims.xs))
                .shimmer(),
        )
        Spacer(Modifier.height(dims.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(dims.xs))
                .shimmer(),
        )
    }
}
