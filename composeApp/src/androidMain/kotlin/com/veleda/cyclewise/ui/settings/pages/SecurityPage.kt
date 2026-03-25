package com.veleda.cyclewise.ui.settings.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.backup.BackupErrorDialog
import com.veleda.cyclewise.ui.backup.BackupMetadataPreviewDialog
import com.veleda.cyclewise.ui.backup.BackupOverwriteConfirmDialog
import com.veleda.cyclewise.ui.backup.BackupOverwriteWarningDialog
import com.veleda.cyclewise.ui.backup.BackupPassphraseDialog
import com.veleda.cyclewise.ui.backup.BackupProgressDialog
import com.veleda.cyclewise.ui.backup.BackupSchemaErrorDialog
import com.veleda.cyclewise.ui.backup.ImportStep
import com.veleda.cyclewise.ui.settings.SecuritySettingsState
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.settings.components.SettingsSectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Page 0 — Security: Autolock timeout, passphrase management, and data deletion.
 */
// Settings page with four section cards and import dialog flow
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SecurityPage(
    state: SecuritySettingsState,
    onEvent: (SettingsEvent) -> Unit,
    isSessionActive: Boolean,
    onLockNow: () -> Unit,
    onExportClicked: () -> Unit,
    onImportClicked: () -> Unit,
) {
    val dims = LocalDimensions.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.md),
        verticalArrangement = Arrangement.spacedBy(dims.md)
    ) {
        Spacer(Modifier.height(dims.sm))

        // ── Session Card ──────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_session)) {
            Text(
                stringResource(R.string.settings_autolock_title),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = dims.md)
            )

            val options = listOf(5, 10, 15, 30)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.md)
            ) {
                options.forEachIndexed { index, minutes ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        onClick = { onEvent(SettingsEvent.AutolockChanged(minutes)) },
                        selected = state.autolockMinutes == minutes,
                        label = { Text("$minutes ${stringResource(R.string.settings_autolock_minutes_unit)}") }
                    )
                }
            }

            Spacer(Modifier.height(dims.sm))

            Button(
                enabled = isSessionActive,
                onClick = onLockNow,
                modifier = Modifier.padding(horizontal = dims.md)
            ) {
                Text(stringResource(R.string.settings_lock_button))
            }
            if (!isSessionActive) {
                Text(
                    stringResource(R.string.settings_locked_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = dims.md)
                )
            }
        }

        // ── Passphrase Card ───────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_passphrase)) {
            Text(
                stringResource(R.string.settings_change_passphrase_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = dims.md)
            )
            Spacer(Modifier.height(dims.sm))
            FilledTonalButton(
                enabled = isSessionActive,
                onClick = { onEvent(SettingsEvent.ChangePassphraseRequested) },
                modifier = Modifier.padding(horizontal = dims.md)
            ) {
                Text(stringResource(R.string.settings_change_passphrase))
            }
        }

        // Change Passphrase dialog
        if (state.showChangePassphraseDialog) {
            ChangePassphraseDialog(state = state, onEvent = onEvent)
        }

        // ── Backup & Restore Card ─────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_backup)) {
            Text(
                stringResource(R.string.settings_backup_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = dims.md)
            )
            Spacer(Modifier.height(dims.sm))
            FilledTonalButton(
                enabled = isSessionActive && !state.isExporting,
                onClick = onExportClicked,
                modifier = Modifier.padding(horizontal = dims.md)
            ) {
                Text(stringResource(R.string.settings_export_backup))
            }
            Spacer(Modifier.height(dims.sm))
            FilledTonalButton(
                onClick = onImportClicked,
                modifier = Modifier.padding(horizontal = dims.md)
            ) {
                Text(stringResource(R.string.settings_import_backup))
            }
        }

        // Export progress dialog
        if (state.isExporting) {
            BackupProgressDialog(
                title = stringResource(R.string.backup_progress_exporting_title),
                body = stringResource(R.string.backup_progress_exporting_body),
            )
        }

        // Export error dialog
        state.exportError?.let { error ->
            BackupErrorDialog(
                message = stringResource(R.string.settings_export_error, error),
                onDismiss = { onEvent(SettingsEvent.ImportDismissed) },
            )
        }

        // Import dialog flow driven by importStep
        when (state.importStep) {
            ImportStep.METADATA_PREVIEW -> {
                state.importMetadata?.let { metadata ->
                    BackupMetadataPreviewDialog(
                        metadata = metadata,
                        onConfirm = { onEvent(SettingsEvent.ImportMetadataConfirmed) },
                        onDismiss = { onEvent(SettingsEvent.ImportDismissed) },
                    )
                }
            }

            ImportStep.PASSPHRASE_ENTRY -> {
                BackupPassphraseDialog(
                    error = if (state.importPassphraseError == "wrong_passphrase") {
                        stringResource(R.string.backup_passphrase_error_wrong)
                    } else {
                        state.importPassphraseError
                    },
                    isVerifying = state.isVerifyingPassphrase,
                    onVerify = { onEvent(SettingsEvent.ImportPassphraseEntered(it)) },
                    onDismiss = { onEvent(SettingsEvent.ImportDismissed) },
                )
            }

            ImportStep.FIRST_WARNING -> {
                BackupOverwriteWarningDialog(
                    onConfirm = { onEvent(SettingsEvent.ImportFirstWarningConfirmed) },
                    onDismiss = { onEvent(SettingsEvent.ImportDismissed) },
                )
            }

            ImportStep.SECOND_CONFIRM -> {
                BackupOverwriteConfirmDialog(
                    confirmText = state.importConfirmText,
                    onTextChanged = { onEvent(SettingsEvent.ImportConfirmTextChanged(it)) },
                    onConfirm = { onEvent(SettingsEvent.ImportSecondConfirmed) },
                    onDismiss = { onEvent(SettingsEvent.ImportDismissed) },
                )
            }

            ImportStep.IMPORTING -> {
                BackupProgressDialog(
                    title = stringResource(R.string.backup_progress_importing_title),
                    body = stringResource(R.string.backup_progress_importing_body),
                )
            }

            ImportStep.IDLE -> { /* no dialog */ }
        }

        // Schema error dialog (shown when importError starts with "schema_too_new:")
        state.importError?.let { error ->
            if (error.startsWith("schema_too_new:")) {
                val parts = error.split(":")
                val backupVersion = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val currentVersion = parts.getOrNull(2)?.toIntOrNull() ?: 0
                BackupSchemaErrorDialog(
                    backupSchemaVersion = backupVersion,
                    currentSchemaVersion = currentVersion,
                    onDismiss = { onEvent(SettingsEvent.ImportDismissed) },
                )
            } else {
                BackupErrorDialog(
                    message = error,
                    onDismiss = { onEvent(SettingsEvent.ImportDismissed) },
                )
            }
        }

        // ── Data Management Card ──────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_data_management)) {
            Text(
                stringResource(R.string.settings_delete_all_data_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = dims.md)
            )
            Spacer(Modifier.height(dims.sm))
            Button(
                onClick = { onEvent(SettingsEvent.DeleteAllDataRequested) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.padding(horizontal = dims.md)
            ) {
                Text(stringResource(R.string.settings_delete_all_data))
            }
        }

        // Delete data confirmation dialogs
        if (state.showDeleteFirstConfirmation) {
            DeleteFirstConfirmDialog(onEvent = onEvent)
        }

        if (state.showDeleteSecondConfirmation) {
            DeleteSecondConfirmDialog(
                deleteConfirmText = state.deleteConfirmText,
                onEvent = onEvent,
            )
        }

        if (state.isDeletingData) {
            DeleteProgressDialog()
        }

        Spacer(Modifier.height(dims.xl))
    }
}

