package com.veleda.cyclewise.ui.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.domain.models.Symptom
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // Add ExperimentalLayoutApi
@Composable
fun DailyLogScreen(
    date: LocalDate,
    onSaveComplete: () -> Unit
) {
    val sessionScope = getKoin().getScope("session")
    val viewModel: DailyLogViewModel = koinViewModel(
        scope = sessionScope,
        parameters = { parametersOf(date) }
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.saveLog()
                onSaveComplete()
            }) {
                Icon(Icons.Default.Check, contentDescription = "Save Log")
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            // vvv FIX: Use the new 'log' state object vvv
            uiState.log != null -> {
                val log = uiState.log!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Log for ${log.entry.entryDate}",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    SectionTitle("Flow")
                    FlowIntensitySelector(
                        selectedIntensity = log.entry.flowIntensity, // Pass the enum
                        onSelectionChanged = { viewModel.setFlowIntensity(it) }
                    )

                    SectionTitle("Mood")
                    MoodSelector(
                        selectedMood = log.entry.moodScore,
                        onSelectionChanged = { viewModel.setMoodScore(it) }
                    )

                    SectionTitle("Symptoms")
                    SymptomSelector(
                        allSymptoms = viewModel.commonSymptoms,
                        selectedSymptoms = log.symptoms,
                        onSymptomClick = { viewModel.toggleSymptom(it) }
                    )

                    SectionTitle("Medications")
                    MedicationLogger(
                        loggedMedications = log.medicationLogs,
                        medicationLibrary = viewModel.medicationLibrary.collectAsState().value,
                        onAddMedication = { viewModel.onAddMedication(it) },
                        onCreateAndAddMedication = { viewModel.onCreateAndAddMedication(it) },
                        onRemoveMedication = { viewModel.onRemoveMedication(it) }
                    )

                    SectionTitle("Custom Tags")
                    CustomTagLogger(
                        tags = log.entry.customTags,
                        onAddTag = { viewModel.onAddTag(it) },
                        onRemoveTag = { viewModel.onRemoveTag(it) }
                    )

                    SectionTitle("Notes")
                    NoteEditor(
                        note = log.entry.note ?: "",
                        onNoteChanged = { viewModel.onNoteChanged(it) }
                    )

                    Spacer(Modifier.height(80.dp)) // Spacer for the FAB
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlowIntensitySelector(
    selectedIntensity: FlowIntensity?,
    onSelectionChanged: (FlowIntensity?) -> Unit
) {
    val options = FlowIntensity.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (intensity in options) {
            FilterChip(
                selected = selectedIntensity == intensity,
                onClick = {
                    val newSelection = if (selectedIntensity == intensity) null else intensity
                    onSelectionChanged(newSelection)
                },
                label = { Text(intensity.name.replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
private fun MoodSelector(
    selectedMood: Int?,
    onSelectionChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        (1..5).forEach { score ->
            IconButton(onClick = { onSelectionChanged(score) }) {
                val icon = if (score <= (selectedMood ?: 0)) Icons.Filled.Star else Icons.Outlined.Clear
                Icon(icon, contentDescription = "Mood score $score", modifier = Modifier.size(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SymptomSelector(
    allSymptoms: List<String>,
    selectedSymptoms: List<Symptom>,
    onSymptomClick: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        allSymptoms.forEach { symptomType ->
            val isSelected = selectedSymptoms.any { it.type == symptomType }
            FilterChip(
                selected = isSelected,
                onClick = { onSymptomClick(symptomType) },
                label = { Text(symptomType.replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationLogger(
    loggedMedications: List<MedicationLog>,
    medicationLibrary: List<Medication>,
    onAddMedication: (Medication) -> Unit,
    onCreateAndAddMedication: (String) -> Unit,
    onRemoveMedication: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val filteredLibrary = remember(query, medicationLibrary) {
        if (query.isBlank()) {
            emptyList()
        } else {
            medicationLibrary.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // --- 1. Display currently logged medications ---
        if (loggedMedications.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                loggedMedications.forEach { log ->
                    // Find the full medication info from the library using the ID
                    val medInfo = medicationLibrary.find { it.id == log.medicationId }
                    if (medInfo != null) {
                        InputChip(
                            // vvv THE FIX IS HERE vvv
                            selected = false, // This chip is just for display, not selection
                            onClick = { /* No action on click */ },
                            // ^^^ THE FIX IS HERE ^^^
                            label = { Text(medInfo.name) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onRemoveMedication(log.id) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove ${medInfo.name}")
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- 2. Autocomplete search and add box ---
        ExposedDropdownMenuBox(
            expanded = isSearchExpanded && query.isNotEmpty(),
            onExpandedChange = {
                // We don't want to change the expanded state on click, only through focus.
            }
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    isSearchExpanded = true // Show dropdown when user types
                },
                label = { Text("Search or add medication...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(), // Connects the text field to the dropdown
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { // Clear button
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )

            // The actual dropdown menu
            ExposedDropdownMenu(
                expanded = isSearchExpanded && query.isNotEmpty(),
                onDismissRequest = { isSearchExpanded = false } // Hide when user clicks away
            ) {
                filteredLibrary.forEach { med ->
                    DropdownMenuItem(
                        text = { Text(med.name) },
                        onClick = {
                            onAddMedication(med)
                            query = ""
                            isSearchExpanded = false
                        }
                    )
                }
                // Show "Create new" option if the query doesn't exactly match any library item
                if (query.isNotEmpty() && filteredLibrary.none { it.name.equals(query, ignoreCase = true) }) {
                    DropdownMenuItem(
                        text = {
                            Text("Create new: \"$query\"")
                        },
                        onClick = {
                            onCreateAndAddMedication(query)
                            query = ""
                            isSearchExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Create new") }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomTagLogger(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    // Similar implementation to MedicationLogger
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Add a custom tag...") },
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
                    Icon(Icons.Default.Add, contentDescription = "Add Tag")
                }
            }
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.forEach { tag ->
                InputChip(
                    selected = false,
                    onClick = { /* Not used */ },
                    label = { Text(tag) },
                    trailingIcon = {
                        IconButton(onClick = { onRemoveTag(tag) }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove $tag")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NoteEditor(
    note: String,
    onNoteChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = note,
        onValueChange = onNoteChanged,
        label = { Text("Add any notes...") },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        placeholder = { Text("How are you feeling? Any observations?") }
    )
}
