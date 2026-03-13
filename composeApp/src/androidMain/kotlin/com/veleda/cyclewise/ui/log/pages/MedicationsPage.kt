package com.veleda.cyclewise.ui.log.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.ui.log.MAX_NAME_LENGTH
import com.veleda.cyclewise.ui.log.components.SectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Daily log page for medication tracking.
 *
 * Shows a count of currently logged medications and a [MedicationLogger] inside
 * a [SectionCard] with an info button for educational content. Supports long-press
 * on chips to rename or delete medications from the library.
 *
 * @param loggedMedications          Medication logs already recorded for this day.
 * @param medicationLibrary          Full list of available medications to choose from.
 * @param onToggleMedication         Callback when the user toggles a medication chip.
 * @param onCreateAndAddMedication   Callback when the user creates and logs a new medication by name.
 * @param onShowEducationalSheet     Callback to display educational content for the given tag.
 * @param medicationForContextMenu   Medication whose context menu is currently shown, or null.
 * @param medicationRenaming         Medication currently being renamed (dialog open), or null.
 * @param medicationToDelete         Medication pending deletion confirmation, or null.
 * @param medicationDeleteLogCount   Number of logs referencing [medicationToDelete].
 * @param renameError                Inline validation error for the rename dialog, or null.
 * @param onMedicationLongPressed    Callback when the user long-presses a medication chip.
 * @param onRenameClicked            Callback when "Rename" is selected from the context menu.
 * @param onRenameConfirmed          Callback with (id, newName) when the user confirms the rename.
 * @param onDeleteClicked            Callback when "Delete" is selected from the context menu.
 * @param onDeleteConfirmed          Callback with medicationId when the user confirms deletion.
 * @param onEditDismissed            Callback to dismiss context menu, rename dialog, or delete dialog.
 */
