package com.veleda.cyclewise.ui.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.domain.models.FlowIntensity
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
                // vvv FIX: Call the correct save function vvv
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
    // We can now get the options directly from the enum
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
                val icon = if (score <= (selectedMood ?: 0)) Icons.Filled.Star else Icons.Outlined.Star
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
