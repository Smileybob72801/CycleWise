package com.veleda.cyclewise.ui.log.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.ui.log.MAX_NAME_LENGTH
import com.veleda.cyclewise.ui.log.components.SectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Daily log page for symptom tracking.
 *
 * Shows a count of currently logged symptoms and a [SymptomLogger] inside a
 * [SectionCard] with an info button for educational content. Supports long-press
 * on chips to rename or delete symptoms from the library.
 *
 * @param loggedSymptoms           Symptom logs already recorded for this day.
 * @param symptomLibrary           Full list of available symptoms to choose from.
 * @param onToggleSymptom          Callback when the user toggles a symptom chip.
 * @param onCreateAndAddSymptom    Callback when the user creates and logs a new symptom by name.
 * @param onShowEducationalSheet   Callback to display educational content for the given tag.
 * @param symptomForContextMenu    Symptom whose context menu is currently shown, or null.
 * @param symptomRenaming          Symptom currently being renamed (dialog open), or null.
 * @param symptomToDelete          Symptom pending deletion confirmation, or null.
 * @param symptomDeleteLogCount    Number of logs referencing [symptomToDelete].
 * @param renameError              Inline validation error for the rename dialog, or null.
 * @param onSymptomLongPressed     Callback when the user long-presses a symptom chip.
 * @param onRenameClicked          Callback when "Rename" is selected from the context menu.
 * @param onRenameConfirmed        Callback with (id, newName) when the user confirms the rename.
 * @param onDeleteClicked          Callback when "Delete" is selected from the context menu.
 * @param onDeleteConfirmed        Callback with symptomId when the user confirms deletion.
 * @param onEditDismissed          Callback to dismiss context menu, rename dialog, or delete dialog.
 */
