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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
