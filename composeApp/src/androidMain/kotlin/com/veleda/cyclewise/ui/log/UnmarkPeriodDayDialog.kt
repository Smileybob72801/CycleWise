package com.veleda.cyclewise.ui.log

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R

/**
 * Confirmation dialog shown in the DailyLog screen when the user toggles the period
 * switch off on a day that has logged flow, color, or consistency data.
 *
 * @param showDialog Whether the dialog is visible.
 * @param onConfirm Called when the user confirms removal.
 * @param onDismiss Called when the user cancels or dismisses the dialog.
 */
@Composable
internal fun UnmarkPeriodDayDialog(
    showDialog: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.unmark_period_day_title)) },
            text = { Text(stringResource(R.string.unmark_period_day_message)) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.unmark_period_day_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.tracker_cancel))
                }
            },
        )
    }
}