@Composable
internal fun SymptomsPage(
    loggedSymptoms: List<SymptomLog>,
    symptomLibrary: List<Symptom>,
    onToggleSymptom: (Symptom) -> Unit,
    onCreateAndAddSymptom: (String) -> Unit,
    onShowEducationalSheet: (String) -> Unit,
    symptomForContextMenu: Symptom? = null,
    symptomRenaming: Symptom? = null,
    symptomToDelete: Symptom? = null,
    symptomDeleteLogCount: Int = 0,
    renameError: String? = null,
    onSymptomLongPressed: (Symptom) -> Unit = {},
    onRenameClicked: (Symptom) -> Unit = {},
    onRenameConfirmed: (String, String) -> Unit = { _, _ -> },
    onDeleteClicked: (Symptom) -> Unit = {},
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

        if (loggedSymptoms.isNotEmpty()) {
            Text(
                text = stringResource(R.string.daily_log_symptoms_count, loggedSymptoms.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(
            title = stringResource(R.string.daily_log_symptoms_title),
            icon = Icons.Outlined.LocalHospital,
            onInfoClick = { onShowEducationalSheet("Symptoms") },
        ) {
            SymptomLogger(
                loggedSymptoms = loggedSymptoms,
                symptomLibrary = symptomLibrary,
                onToggleSymptom = onToggleSymptom,
                onCreateAndAddSymptom = onCreateAndAddSymptom,
                symptomForContextMenu = symptomForContextMenu,
                onSymptomLongPressed = onSymptomLongPressed,
                onRenameClicked = onRenameClicked,
                onDeleteClicked = onDeleteClicked,
                onEditDismissed = onEditDismissed,
            )
        }

        Spacer(Modifier.height(dims.xl))
    }

    // Rename dialog
    if (symptomRenaming != null) {
        RenameDialog(
            title = stringResource(R.string.library_rename_symptom_title),
            currentName = symptomRenaming.name,
            renameError = renameError,
            onConfirm = { newName -> onRenameConfirmed(symptomRenaming.id, newName) },
            onDismiss = onEditDismissed,
        )
    }

    // Delete confirmation dialog
    if (symptomToDelete != null) {
        DeleteLibraryItemDialog(
            title = stringResource(R.string.library_delete_symptom_title),
            message = if (symptomDeleteLogCount > 0) {
                stringResource(R.string.library_delete_symptom_message_with_logs, symptomDeleteLogCount)
            } else {
                stringResource(R.string.library_delete_symptom_message_no_logs)
            },
            onConfirm = { onDeleteConfirmed(symptomToDelete.id) },
            onDismiss = onEditDismissed,
        )
    }
}

/**
 * Chip-based symptom selector with long-press context menu and an inline text field
 * for creating new symptoms.
 *
 * Displays all symptoms from [symptomLibrary] as [FilterChip]s (selected state
 * reflects [loggedSymptoms]). Long-pressing a chip opens a context menu with Rename
 * and Delete options. The text field allows the user to type a custom symptom name
 * and add it via the trailing icon or the keyboard Done action.
 *
 * @param loggedSymptoms        Symptom logs already recorded for this day.
 * @param symptomLibrary        Full list of available symptoms to display as chips.
 * @param onToggleSymptom       Callback when the user toggles a symptom chip.
 * @param onCreateAndAddSymptom Callback when the user submits a new symptom name.
 * @param symptomForContextMenu Symptom whose context menu is currently shown, or null.
 * @param onSymptomLongPressed  Callback when the user long-presses a symptom chip.
 * @param onRenameClicked       Callback when "Rename" is selected from the context menu.
 * @param onDeleteClicked       Callback when "Delete" is selected from the context menu.
 * @param onEditDismissed       Callback to dismiss the context menu.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun SymptomLogger(
    loggedSymptoms: List<SymptomLog>,
    symptomLibrary: List<Symptom>,
    onToggleSymptom: (Symptom) -> Unit,
    onCreateAndAddSymptom: (String) -> Unit,
    symptomForContextMenu: Symptom? = null,
    onSymptomLongPressed: (Symptom) -> Unit = {},
    onRenameClicked: (Symptom) -> Unit = {},
    onDeleteClicked: (Symptom) -> Unit = {},
    onEditDismissed: () -> Unit = {},
) {
    var newSymptomName by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.md)) {
        if (symptomLibrary.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm),
                verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm),
            ) {
                symptomLibrary.forEach { symptom ->
                    val isSelected = loggedSymptoms.any { it.symptomId == symptom.id }
                    Box {
                        LibraryChip(
                            label = symptom.name,
                            selected = isSelected,
                            testTag = "chip-${symptom.name.uppercase()}",
                            onClick = { onToggleSymptom(symptom) },
                            onLongClick = { onSymptomLongPressed(symptom) },
                            onRenameAction = { onRenameClicked(symptom) },
                            onDeleteAction = { onDeleteClicked(symptom) },
                        )

                        if (symptomForContextMenu?.id == symptom.id) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = onEditDismissed,
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.library_rename)) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Edit, contentDescription = null)
                                    },
                                    onClick = { onRenameClicked(symptom) },
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
                                    onClick = { onDeleteClicked(symptom) },
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = newSymptomName,
            onValueChange = { if (it.length <= MAX_NAME_LENGTH) newSymptomName = it },
            label = { Text(stringResource(R.string.daily_log_add_symptom)) },
            modifier = Modifier
                .testTag("create-symptom-textbox")
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (newSymptomName.isNotBlank()) {
                    onCreateAndAddSymptom(newSymptomName)
                    newSymptomName = ""
                }
            }),
            trailingIcon = {
                IconButton(
                    onClick = {
                        onCreateAndAddSymptom(newSymptomName)
                        newSymptomName = ""
                    },
                    enabled = newSymptomName.isNotBlank(),
                    modifier = Modifier.testTag("create-symptom-button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.daily_log_create_symptom))
                }
            }
        )
    }
}

/**
 * A chip composable that supports both tap (toggle) and long-press (context menu)
 * gestures. Unlike Material3's [FilterChip], this uses [Surface] + [combinedClickable]
 * directly so that `onLongClick` is not consumed by an internal clickable handler.
 *
 * Styled to match [FilterChip] appearance: pill shape, checkmark when selected,
 * secondary-container fill when selected, and outline when unselected.
 *
 * @param label      text displayed inside the chip.
 * @param selected   whether the chip is in the selected (toggled-on) state.
 * @param testTag    test tag applied to the chip's root [Surface].
 * @param onClick    callback when the chip is tapped.
 * @param onLongClick callback when the chip is long-pressed.
 * @param onRenameAction accessibility custom action for rename.
 * @param onDeleteAction accessibility custom action for delete.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryChip(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRenameAction: () -> Unit,
    onDeleteAction: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (selected) colors.secondaryContainer else colors.surface
    val labelColor = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant
    val borderColor = if (selected) colors.secondary else colors.outline

    Surface(
        modifier = Modifier
            .testTag(testTag)
            .semantics {
                role = Role.Checkbox
                this.selected = selected
                stateDescription = if (selected) "Selected" else "Not selected"
                customActions = listOf(
                    CustomAccessibilityAction("Rename") { onRenameAction(); true },
                    CustomAccessibilityAction("Delete") { onDeleteAction(); true },
                )
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colors.onSecondaryContainer,
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
            )
        }
    }
}

/**
 * Reusable rename dialog for library items (symptoms and medications).
 *
 * Shows an [AlertDialog] with an [OutlinedTextField] pre-filled with [currentName].
 * The Save button is disabled when the name is blank or unchanged.
 *
 * @param title       dialog title (e.g. "Rename Symptom").
 * @param currentName the item's current name, pre-filled in the text field.
 * @param renameError inline validation error, or null.
 * @param onConfirm   callback with the new trimmed name.
 * @param onDismiss   callback to close the dialog.
 */
@Composable
internal fun RenameDialog(
    title: String,
    currentName: String,
    renameError: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= MAX_NAME_LENGTH) name = it },
                label = { Text(stringResource(R.string.library_rename_label)) },
                isError = renameError != null,
                supportingText = renameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name.trim() != currentName,
            ) {
                Text(stringResource(R.string.library_rename_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.library_cancel))
            }
        },
    )
}

/**
 * Reusable delete confirmation dialog for library items (symptoms and medications).
 *
 * @param title     dialog title (e.g. "Delete Symptom").
 * @param message   body text, typically including a log count warning or no-logs message.
 * @param onConfirm callback when the user confirms deletion.
 * @param onDismiss callback to close the dialog.
 */
@Composable
internal fun DeleteLibraryItemDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.library_delete_confirm))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.library_cancel))
            }
        },
    )
}
