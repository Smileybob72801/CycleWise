package com.veleda.cyclewise.ui.tracker

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.getKoin

/**
 * Main UI for tracking cycles: shows a list and an Add button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(passphrase: String) {
    val viewModel = remember(passphrase) {
        getKoin().get<CycleViewModel>(
            parameters = { parametersOf(passphrase) }
        )
    }

    val cycles by viewModel.cycles.collectAsState()
    Log.d("TrackerScreen", "Loading ViewModel with passphrase: $passphrase")


    Scaffold(
        topBar = { TopAppBar(title = { Text("Cycle Tracker") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onAddNewCycleClicked() }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (cycles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No cycles yet. Tap + to add.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cycles) { cycle ->
                        CycleItem(cycle)
                    }
                }
            }
        }
    }
}

@Composable
fun CycleItem(cycle: com.veleda.cyclewise.domain.models.Cycle) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cycle ID: ${cycle.id}",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Start Date: ${cycle.startDate}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "End Date: ${cycle.endDate ?: "Ongoing"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
