package com.veleda.cyclewise.ui.log.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.log.MAX_NAME_LENGTH
import com.veleda.cyclewise.ui.log.MAX_NOTE_LENGTH
import com.veleda.cyclewise.ui.log.components.SectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions

@Composable
internal fun NotesTagsPage(
    tags: List<String>,
    note: String,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
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

        SectionCard(
            title = stringResource(R.string.daily_log_custom_tags_title),
            icon = Icons.AutoMirrored.Outlined.Notes,
        ) {
            CustomTagLogger(
                tags = tags,
                onAddTag = onAddTag,
                onRemoveTag = onRemoveTag,
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

        Spacer(Modifier.height(dims.xl))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CustomTagLogger(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm)) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= MAX_NAME_LENGTH) text = it },
            label = { Text(stringResource(R.string.daily_log_add_tag)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                onAddTag(text)
                text = ""
            }),
            trailingIcon = {
                IconButton(onClick = {
                    onAddTag(text)
                    text = ""
                }, enabled = text.isNotBlank()) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.daily_log_add_tag_button))
                }
            }
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm),
        ) {
            tags.forEach { tag ->
                InputChip(
                    selected = false,
                    onClick = { /* Not used */ },
                    label = { Text(tag) },
                    trailingIcon = {
                        IconButton(onClick = { onRemoveTag(tag) }, modifier = Modifier.size(LocalDimensions.current.lg)) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.daily_log_remove_tag, tag))
                        }
                    }
                )
            }
        }
    }
}

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
