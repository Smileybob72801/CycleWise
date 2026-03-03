package com.veleda.cyclewise.ui.settings.pages

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.settings.GeneralSettingsState
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.settings.components.SettingsSectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.koin.core.scope.Scope
import kotlin.math.roundToInt

/**
 * Page 0 — General: Security settings and Insights configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GeneralPage(
    state: GeneralSettingsState,
    onEvent: (SettingsEvent) -> Unit,
    session: Scope?,
    onLockNow: () -> Unit,
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

        // ── Security Card ────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_security)) {
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

            HorizontalDivider(modifier = Modifier.padding(horizontal = dims.md))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_change_passphrase)) },
                supportingContent = { Text(stringResource(R.string.settings_change_passphrase_description)) },
                modifier = Modifier.clickable(enabled = session != null) {
                    onEvent(SettingsEvent.ChangePassphraseRequested)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = dims.md))

            Button(
                enabled = session != null,
                onClick = onLockNow,
                modifier = Modifier.padding(horizontal = dims.md)
            ) {
                Text(stringResource(R.string.settings_lock_button))
            }
            if (session == null) {
                Text(
                    stringResource(R.string.settings_locked_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = dims.md)
                )
            }
        }

        // Change Passphrase dialog
        if (state.showChangePassphraseDialog) {
            ChangePassphraseDialog(state = state, onEvent = onEvent)
        }

        // ── Insights Card ────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_insights_title)) {
            Text(
                stringResource(R.string.settings_top_symptoms, state.topSymptomsCount),
                modifier = Modifier.padding(horizontal = dims.md)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.md),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (1..5).forEach { value ->
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (value == state.topSymptomsCount)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (value == state.topSymptomsCount)
                            FontWeight.Bold
                        else
                            FontWeight.Normal
                    )
                }
            }
            Slider(
                value = state.topSymptomsCount.toFloat(),
                onValueChange = { newValue ->
                    onEvent(SettingsEvent.TopSymptomsCountChanged(newValue.roundToInt()))
                },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.padding(horizontal = dims.md)
            )
        }

        // ── Tutorial Card ──────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_tutorial)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_reset_hints)) },
                supportingContent = { Text(stringResource(R.string.settings_reset_hints_description)) },
                modifier = Modifier.clickable { onEvent(SettingsEvent.ResetTutorialHints) }
            )
        }

        // Show a Toast when hints are successfully reset.
        if (state.showHintResetConfirmation) {
            val context = LocalContext.current
            val message = stringResource(R.string.settings_reset_hints_confirmation)
            LaunchedEffect(Unit) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        // ── Legal Card ───────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_legal)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_legal_privacy_policy)) },
                modifier = Modifier.clickable { onEvent(SettingsEvent.ShowPrivacyPolicyDialog) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = dims.md))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_legal_terms_of_service)) },
                modifier = Modifier.clickable { onEvent(SettingsEvent.ShowTermsOfServiceDialog) }
            )
        }

        if (state.showPrivacyPolicyDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(SettingsEvent.DismissPrivacyPolicyDialog) },
                title = { Text(stringResource(R.string.settings_legal_privacy_policy)) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(stringResource(R.string.legal_privacy_policy_body))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onEvent(SettingsEvent.DismissPrivacyPolicyDialog) }) {
                        Text(stringResource(R.string.legal_dialog_close))
                    }
                }
            )
        }

        if (state.showTermsOfServiceDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(SettingsEvent.DismissTermsOfServiceDialog) },
                title = { Text(stringResource(R.string.settings_legal_terms_of_service)) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(stringResource(R.string.legal_terms_of_service_body))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onEvent(SettingsEvent.DismissTermsOfServiceDialog) }) {
                        Text(stringResource(R.string.legal_dialog_close))
                    }
                }
            )
        }

        // ── Data Management Card ────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_data_management)) {
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(R.string.settings_delete_all_data),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_delete_all_data_description))
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                modifier = Modifier.clickable {
                    onEvent(SettingsEvent.DeleteAllDataRequested)
                }
            )
        }

        // First confirmation dialog
        if (state.showDeleteFirstConfirmation) {
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

        // Second confirmation dialog with text input
        if (state.showDeleteSecondConfirmation) {
            AlertDialog(
                onDismissRequest = { onEvent(SettingsEvent.DeleteAllDataCancelled) },
                title = { Text(stringResource(R.string.settings_delete_second_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(dims.md)) {
                        Text(stringResource(R.string.settings_delete_second_body))
                        OutlinedTextField(
                            value = state.deleteConfirmText,
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
                        enabled = state.deleteConfirmText == "DELETE",
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

        // Progress dialog while deleting
        if (state.isDeletingData) {
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

        Spacer(Modifier.height(dims.xl))
    }
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
 * [GeneralSettingsState.changePassphraseError].
 */
@Composable
private fun ChangePassphraseDialog(
    state: GeneralSettingsState,
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
    state: GeneralSettingsState,
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
