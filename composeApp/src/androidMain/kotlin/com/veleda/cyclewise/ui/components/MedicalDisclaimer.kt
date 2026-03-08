package com.veleda.cyclewise.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Standard medical disclaimer banner used in educational bottom sheets and the
 * Learn section of the Insights tab.
 *
 * Renders a compact warning-style surface with an info icon and the disclaimer text
 * sourced from `R.string.medical_disclaimer_text`. Uses `errorContainer` background
 * to draw attention without being alarming.
 *
 * @param modifier Modifier applied to the outer [Surface].
 */
@Composable
fun MedicalDisclaimer(modifier: Modifier = Modifier) {
    val dims = LocalDimensions.current

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(dims.md),
            horizontalArrangement = Arrangement.spacedBy(dims.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.cd_medical_disclaimer),
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(dims.iconSm),
            )
            Text(
                text = stringResource(R.string.medical_disclaimer_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
