package com.veleda.cyclewise.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Dismissible dialog that displays a bulleted list of usage tips.
 *
 * Used alongside [HelpButton] to provide contextual guidance for a screen
 * or section without permanently consuming layout space.
 *
 * @param title     Dialog title (e.g. "Tracker Help").
 * @param tips      Ordered list of plain-text tips, each rendered with a leading bullet.
 * @param onDismiss Callback invoked when the user taps "Got it" or dismisses the dialog.
 */
@Composable
fun HelpDialog(
    title: String,
    tips: List<String>,
    onDismiss: () -> Unit,
) {
    val dims = LocalDimensions.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dims.sm)) {
                tips.forEach { tip ->
                    Text(
                        text = "\u2022 $tip",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.help_dialog_dismiss))
            }
        },
    )
}
