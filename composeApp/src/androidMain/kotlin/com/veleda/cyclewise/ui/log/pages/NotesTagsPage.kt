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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.veleda.cyclewise.ui.components.HelpDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.CustomTag
import com.veleda.cyclewise.domain.models.CustomTagLog
import com.veleda.cyclewise.ui.log.MAX_NAME_LENGTH
import com.veleda.cyclewise.ui.log.MAX_NOTE_LENGTH
import com.veleda.cyclewise.ui.log.components.SectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Daily log page for custom tags and free-text notes.
 *
 * Renders a [CustomTagLogger] for toggling/creating/editing tags and a [NoteEditor]
 * for free-form notes, each inside a [SectionCard]. Supports long-press on tag chips
 * to rename or delete tags from the library.
 *
 * @param loggedCustomTags         Custom tag logs already recorded for this day.
 * @param customTagLibrary         Full list of available custom tags to choose from.
 * @param onToggleCustomTag        Callback when the user toggles a custom tag chip.
 * @param onCreateAndAddCustomTag  Callback when the user creates and logs a new tag by name.
 * @param note                     Current note text.
 * @param onNoteChanged            Callback when the note text changes.
 * @param onDone                   Callback when the user taps the "Done" button to return to the Tracker.
 * @param customTagForContextMenu  Custom tag whose context menu is currently shown, or null.
 * @param customTagRenaming        Custom tag currently being renamed (dialog open), or null.
 * @param customTagToDelete        Custom tag pending deletion confirmation, or null.
 * @param customTagDeleteLogCount  Number of logs referencing [customTagToDelete].
 * @param renameError              Inline validation error for the rename dialog, or null.
 * @param onCustomTagLongPressed   Callback when the user long-presses a custom tag chip.
 * @param onRenameClicked          Callback when "Rename" is selected from the context menu.
 * @param onRenameConfirmed        Callback with (id, newName) when the user confirms the rename.
 * @param onDeleteClicked          Callback when "Delete" is selected from the context menu.
 * @param onDeleteConfirmed        Callback with tagId when the user confirms deletion.
 * @param onEditDismissed          Callback to dismiss context menu, rename dialog, or delete dialog.
 */
@Composable
internal fun NotesTagsPage(
    loggedCustomTags: List<CustomTagLog>,
    customTagLibrary: List<CustomTag>,
    onToggleCustomTag: (CustomTag) -> Unit,
    onCreateAndAddCustomTag: (String) -> Unit,
    note: String,
    onNoteChanged: (String) -> Unit,
    onDone: () -> Unit = {},
    customTagForContextMenu: CustomTag? = null,
    customTagRenaming: CustomTag? = null,
    customTagToDelete: CustomTag? = null,
    customTagDeleteLogCount: Int = 0,
    renameError: String? = null,
    onCustomTagLongPressed: (CustomTag) -> Unit = {},
    onRenameClicked: (CustomTag) -> Unit = {},
    onRenameConfirmed: (String, String) -> Unit = { _, _ -> },
    onDeleteClicked: (CustomTag) -> Unit = {},
    onDeleteConfirmed: (String) -> Unit = {},
    onEditDismissed: () -> Unit = {},
) {
    val dims = LocalDimensions.current
    var showHelp by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.md),
        verticalArrangement = Arrangement.spacedBy(dims.md),
    ) {
        Spacer(Modifier.height(dims.sm))

        if (loggedCustomTags.isNotEmpty()) {
            Text(
                text = stringResource(R.string.daily_log_custom_tags_count, loggedCustomTags.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(
            title = stringResource(R.string.daily_log_custom_tags_title),
            icon = Icons.AutoMirrored.Outlined.Notes,
            onHelpClick = { showHelp = true },
        ) {
            CustomTagLogger(
                loggedCustomTags = loggedCustomTags,
                customTagLibrary = customTagLibrary,
                onToggleCustomTag = onToggleCustomTag,
                onCreateAndAddCustomTag = onCreateAndAddCustomTag,
                customTagForContextMenu = customTagForContextMenu,
                onCustomTagLongPressed = onCustomTagLongPressed,
                onRenameClicked = onRenameClicked,
                onDeleteClicked = onDeleteClicked,
                onEditDismissed = onEditDismissed,
            )
        }

        SectionCard(
            title = stringResource(R.string.daily_log_notes_title),
            icon = Icons.AutoMirrored.Outlined.Notes,
        ) {
            NoteEditor(
                note = note,
                onNoteChanged = onNoteChanged,
            )
        }

        FilledTonalButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.daily_log_done_button))
        }

        Spacer(Modifier.height(dims.xl))
    }

    // Rename dialog
    if (customTagRenaming != null) {
        RenameDialog(
            title = stringResource(R.string.library_rename_custom_tag_title),
            currentName = customTagRenaming.name,
            renameError = renameError,
            onConfirm = { newName -> onRenameConfirmed(customTagRenaming.id, newName) },
            onDismiss = onEditDismissed,
        )
    }

    // Delete confirmation dialog
    if (customTagToDelete != null) {
        DeleteLibraryItemDialog(
            title = stringResource(R.string.library_delete_custom_tag_title),
            message = if (customTagDeleteLogCount > 0) {
                stringResource(R.string.library_delete_custom_tag_message_with_logs, customTagDeleteLogCount)
            } else {
                stringResource(R.string.library_delete_custom_tag_message_no_logs)
            },
            onConfirm = { onDeleteConfirmed(customTagToDelete.id) },
            onDismiss = onEditDismissed,
        )
    }

    if (showHelp) {
        HelpDialog(
            title = stringResource(R.string.help_notes_title),
            tips = listOf(
                stringResource(R.string.help_notes_tip_toggle),
                stringResource(R.string.help_notes_tip_create),
                stringResource(R.string.help_notes_tip_edit),
                stringResource(R.string.help_notes_tip_freeform),
            ),
            onDismiss = { showHelp = false },
        )
    }
}

