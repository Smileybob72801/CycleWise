package com.veleda.cyclewise.ui.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star as StarOutlined
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.ui.theme.RhythmWiseColors
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodConsistency
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.ui.auth.WaterTrackerCounter
import com.veleda.cyclewise.ui.utils.toLocalizedDateString
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf
import androidx.compose.ui.platform.testTag

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyLogScreen(
    date: LocalDate,
    onSaveComplete: () -> Unit,
    isPeriodDay: Boolean
) {
    val sessionScope = getKoin().getScope("session")

    val viewModel: DailyLogViewModel = koinViewModel(
        scope = sessionScope,
        parameters = { parametersOf(date, isPeriodDay) }
    )

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DailyLogEffect.NavigateBack -> onSaveComplete()
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.onEvent(DailyLogEvent.SaveLog)
                },
                modifier = Modifier.testTag("save_log_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.daily_log_save))
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
                        text = stringResource(R.string.daily_log_for, log.entry.entryDate.toLocalizedDateString()),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    if (uiState.isPeriodDay) {
                        SectionTitle(stringResource(R.string.daily_log_flow_title))
                        FlowIntensitySelector(
                            selectedIntensity = log.periodLog?.flowIntensity,
                            onSelectionChanged = { viewModel.onEvent(DailyLogEvent.FlowIntensityChanged(it)) }
                        )

                        SectionTitle(stringResource(R.string.period_color_section_title))
                        PeriodColorSelector(
                            selectedColor = log.periodLog?.periodColor,
                            onSelectionChanged = { viewModel.onEvent(DailyLogEvent.PeriodColorChanged(it)) }
                        )

                        SectionTitle(stringResource(R.string.period_consistency_section_title))
                        PeriodConsistencySelector(
                            selectedConsistency = log.periodLog?.periodConsistency,
                            onSelectionChanged = { viewModel.onEvent(DailyLogEvent.PeriodConsistencyChanged(it)) }
                        )
                    }

                    SectionTitle(stringResource(R.string.daily_log_mood_title))
                    MoodSelector(
                        selectedMood = log.entry.moodScore,
                        onSelectionChanged = { viewModel.onEvent(DailyLogEvent.MoodScoreChanged(it)) }
                    )

                    SectionTitle(stringResource(R.string.energy_section_title))
                    ScoreSelector(
                        selectedScore = log.entry.energyLevel,
                        onSelectionChanged = { viewModel.onEvent(DailyLogEvent.EnergyLevelChanged(it)) },
                        contentDescriptionPrefix = stringResource(R.string.energy_section_title)
                    )

                    SectionTitle(stringResource(R.string.libido_section_title))
                    ScoreSelector(
                        selectedScore = log.entry.libidoScore,
                        onSelectionChanged = { viewModel.onEvent(DailyLogEvent.LibidoScoreChanged(it)) },
                        contentDescriptionPrefix = stringResource(R.string.libido_section_title)
                    )

                    SectionTitle(stringResource(R.string.water_section_title))
                    WaterTrackerCounter(
                        cups = uiState.waterCups,
                        onIncrement = { viewModel.onEvent(DailyLogEvent.WaterIncrement) },
                        onDecrement = { viewModel.onEvent(DailyLogEvent.WaterDecrement) },
                        yesterdayMessage = null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    SectionTitle(stringResource(R.string.daily_log_symptoms_title))
                    SymptomLogger(
                        loggedSymptoms = log.symptomLogs,
                        symptomLibrary = uiState.symptomLibrary,
                        onToggleSymptom = { symptom ->
                            viewModel.onEvent(DailyLogEvent.SymptomToggled(symptom))
                        },
                        onCreateAndAddSymptom = { name ->
                            viewModel.onEvent(DailyLogEvent.CreateAndAddSymptom(name))
                        }
                    )

                    SectionTitle(stringResource(R.string.daily_log_medications_title))
                    MedicationLogger(
                        loggedMedications = log.medicationLogs,
                        medicationLibrary = uiState.medicationLibrary,
                        onToggleMedication = { medication ->
                            viewModel.onEvent(DailyLogEvent.MedicationToggled(medication))
                        },
                        onCreateAndAddMedication = { name ->
                            viewModel.onEvent(DailyLogEvent.MedicationCreatedAndAdded(name))
                        }
                    )

                    SectionTitle(stringResource(R.string.daily_log_custom_tags_title))
                    CustomTagLogger(
                        tags = log.entry.customTags,
                        onAddTag = { viewModel.onEvent(DailyLogEvent.TagAdded(it)) },
                        onRemoveTag = { viewModel.onEvent(DailyLogEvent.TagRemoved(it)) }
                    )

                    SectionTitle(stringResource(R.string.daily_log_notes_title))
                    NoteEditor(
                        note = log.entry.note ?: "",
                        onNoteChanged = { viewModel.onEvent(DailyLogEvent.NoteChanged(it)) }
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
                val icon = if (score <= (selectedMood ?: 0)) Icons.Filled.Star else Icons.Outlined.StarOutlined
                Icon(
                    icon,
                    contentDescription = stringResource(R.string.daily_log_mood_score, score),
                    modifier = Modifier.size(40.dp),
                    tint = if (score <= (selectedMood ?: 0))
                        RhythmWiseColors.StarGold
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Reusable 1-5 star rating selector for numeric wellness scores (energy, libido).
 *
 * @param selectedScore Currently selected score (1-5), or null if unset.
 * @param onSelectionChanged Callback invoked when the user taps a score.
 * @param contentDescriptionPrefix Prefix for accessibility labels (e.g., "Energy").
 */
@Composable
private fun ScoreSelector(
    selectedScore: Int?,
    onSelectionChanged: (Int) -> Unit,
    contentDescriptionPrefix: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        (1..5).forEach { score ->
            IconButton(onClick = { onSelectionChanged(score) }) {
                val icon = if (score <= (selectedScore ?: 0)) Icons.Filled.Star else Icons.Outlined.StarOutlined
                Icon(
                    icon,
                    contentDescription = stringResource(R.string.daily_log_score, contentDescriptionPrefix, score),
                    modifier = Modifier.size(40.dp),
                    tint = if (score <= (selectedScore ?: 0))
                        RhythmWiseColors.StarGold
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Selector for [PeriodColor] using wrapping filter chips.
 * Tapping an already-selected chip deselects it (sends null).
 *
 * @param selectedColor Currently selected color, or null.
 * @param onSelectionChanged Callback with the new selection (or null on deselect).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PeriodColorSelector(
    selectedColor: PeriodColor?,
    onSelectionChanged: (PeriodColor?) -> Unit
) {
    val labels = mapOf(
        PeriodColor.PINK to stringResource(R.string.period_color_pink),
        PeriodColor.BRIGHT_RED to stringResource(R.string.period_color_bright_red),
        PeriodColor.DARK_RED to stringResource(R.string.period_color_dark_red),
        PeriodColor.BROWN to stringResource(R.string.period_color_brown),
        PeriodColor.BLACK_OR_VERY_DARK to stringResource(R.string.period_color_black),
        PeriodColor.UNUSUAL_COLOR to stringResource(R.string.period_color_unusual)
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((color, label) in labels) {
            FilterChip(
                selected = selectedColor == color,
                onClick = {
                    val newSelection = if (selectedColor == color) null else color
                    onSelectionChanged(newSelection)
                },
                label = { Text(label) }
            )
        }
    }
}

/**
 * Selector for [PeriodConsistency] using wrapping filter chips.
 * Tapping an already-selected chip deselects it (sends null).
 *
 * @param selectedConsistency Currently selected consistency, or null.
 * @param onSelectionChanged Callback with the new selection (or null on deselect).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PeriodConsistencySelector(
    selectedConsistency: PeriodConsistency?,
    onSelectionChanged: (PeriodConsistency?) -> Unit
) {
    val labels = mapOf(
        PeriodConsistency.THIN to stringResource(R.string.period_consistency_thin),
        PeriodConsistency.MODERATE to stringResource(R.string.period_consistency_moderate),
        PeriodConsistency.THICK to stringResource(R.string.period_consistency_thick),
        PeriodConsistency.STRINGY to stringResource(R.string.period_consistency_stringy),
        PeriodConsistency.CLOTS_SMALL to stringResource(R.string.period_consistency_clots_small),
        PeriodConsistency.CLOTS_LARGE to stringResource(R.string.period_consistency_clots_large)
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((consistency, label) in labels) {
            FilterChip(
                selected = selectedConsistency == consistency,
                onClick = {
                    val newSelection = if (selectedConsistency == consistency) null else consistency
                    onSelectionChanged(newSelection)
                },
                label = { Text(label) }
            )
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

        OutlinedTextField(
            value = newMedicationName,
            onValueChange = { newMedicationName = it },
            label = { Text(stringResource(R.string.daily_log_add_medication)) },
            modifier = Modifier.fillMaxWidth(),
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
                    enabled = newMedicationName.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.daily_log_create_medication))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CustomTagLogger(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.forEach { tag ->
                InputChip(
                    selected = false,
                    onClick = { /* Not used */ },
                    label = { Text(tag) },
                    trailingIcon = {
                        IconButton(onClick = { onRemoveTag(tag) }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.daily_log_remove_tag, tag))
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
        label = { Text(stringResource(R.string.daily_log_add_notes)) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        placeholder = { Text(stringResource(R.string.daily_log_notes_placeholder)) }
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
    var newSymptomName by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

        OutlinedTextField(
            value = newSymptomName,
            onValueChange = { newSymptomName = it },
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