package com.veleda.cyclewise.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Displays the source attribution for an educational article.
 *
 * Shows the [sourceName] (e.g. "Office on Women's Health") followed by a
 * public-domain notice line. Both lines use muted `onSurfaceVariant` colour
 * to indicate metadata rather than primary content.
 *
 * @param sourceName The human-readable name of the content source.
 * @param modifier   Modifier applied to the outer [Column].
 */
@Composable
fun SourceAttribution(
    sourceName: String,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dims.xs),
    ) {
        Text(
            text = stringResource(R.string.source_attribution_prefix, sourceName),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.source_public_domain_notice),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
