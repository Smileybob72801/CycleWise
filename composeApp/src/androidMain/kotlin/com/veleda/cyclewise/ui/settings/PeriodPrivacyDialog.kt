package com.veleda.cyclewise.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R

/**
 * Privacy warning dialog shown the first time the user enables the period
 * prediction reminder.
 *
 * Informs the user that enabling this reminder stores the predicted period date
 * in unencrypted local storage and that notifications may appear on the lock screen.
 * The user must explicitly accept before the reminder is activated.
 *
 * @param onAccept called when the user taps "I Understand" — should persist
 *                 [AppSettings.reminderPeriodPrivacyAccepted] and enable the reminder.
 * @param onDismiss called when the user taps "Cancel" or dismisses the dialog.
 */
@Composable
fun PeriodPrivacyDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_privacy_dialog_title)) },
        text = { Text(stringResource(R.string.reminder_privacy_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(stringResource(R.string.reminder_privacy_dialog_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reminder_privacy_dialog_cancel))
            }
        }
    )
}
