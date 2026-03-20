package com.veleda.cyclewise.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Compact help icon button that opens a usage-tips dialog when tapped.
 *
 * Uses `Icons.AutoMirrored.Outlined.HelpOutline` with `onSurfaceVariant` tint and `iconSm`
 * sizing to sit unobtrusively beside section headers.
 *
 * @param onClick             Callback invoked when the button is tapped.
 * @param contentDescription  Accessibility label describing what the button opens.
 * @param modifier            Modifier applied to the outer [IconButton].
 */
@Composable
fun HelpButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current

    IconButton(
        onClick = onClick,
        modifier = modifier.size(dims.iconSm),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(dims.iconSm),
        )
    }
}
