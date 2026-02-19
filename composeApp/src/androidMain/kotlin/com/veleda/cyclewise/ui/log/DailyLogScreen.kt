package com.veleda.cyclewise.ui.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Star as StarOutlined
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodConsistency
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.ui.auth.WaterTrackerCounter
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.theme.RhythmWiseColors
import com.veleda.cyclewise.ui.utils.toLocalizedDateString
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf
import androidx.compose.ui.platform.testTag

/** Number of pages in the daily log pager. */
private const val PAGE_COUNT = 5

/** Page indices for the daily log pager. */
private const val PAGE_WELLNESS = 0
private const val PAGE_PERIOD = 1
private const val PAGE_SYMPTOMS = 2
private const val PAGE_MEDICATIONS = 3
private const val PAGE_NOTES = 4

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyLogScreen(
    date: LocalDate,
) {
    val dims = LocalDimensions.current
    val sessionScope = getKoin().getScope("session")

    val viewModel: DailyLogViewModel = koinViewModel(
        scope = sessionScope,
        parameters = { parametersOf(date) }
    )

    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()

    val pageLabels = listOf(
        stringResource(R.string.daily_log_page_wellness),
        stringResource(R.string.daily_log_page_period),
        stringResource(R.string.daily_log_page_symptoms),
        stringResource(R.string.daily_log_page_medications),
        stringResource(R.string.daily_log_page_notes),
    )

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
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Text(
                    text = stringResource(R.string.daily_log_for, log.entry.entryDate.toLocalizedDateString()),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = dims.md, vertical = dims.md)
                )

                // Page indicator tabs
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = dims.md,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    pageLabels.forEachIndexed { index, label ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                }

                // Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("daily_log_pager"),
                ) { page ->
                    when (page) {
                        PAGE_WELLNESS -> WellnessPage(
                            moodScore = log.entry.moodScore,
                            energyLevel = log.entry.energyLevel,
                            libidoScore = log.entry.libidoScore,
                            waterCups = uiState.waterCups,
                            onMoodChanged = { viewModel.onEvent(DailyLogEvent.MoodScoreChanged(it)) },
                            onEnergyChanged = { viewModel.onEvent(DailyLogEvent.EnergyLevelChanged(it)) },
                            onLibidoChanged = { viewModel.onEvent(DailyLogEvent.LibidoScoreChanged(it)) },
                            onWaterIncrement = { viewModel.onEvent(DailyLogEvent.WaterIncrement) },
                            onWaterDecrement = { viewModel.onEvent(DailyLogEvent.WaterDecrement) },
                        )
                        PAGE_PERIOD -> PeriodPage(
                            isPeriodDay = uiState.isPeriodDay,
                            flowIntensity = log.periodLog?.flowIntensity,
                            periodColor = log.periodLog?.periodColor,
                            periodConsistency = log.periodLog?.periodConsistency,
                            onPeriodToggled = { viewModel.onEvent(DailyLogEvent.PeriodToggled(it)) },
                            onFlowChanged = { viewModel.onEvent(DailyLogEvent.FlowIntensityChanged(it)) },
                            onColorChanged = { viewModel.onEvent(DailyLogEvent.PeriodColorChanged(it)) },
                            onConsistencyChanged = { viewModel.onEvent(DailyLogEvent.PeriodConsistencyChanged(it)) },
                        )
                        PAGE_SYMPTOMS -> SymptomsPage(
                            loggedSymptoms = log.symptomLogs,
                            symptomLibrary = uiState.symptomLibrary,
                            onToggleSymptom = { viewModel.onEvent(DailyLogEvent.SymptomToggled(it)) },
                            onCreateAndAddSymptom = { viewModel.onEvent(DailyLogEvent.CreateAndAddSymptom(it)) },
                        )
                        PAGE_MEDICATIONS -> MedicationsPage(
                            loggedMedications = log.medicationLogs,
                            medicationLibrary = uiState.medicationLibrary,
                            onToggleMedication = { viewModel.onEvent(DailyLogEvent.MedicationToggled(it)) },
                            onCreateAndAddMedication = { viewModel.onEvent(DailyLogEvent.MedicationCreatedAndAdded(it)) },
                        )
                        PAGE_NOTES -> NotesTagsPage(
                            tags = log.entry.customTags,
                            note = log.entry.note ?: "",
                            onAddTag = { viewModel.onEvent(DailyLogEvent.TagAdded(it)) },
                            onRemoveTag = { viewModel.onEvent(DailyLogEvent.TagRemoved(it)) },
                            onNoteChanged = { viewModel.onEvent(DailyLogEvent.NoteChanged(it)) },
                        )
                    }
                }
            }
        }
    }
}

// ── Page composables ─────────────────────────────────────────────────