/**
 * First step of the data deletion flow — a simple confirmation dialog
 * asking "Are you sure you want to delete all data?"
 *
 * @param onEvent Event dispatcher for [SettingsEvent] variants.
 */
@Composable
private fun DeleteFirstConfirmDialog(onEvent: (SettingsEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(SettingsEvent.DeleteAllDataCancelled) },
        title = { Text(stringResource(R.string.settings_delete_first_title)) },
        text = { Text(stringResource(R.string.settings_delete_first_body)) },
        confirmButton = {
            Button(
                onClick = { onEvent(SettingsEvent.DeleteAllDataFirstConfirmed) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.settings_delete_first_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(SettingsEvent.DeleteAllDataCancelled) }) {
                Text(stringResource(R.string.settings_delete_first_cancel))
            }
        }
    )
}

/**
 * Second step of the data deletion flow — requires the user to type "DELETE"
 * before the confirm button is enabled.
 *
 * @param deleteConfirmText Current text in the confirmation field.
 * @param onEvent           Event dispatcher for [SettingsEvent] variants.
 */
@Composable
private fun DeleteSecondConfirmDialog(
    deleteConfirmText: String,
    onEvent: (SettingsEvent) -> Unit,
) {
    val dims = LocalDimensions.current

    AlertDialog(
        onDismissRequest = { onEvent(SettingsEvent.DeleteAllDataCancelled) },
        title = { Text(stringResource(R.string.settings_delete_second_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dims.md)) {
                Text(stringResource(R.string.settings_delete_second_body))
                OutlinedTextField(
                    value = deleteConfirmText,
                    onValueChange = { onEvent(SettingsEvent.DeleteConfirmTextChanged(it)) },
                    label = { Text(stringResource(R.string.settings_delete_second_field_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onEvent(SettingsEvent.DeleteAllDataConfirmed) },
                enabled = deleteConfirmText == "DELETE",
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.settings_delete_first_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(SettingsEvent.DeleteAllDataCancelled) }) {
                Text(stringResource(R.string.settings_delete_first_cancel))
            }
        }
    )
}

/**
 * Non-dismissable progress dialog shown while data deletion is in progress.
 */
@Composable
private fun DeleteProgressDialog() {
    val dims = LocalDimensions.current

    AlertDialog(
        onDismissRequest = { /* non-dismissable */ },
        title = { Text(stringResource(R.string.settings_delete_progress_title)) },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dims.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.settings_delete_progress_body))
            }
        },
        confirmButton = { /* no buttons — non-dismissable */ }
    )
}

