package com.veleda.cyclewise.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.BackupMetadata
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Shows backup metadata (app version, schema version, export date) with
 * Confirm/Cancel actions. Displayed after a `.rwbackup` file is selected
 * and successfully validated.
 *
 * @param metadata    The parsed [BackupMetadata] from the backup archive.
 * @param onConfirm   Called when the user taps "Continue" to proceed with import.
 * @param onDismiss   Called when the user cancels the import.
 */
@Composable
fun BackupMetadataPreviewDialog(
    metadata: BackupMetadata,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dims = LocalDimensions.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_metadata_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dims.sm)) {
                Text(
                    stringResource(R.string.backup_metadata_app_version, metadata.appVersionName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.backup_metadata_schema_version, metadata.schemaVersion),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.backup_metadata_export_date, metadata.exportDateUtc),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.backup_metadata_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_delete_first_cancel))
            }
        },
    )
}

/**
 * Passphrase entry dialog for verifying the backup's encryption passphrase.
 *
 * The passphrase text is stored in local [remember] state — never persisted
 * to the ViewModel or disk.
 *
 * @param error        Inline error message (e.g. "Incorrect passphrase"), or `null` for no error.
 * @param isVerifying  Whether passphrase verification is in progress (shows progress indicator).
 * @param onVerify     Called with the entered passphrase when the user taps "Verify".
 * @param onDismiss    Called when the user cancels.
 */
@Composable
fun BackupPassphraseDialog(
    error: String?,
    isVerifying: Boolean,
    onVerify: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val dims = LocalDimensions.current
    var passphrase by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        title = { Text(stringResource(R.string.backup_passphrase_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dims.sm)) {
                Text(
                    stringResource(R.string.backup_passphrase_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(stringResource(R.string.backup_passphrase_label)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                stringResource(
                                    if (passwordVisible) R.string.passphrase_hide
                                    else R.string.passphrase_show,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    },
                    isError = error != null,
                    supportingText = if (error != null) {
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup-passphrase-input"),
                )

                if (isVerifying) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dims.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(stringResource(R.string.backup_passphrase_verifying))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerify(passphrase) },
                enabled = passphrase.isNotBlank() && !isVerifying,
            ) {
                Text(stringResource(R.string.backup_passphrase_verify))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isVerifying,
            ) {
                Text(stringResource(R.string.settings_delete_first_cancel))
            }
        },
    )
}

/**
 * First overwrite warning dialog: "This will replace all existing data. This cannot be undone."
 *
 * @param onConfirm Called when the user taps "Replace Data" to proceed.
 * @param onDismiss Called when the user cancels.
 */
@Composable
fun BackupOverwriteWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_overwrite_first_title)) },
        text = {
            Text(stringResource(R.string.backup_overwrite_first_body))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.backup_overwrite_first_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_delete_first_cancel))
            }
        },
    )
}

/**
 * Second overwrite confirmation dialog requiring the user to type "OVERWRITE".
 *
 * Mirrors the existing "type DELETE" pattern from the data deletion flow in SecurityPage.
 *
 * @param confirmText The current text in the confirmation field.
 * @param onTextChanged Called when the user types in the confirmation field.
 * @param onConfirm    Called when the user taps "Confirm" (enabled only when text == "OVERWRITE").
 * @param onDismiss    Called when the user cancels.
 */
@Composable
fun BackupOverwriteConfirmDialog(
    confirmText: String,
    onTextChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dims = LocalDimensions.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_overwrite_second_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dims.md)) {
                Text(stringResource(R.string.backup_overwrite_second_body))
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = onTextChanged,
                    label = { Text(stringResource(R.string.backup_overwrite_field_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup-overwrite-input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmText == "OVERWRITE",
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.backup_overwrite_first_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_delete_first_cancel))
            }
        },
    )
}

/**
 * Non-dismissable progress dialog shown during export or import operations.
 *
 * @param title The dialog title (e.g. "Exporting Backup" or "Importing Backup").
 * @param body  The dialog body text (e.g. "Creating backup archive...").
 */
@Composable
fun BackupProgressDialog(
    title: String,
    body: String,
) {
    val dims = LocalDimensions.current

    AlertDialog(
        onDismissRequest = { /* non-dismissable */ },
        title = { Text(title) },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dims.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text(body)
            }
        },
        confirmButton = { /* no buttons — non-dismissable */ },
    )
}

/**
 * Informational dialog shown when the backup's schema version exceeds the current app's.
 *
 * Tells the user to update the app before importing.
 *
 * @param backupSchemaVersion  The schema version from the backup archive.
 * @param currentSchemaVersion The current app's schema version.
 * @param onDismiss            Called when the user acknowledges.
 */
@Composable
fun BackupSchemaErrorDialog(
    backupSchemaVersion: Int,
    currentSchemaVersion: Int,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_schema_error_title)) },
        text = {
            Text(
                stringResource(
                    R.string.backup_schema_error_body,
                    backupSchemaVersion,
                    currentSchemaVersion,
                ),
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.about_close))
            }
        },
    )
}

/**
 * Generic error dialog for backup operations with a specific error message.
 *
 * @param message   The error message to display.
 * @param onDismiss Called when the user acknowledges.
 */
@Composable
fun BackupErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_error_title)) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.about_close))
            }
        },
    )
}