@Composable
private fun WellnessPage(
    moodScore: Int?,
    energyLevel: Int?,
    libidoScore: Int?,
    waterCups: Int,
    onMoodChanged: (Int) -> Unit,
    onEnergyChanged: (Int) -> Unit,
    onLibidoChanged: (Int) -> Unit,
    onWaterIncrement: () -> Unit,
    onWaterDecrement: () -> Unit,
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
            title = stringResource(R.string.daily_log_mood_title),
            icon = Icons.Outlined.SelfImprovement,
        ) {
            MoodSelector(
                selectedMood = moodScore,
                onSelectionChanged = onMoodChanged,
            )
        }

        SectionCard(
            title = stringResource(R.string.energy_section_title),
            icon = Icons.Outlined.Bedtime,
        ) {
            ScoreSelector(
                selectedScore = energyLevel,
                onSelectionChanged = onEnergyChanged,
                contentDescriptionPrefix = stringResource(R.string.energy_section_title),
            )
        }

        SectionCard(
            title = stringResource(R.string.libido_section_title),
            icon = Icons.Outlined.Bedtime,
        ) {
            ScoreSelector(
                selectedScore = libidoScore,
                onSelectionChanged = onLibidoChanged,
                contentDescriptionPrefix = stringResource(R.string.libido_section_title),
            )
        }

        SectionCard(
            title = stringResource(R.string.water_section_title),
            icon = Icons.Outlined.WaterDrop,
        ) {
            WaterTrackerCounter(
                cups = waterCups,
                onIncrement = onWaterIncrement,
                onDecrement = onWaterDecrement,
                yesterdayMessage = null,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Bottom spacer for comfortable scrolling past bottom nav
        Spacer(Modifier.height(dims.xl))
    }
}

@Composable
private fun PeriodPage(
    isPeriodDay: Boolean,
    flowIntensity: FlowIntensity?,
    periodColor: PeriodColor?,
    periodConsistency: PeriodConsistency?,
    onPeriodToggled: (Boolean) -> Unit,
    onFlowChanged: (FlowIntensity?) -> Unit,
    onColorChanged: (PeriodColor?) -> Unit,
    onConsistencyChanged: (PeriodConsistency?) -> Unit,
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

        // Period toggle
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dims.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.daily_log_period_toggle),
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(
                    checked = isPeriodDay,
                    onCheckedChange = onPeriodToggled,
                    modifier = Modifier.testTag("period_toggle"),
                )
            }
        }

        AnimatedVisibility(
            visible = isPeriodDay,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(dims.md)) {
                SectionCard(
                    title = stringResource(R.string.daily_log_flow_title),
                    icon = Icons.Outlined.WaterDrop,
                ) {
                    FlowIntensitySelector(
                        selectedIntensity = flowIntensity,
                        onSelectionChanged = onFlowChanged,
                    )
                }

                SectionCard(
                    title = stringResource(R.string.period_color_section_title),
                    icon = Icons.Outlined.WaterDrop,
                ) {
                    PeriodColorSelector(
                        selectedColor = periodColor,
                        onSelectionChanged = onColorChanged,
                    )
                }

                SectionCard(
                    title = stringResource(R.string.period_consistency_section_title),
                    icon = Icons.Outlined.WaterDrop,
                ) {
                    PeriodConsistencySelector(
                        selectedConsistency = periodConsistency,
                        onSelectionChanged = onConsistencyChanged,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isPeriodDay,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dims.xl),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.daily_log_period_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(dims.xl))
    }
}

@Composable
private fun SymptomsPage(
    loggedSymptoms: List<SymptomLog>,
    symptomLibrary: List<Symptom>,
    onToggleSymptom: (Symptom) -> Unit,
    onCreateAndAddSymptom: (String) -> Unit,
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
        ) {
            SymptomLogger(
                loggedSymptoms = loggedSymptoms,
                symptomLibrary = symptomLibrary,
                onToggleSymptom = onToggleSymptom,
                onCreateAndAddSymptom = onCreateAndAddSymptom,
            )
        }

        Spacer(Modifier.height(dims.xl))
    }
}

@Composable
private fun MedicationsPage(
    loggedMedications: List<MedicationLog>,
    medicationLibrary: List<Medication>,
    onToggleMedication: (Medication) -> Unit,
    onCreateAndAddMedication: (String) -> Unit,
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
        ) {
            MedicationLogger(
                loggedMedications = loggedMedications,
                medicationLibrary = medicationLibrary,
                onToggleMedication = onToggleMedication,
                onCreateAndAddMedication = onCreateAndAddMedication,
            )
        }

        Spacer(Modifier.height(dims.xl))
    }
}

@Composable
private fun NotesTagsPage(
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

// ── Shared components ────────────────────────────────────────────────

/**
 * Reusable card wrapper for daily log sections.
 *
 * Provides consistent visual treatment: surfaceVariant background, medium rounded
 * shape, a leading icon and title row, followed by the section [content].
 *
 * @param title   Section heading text.
 * @param icon    Leading icon displayed beside the title.
 * @param content Slot for the section's interactive content.
 */
@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dims = LocalDimensions.current
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(dims.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dims.sm),
                modifier = Modifier.padding(bottom = dims.sm),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            content()
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = LocalDimensions.current.lg, bottom = LocalDimensions.current.sm)
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
        horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm)
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
                    modifier = Modifier.size(LocalDimensions.current.xl),
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
                    modifier = Modifier.size(LocalDimensions.current.xl),
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
        horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm)
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
        horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm)
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

    Column(verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.md)) {
        if (medicationLibrary.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm),
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
    Column(verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm)) {
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
            .heightIn(min = LocalDimensions.current.xl * 4),
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

    Column(verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.md)) {
        if (symptomLibrary.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm),
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