/**
 * A password text field with visibility toggle, used within the change passphrase dialog.
 */
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    isError: Boolean,
    errorText: String?,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None
        else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = onToggleVisibility) {
                Text(
                    stringResource(if (visible) R.string.passphrase_hide else R.string.passphrase_show),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        isError = isError,
        supportingText = if (errorText != null) {
            { Text(errorText, color = MaterialTheme.colorScheme.error) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Steps in the change-passphrase dialog flow. */
private enum class PassphraseDialogStep { INPUT, CONFIRM, SUCCESS }

/**
 * Three-step dialog for changing the database encryption passphrase.
 *
 * 1. **INPUT** — Three password fields with local validation.
 * 2. **CONFIRM** — "Are you sure?" warning before executing the rekey.
 * 3. **SUCCESS** — Non-dismissable confirmation showing the new passphrase one last time.
 *
 * Passphrase text values are stored in local `remember` state (not in the ViewModel)
 * so they are never persisted and are discarded when the dialog is dismissed.
 *
 * Error feedback from the ViewModel is shown inline on the relevant field via
 * [SecuritySettingsState.changePassphraseError].
 */
@Composable
private fun ChangePassphraseDialog(
    state: SecuritySettingsState,
    onEvent: (SettingsEvent) -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var newPassphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(PassphraseDialogStep.INPUT) }

    // If the ViewModel reports an error while on the CONFIRM step, reset to INPUT
    LaunchedEffect(state.changePassphraseError) {
        if (state.changePassphraseError != null && step == PassphraseDialogStep.CONFIRM) {
            step = PassphraseDialogStep.INPUT
        }
    }

    // Transition to SUCCESS when the ViewModel signals verified success
    LaunchedEffect(state.showPassphraseSuccessDialog) {
        if (state.showPassphraseSuccessDialog) {
            step = PassphraseDialogStep.SUCCESS
        }
    }

    when (step) {
        PassphraseDialogStep.INPUT -> {
            val submitEnabled = current.isNotBlank()
                    && newPassphrase.length >= 8
                    && confirmation.isNotBlank()
                    && !state.isChangingPassphrase

            AlertDialog(
                onDismissRequest = {
                    if (!state.isChangingPassphrase) onEvent(SettingsEvent.ChangePassphraseDismissed)
                },
                title = { Text(stringResource(R.string.settings_change_passphrase_title)) },
                text = {
                    ChangePassphraseFields(
                        state = state,
                        current = current,
                        onCurrentChange = { current = it },
                        newPassphrase = newPassphrase,
                        onNewPassphraseChange = { newPassphrase = it },
                        confirmation = confirmation,
                        onConfirmationChange = { confirmation = it },
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { step = PassphraseDialogStep.CONFIRM },
                        enabled = submitEnabled,
                    ) {
                        Text(stringResource(R.string.settings_change_passphrase_submit))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onEvent(SettingsEvent.ChangePassphraseDismissed) },
                        enabled = !state.isChangingPassphrase,
                    ) {
                        Text(stringResource(R.string.settings_delete_first_cancel))
                    }
                }
            )
        }

        PassphraseDialogStep.CONFIRM -> {
            AlertDialog(
                onDismissRequest = {
                    if (!state.isChangingPassphrase) step = PassphraseDialogStep.INPUT
                },
                title = { Text(stringResource(R.string.settings_change_passphrase_confirm_title)) },
                text = {
                    val dims = LocalDimensions.current
                    Column(verticalArrangement = Arrangement.spacedBy(dims.md)) {
                        Text(stringResource(R.string.settings_change_passphrase_confirm_body))
                        if (state.isChangingPassphrase) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(dims.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text(stringResource(R.string.settings_change_passphrase_progress))
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onEvent(
                                SettingsEvent.ChangePassphraseSubmitted(
                                    current, newPassphrase, confirmation,
                                )
                            )
                        },
                        enabled = !state.isChangingPassphrase,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(stringResource(R.string.settings_change_passphrase_confirm_button))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { step = PassphraseDialogStep.INPUT },
                        enabled = !state.isChangingPassphrase,
                    ) {
                        Text(stringResource(R.string.settings_change_passphrase_back))
                    }
                }
            )
        }

        PassphraseDialogStep.SUCCESS -> {
            var passphraseVisible by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { /* non-dismissable */ },
                title = { Text(stringResource(R.string.settings_change_passphrase_success_title)) },
                text = {
                    val dims = LocalDimensions.current
                    Column(verticalArrangement = Arrangement.spacedBy(dims.md)) {
                        Text(
                            stringResource(R.string.settings_change_passphrase_success_warning),
                            color = MaterialTheme.colorScheme.error,
                        )
                        OutlinedTextField(
                            value = newPassphrase,
                            onValueChange = { /* read-only */ },
                            readOnly = true,
                            label = { Text(stringResource(R.string.settings_change_passphrase_new_label)) },
                            visualTransformation = if (passphraseVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { passphraseVisible = !passphraseVisible }) {
                                    Text(
                                        stringResource(
                                            if (passphraseVisible) R.string.passphrase_hide
                                            else R.string.passphrase_show
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onEvent(SettingsEvent.ChangePassphraseSuccessAcknowledged)
                        },
                    ) {
                        Text(stringResource(R.string.settings_change_passphrase_success_acknowledge))
                    }
                },
            )
        }
    }
}

/** The three password fields, general error display, and optional progress indicator. */
@Composable
private fun ChangePassphraseFields(
    state: SecuritySettingsState,
    current: String,
    onCurrentChange: (String) -> Unit,
    newPassphrase: String,
    onNewPassphraseChange: (String) -> Unit,
    confirmation: String,
    onConfirmationChange: (String) -> Unit,
) {
    val dims = LocalDimensions.current
    val error = state.changePassphraseError
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(dims.sm)) {
        PasswordField(
            value = current,
            onValueChange = onCurrentChange,
            label = stringResource(R.string.settings_change_passphrase_current_label),
            visible = currentVisible,
            onToggleVisibility = { currentVisible = !currentVisible },
            isError = error == "wrong_current",
            errorText = if (error == "wrong_current")
                stringResource(R.string.settings_change_passphrase_error_wrong_current) else null,
        )
        PasswordField(
            value = newPassphrase,
            onValueChange = onNewPassphraseChange,
            label = stringResource(R.string.settings_change_passphrase_new_label),
            visible = newVisible,
            onToggleVisibility = { newVisible = !newVisible },
            isError = error == "too_short",
            errorText = if (error == "too_short")
                stringResource(R.string.settings_change_passphrase_error_too_short) else null,
        )
        PasswordField(
            value = confirmation,
            onValueChange = onConfirmationChange,
            label = stringResource(R.string.settings_change_passphrase_confirm_label),
            visible = confirmVisible,
            onToggleVisibility = { confirmVisible = !confirmVisible },
            isError = error == "mismatch",
            errorText = if (error == "mismatch")
                stringResource(R.string.settings_change_passphrase_error_mismatch) else null,
        )

        // General (non-field-specific) errors
        if (error == "failed") {
            Text(
                stringResource(R.string.settings_change_passphrase_error_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (error == "verification_failed") {
            Text(
                stringResource(R.string.settings_change_passphrase_error_verification_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (state.isChangingPassphrase) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dims.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.settings_change_passphrase_progress))
            }
        }
    }
}
