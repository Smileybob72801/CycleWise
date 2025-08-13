package com.veleda.cyclewise.ui.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.domain.models.FlowIntensity
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin // <-- ADD THIS IMPORT
import org.koin.core.parameter.parametersOf

@Composable
fun DailyLogScreen(
    date: LocalDate,
    onSaveComplete: () -> Unit
) {
    // 1. Get the ACTIVE scope INSTANCE using the unique ID we gave it
    //    during creation in PassphraseScreen.
    val sessionScope = getKoin().getScope("session")

    // 2. Pass this instance to koinViewModel.
    val viewModel: DailyLogViewModel = koinViewModel(
        scope = sessionScope, // Pass the Scope INSTANCE here
        parameters = { parametersOf(date) }
    )

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.saveEntry()
                onSaveComplete()
            }) {
                Icon(Icons.Default.Check, contentDescription = "Save Entry")
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
            uiState.entry != null -> {
                val entry = uiState.entry!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Log for ${entry.entryDate}",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    SectionTitle("Flow")
                    FlowIntensitySelector(
                        selectedIntensity = entry.flowIntensity,
                        onSelectionChanged = { viewModel.setFlowIntensity(it) }
                    )

                    SectionTitle("Mood")
                    MoodSelector(
                        selectedMood = entry.moodScore,
                        onSelectionChanged = { viewModel.setMoodScore(it) }
                    )

                    Spacer(Modifier.height(80.dp))
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

@Composable
private fun FlowIntensitySelector(
    selectedIntensity: String?,
    onSelectionChanged: (String?) -> Unit
) {
    val options = listOf(FlowIntensity.LIGHT, FlowIntensity.MEDIUM, FlowIntensity.HEAVY)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { intensity ->
            FilterChip(
                selected = selectedIntensity == intensity,
                onClick = {
                    val newSelection = if (selectedIntensity == intensity) null else intensity
                    onSelectionChanged(newSelection)
                },
                label = { Text(intensity.replaceFirstChar { it.uppercase() }) }
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
                val icon = if (score <= (selectedMood ?: 0)) Icons.Default.Star else Icons.Outlined.Close
                Icon(icon, contentDescription = "Mood score $score", modifier = Modifier.size(40.dp))
            }
        }
    }
}