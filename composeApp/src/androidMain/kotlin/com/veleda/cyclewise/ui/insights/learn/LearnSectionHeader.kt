package com.veleda.cyclewise.ui.insights.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Header for the Learn section with title and subtitle.
 *
 * Wrapped in a [Surface] with background color so it visually separates
 * from the insight cards above during scrolling.
 */
@Composable
internal fun LearnSectionHeader(modifier: Modifier = Modifier) {
    val dims = LocalDimensions.current

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = dims.sm),
            verticalArrangement = Arrangement.spacedBy(dims.xs)
        ) {
            Text(
                text = stringResource(R.string.insights_learn_header),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.insights_learn_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