/**
 * Chip-based custom tag selector with long-press context menu and an inline text field
 * for creating new tags.
 *
 * Displays all custom tags from [customTagLibrary] as [LibraryChip]s (selected
 * state reflects [loggedCustomTags]). Long-pressing a chip opens a context menu
 * with Rename and Delete options. The text field allows the user to type a custom
 * tag name and add it via the trailing icon or the keyboard Done action.
 *
 * @param loggedCustomTags        Custom tag logs already recorded for this day.
 * @param customTagLibrary        Full list of available custom tags to display as chips.
 * @param onToggleCustomTag       Callback when the user toggles a custom tag chip.
 * @param onCreateAndAddCustomTag Callback when the user submits a new tag name.
 * @param customTagForContextMenu Custom tag whose context menu is currently shown, or null.
 * @param onCustomTagLongPressed  Callback when the user long-presses a custom tag chip.
 * @param onRenameClicked         Callback when "Rename" is selected from the context menu.
 * @param onDeleteClicked         Callback when "Delete" is selected from the context menu.
 * @param onEditDismissed         Callback to dismiss the context menu.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomTagLogger(
    loggedCustomTags: List<CustomTagLog>,
    customTagLibrary: List<CustomTag>,
    onToggleCustomTag: (CustomTag) -> Unit,
    onCreateAndAddCustomTag: (String) -> Unit,
    customTagForContextMenu: CustomTag? = null,
    onCustomTagLongPressed: (CustomTag) -> Unit = {},
    onRenameClicked: (CustomTag) -> Unit = {},
    onDeleteClicked: (CustomTag) -> Unit = {},
    onEditDismissed: () -> Unit = {},
) {
    var newTagName by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.md)) {
        if (customTagLibrary.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm),
                verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm),
            ) {
                customTagLibrary.forEach { tag ->
                    val isSelected = loggedCustomTags.any { it.tagId == tag.id }
                    Box {
                        LibraryChip(
                            label = tag.name,
                            selected = isSelected,
                            testTag = "chip-${tag.name.uppercase()}",
                            onClick = { onToggleCustomTag(tag) },
                            onLongClick = { onCustomTagLongPressed(tag) },
                            onRenameAction = { onRenameClicked(tag) },
                            onDeleteAction = { onDeleteClicked(tag) },
                        )

                        if (customTagForContextMenu?.id == tag.id) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = onEditDismissed,
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.library_rename)) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Edit, contentDescription = null)
                                    },
                                    onClick = { onRenameClicked(tag) },
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
                                    onClick = { onDeleteClicked(tag) },
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = newTagName,
            onValueChange = { if (it.length <= MAX_NAME_LENGTH) newTagName = it },
            label = { Text(stringResource(R.string.daily_log_add_custom_tag)) },
            modifier = Modifier
                .testTag("create-custom-tag-textbox")
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                onCreateAndAddCustomTag(newTagName)
                newTagName = ""
            }),
            trailingIcon = {
                IconButton(
                    onClick = {
                        onCreateAndAddCustomTag(newTagName)
                        newTagName = ""
                    },
                    enabled = newTagName.isNotBlank(),
                    modifier = Modifier.testTag("create-custom-tag-button"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.daily_log_create_custom_tag))
                }
            }
        )
    }
}

/**
 * Multi-line text field for daily log notes with a character counter.
 *
 * Enforces a maximum length of [MAX_NOTE_LENGTH] characters and displays
 * a live character count in the supporting text.
 *
 * @param note Current note text.
 * @param onNoteChanged Callback invoked on each text change.
 */
@Composable
internal fun NoteEditor(
    note: String,
    onNoteChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = note,
        onValueChange = { if (it.length <= MAX_NOTE_LENGTH) onNoteChanged(it) },
        label = { Text(stringResource(R.string.daily_log_add_notes)) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LocalDimensions.current.xl * 4),
        placeholder = { Text(stringResource(R.string.daily_log_notes_placeholder)) },
        supportingText = {
            Text(
                text = stringResource(R.string.daily_log_notes_char_count, note.length, MAX_NOTE_LENGTH),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        },
    )
}
