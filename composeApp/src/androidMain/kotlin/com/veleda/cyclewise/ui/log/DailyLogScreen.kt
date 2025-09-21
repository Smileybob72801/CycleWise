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
import com.veleda.cyclewise.domain.models.SymptomLog
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf
import androidx.compose.ui.platform.testTag

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
            FloatingActionButton(
                onClick = {
                    viewModel.saveLog()
                    onSaveComplete()
                },
                modifier = Modifier.testTag("save_log_button") // <-- ADD THIS
            ) {
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
                    SymptomLogger(
                        loggedSymptoms = log.symptomLogs,
                        symptomLibrary = uiState.symptomLibrary,
                        onToggleSymptom = { viewModel.onToggleSymptom(it) },
                        onCreateAndAddSymptom = { viewModel.onCreateAndAddSymptom(it) }
                    )

                    SectionTitle("Medications")
                    MedicationLogger(
                        loggedMedications = log.medicationLogs,
                        medicationLibrary = uiState.medicationLibrary,
                        onToggleMedication = { viewModel.onToggleMedication(it) },
                        onCreateAndAddMedication = { viewModel.onCreateAndAddMedication(it) }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MedicationLogger(
    loggedMedications: List<MedicationLog>,
    medicationLibrary: List<Medication>,
    onToggleMedication: (Medication) -> Unit,
    onCreateAndAddMedication: (String) -> Unit
) {
    var newMedicationName by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Part 1: Display the entire library as selectable chips
        if (medicationLibrary.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                medicationLibrary.forEach { medication ->
                    val isSelected = loggedMedications.any { it.medicationId == medication.id }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleMedication(medication) },
                        label = { Text(medication.name) }
                    )
                }
            }
        }

        // Part 2: Text field to add a new medication to the library
        OutlinedTextField(
            value = newMedicationName,
            onValueChange = { newMedicationName = it },
            label = { Text("Add new medication...") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                onCreateAndAddMedication(newMedicationName)
                newMedicationName = "" // Clear text after adding
            }),
            trailingIcon = {
                IconButton(
                    onClick = {
                        onCreateAndAddMedication(newMedicationName)
                        newMedicationName = ""
                    },
                    enabled = newMedicationName.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create and Add Medication")
                }
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SymptomLogger(
    loggedSymptoms: List<SymptomLog>,
    symptomLibrary: List<Symptom>,
    onToggleSymptom: (Symptom) -> Unit,
    onCreateAndAddSymptom: (String) -> Unit
) {
    var newMedicationName by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Part 1: Display the entire library as selectable chips
        if (symptomLibrary.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                symptomLibrary.forEach { symptom ->
                    val isSelected = loggedSymptoms.any { it.symptomId == symptom.id }
                    FilterChip(
                        modifier = Modifier.testTag("chip-${symptom.name.uppercase()}"),
                        selected = isSelected,
                        onClick = { onToggleSymptom(symptom) },
                        label = { Text(symptom.name) }
                    )
                }
            }
        }

        // Part 2: Text field to add a new symptom to the library
        OutlinedTextField(
            value = newMedicationName,
            onValueChange = { newMedicationName = it },
            label = { Text("Add new symptom...") },
            modifier = Modifier
                .testTag("create-symptom-textbox")
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                onCreateAndAddSymptom(newMedicationName)
                newMedicationName = "" // Clear text after adding
            }),
            trailingIcon = {
                IconButton(
                    onClick = {
                        onCreateAndAddSymptom(newMedicationName)
                        newMedicationName = ""
                    },
                    enabled = newMedicationName.isNotBlank(),
                    modifier = Modifier.testTag("create-symptom-button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create and Add Symptom")
                }
            }
        )
    }
}