@Composable
internal fun MedicationsPage(
    loggedMedications: List<MedicationLog>,
    medicationLibrary: List<Medication>,
    onToggleMedication: (Medication) -> Unit,
    onCreateAndAddMedication: (String) -> Unit,
    onShowEducationalSheet: (String) -> Unit,
    medicationForContextMenu: Medication? = null,
    medicationRenaming: Medication? = null,
    medicationToDelete: Medication? = null,
    medicationDeleteLogCount: Int = 0,
    renameError: String? = null,
    onMedicationLongPressed: (Medication) -> Unit = {},
    onRenameClicked: (Medication) -> Unit = {},
    onRenameConfirmed: (String, String) -> Unit = { _, _ -> },
    onDeleteClicked: (Medication) -> Unit = {},
    onDeleteConfirmed: (String) -> Unit = {},
    onEditDismissed: () -> Unit = {},
) {
    val dims = LocalDimensions.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.md),
        verticalArrangement = Arrangement.spacedBy(dims.md),
    ) {
        Spacer(Modifier.height(dims.sm))

        if (loggedMedications.isNotEmpty()) {
            Text(
                text = stringResource(R.string.daily_log_medications_count, loggedMedications.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(
            title = stringResource(R.string.daily_log_medications_title),
            icon = Icons.Outlined.MedicalServices,
            onInfoClick = { onShowEducationalSheet("Medication") },
        ) {
            MedicationLogger(
                loggedMedications = loggedMedications,
                medicationLibrary = medicationLibrary,
                onToggleMedication = onToggleMedication,
                onCreateAndAddMedication = onCreateAndAddMedication,
                medicationForContextMenu = medicationForContextMenu,
                onMedicationLongPressed = onMedicationLongPressed,
                onRenameClicked = onRenameClicked,
                onDeleteClicked = onDeleteClicked,
                onEditDismissed = onEditDismissed,
            )
        }

        Spacer(Modifier.height(dims.xl))
    }

    // Rename dialog
    if (medicationRenaming != null) {
        RenameDialog(
            title = stringResource(R.string.library_rename_medication_title),
            currentName = medicationRenaming.name,
            renameError = renameError,
            onConfirm = { newName -> onRenameConfirmed(medicationRenaming.id, newName) },
            onDismiss = onEditDismissed,
        )
    }

    // Delete confirmation dialog
    if (medicationToDelete != null) {
        DeleteLibraryItemDialog(
            title = stringResource(R.string.library_delete_medication_title),
            message = if (medicationDeleteLogCount > 0) {
                stringResource(R.string.library_delete_medication_message_with_logs, medicationDeleteLogCount)
            } else {
                stringResource(R.string.library_delete_medication_message_no_logs)
            },
            onConfirm = { onDeleteConfirmed(medicationToDelete.id) },
            onDismiss = onEditDismissed,
        )
    }
}

/**
 * Chip-based medication selector with long-press context menu and an inline text field
 * for creating new medications.
 *
 * Displays all medications from [medicationLibrary] as [FilterChip]s (selected
 * state reflects [loggedMedications]). Long-pressing a chip opens a context menu
 * with Rename and Delete options. The text field allows the user to type a custom
 * medication name and add it via the trailing icon or the keyboard Done action.
 *
 * @param loggedMedications        Medication logs already recorded for this day.
 * @param medicationLibrary        Full list of available medications to display as chips.
 * @param onToggleMedication       Callback when the user toggles a medication chip.
 * @param onCreateAndAddMedication Callback when the user submits a new medication name.
 * @param medicationForContextMenu Medication whose context menu is currently shown, or null.
 * @param onMedicationLongPressed  Callback when the user long-presses a medication chip.
 * @param onRenameClicked          Callback when "Rename" is selected from the context menu.
 * @param onDeleteClicked          Callback when "Delete" is selected from the context menu.
 * @param onEditDismissed          Callback to dismiss the context menu.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MedicationLogger(
    loggedMedications: List<MedicationLog>,
    medicationLibrary: List<Medication>,
    onToggleMedication: (Medication) -> Unit,
    onCreateAndAddMedication: (String) -> Unit,
    medicationForContextMenu: Medication? = null,
    onMedicationLongPressed: (Medication) -> Unit = {},
    onRenameClicked: (Medication) -> Unit = {},
    onDeleteClicked: (Medication) -> Unit = {},
    onEditDismissed: () -> Unit = {},
) {
    var newMedicationName by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.md)) {
        if (medicationLibrary.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm),
                verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm),
            ) {
                medicationLibrary.forEach { medication ->
                    val isSelected = loggedMedications.any { it.medicationId == medication.id }
                    Box {
                        LibraryChip(
                            label = medication.name,
                            selected = isSelected,
                            testTag = "chip-${medication.name.uppercase()}",
                            onClick = { onToggleMedication(medication) },
                            onLongClick = { onMedicationLongPressed(medication) },
                            onRenameAction = { onRenameClicked(medication) },
                            onDeleteAction = { onDeleteClicked(medication) },
                        )

                        if (medicationForContextMenu?.id == medication.id) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = onEditDismissed,
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.library_rename)) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Edit, contentDescription = null)
                                    },
                                    onClick = { onRenameClicked(medication) },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.library_delete),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = { onDeleteClicked(medication) },
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = newMedicationName,
            onValueChange = { if (it.length <= MAX_NAME_LENGTH) newMedicationName = it },
            label = { Text(stringResource(R.string.daily_log_add_medication)) },
            modifier = Modifier
                .testTag("create-medication-textbox")
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                onCreateAndAddMedication(newMedicationName)
                newMedicationName = ""
            }),
            trailingIcon = {
                IconButton(
                    onClick = {
                        onCreateAndAddMedication(newMedicationName)
                        newMedicationName = ""
                    },
                    enabled = newMedicationName.isNotBlank(),
                    modifier = Modifier.testTag("create-medication-button"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.daily_log_create_medication))
                }
            }
        )
    }
}
