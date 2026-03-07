package com.veleda.cyclewise.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.components.LottieAnimationBox
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Rich empty state displayed when no insights have been generated yet.
 *
 * Shows a Lottie animation, a heading, and explanatory body text
 * to guide the user toward tracking their cycle.
 */
@Composable
internal fun InsightsEmptyState(modifier: Modifier = Modifier) {
    val dims = LocalDimensions.current

    Column(
        modifier = modifier.padding(horizontal = dims.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dims.sm)
    ) {
        LottieAnimationBox(
            animationResId = R.raw.anim_empty_insights,
            modifier = Modifier.size(dims.iconLg),
            contentDescription = stringResource(R.string.lottie_cd_empty_insights),
        )
        Text(
            text = stringResource(R.string.insights_empty_heading),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.insights_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
