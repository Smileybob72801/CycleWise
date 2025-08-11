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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.getKoin
import androidx.navigation.compose.*
import com.veleda.cyclewise.domain.models.Cycle
import com.veleda.cyclewise.ui.nav.*

/**
 * Main UI for tracking cycles: shows a list and an Add button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(navController: NavController) {
    val scope = getKoin().getScopeOrNull("session")

    if (scope == null) {
        // If no unlocked session, send user back to PassphraseScreen
        LaunchedEffect(Unit) {
            navController.navigate(NavRoute.Passphrase.route) {
                popUpTo(0) { inclusive = true }
            }
        }
        return
    }

    val viewModel: CycleViewModel = scope.get()

    val cycles by viewModel.cycles.collectAsState()


    Scaffold(
        topBar = { TopAppBar(title = { Text("Cycle Tracker") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onAddNewCycleClicked() }) {
                Icon(Icons.Default.Add, contentDescription = null)

                val hasOngoing = cycles.any { it.endDate == null }
                if (hasOngoing) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.onEndCycleClicked() },
                        icon = { Icon(Icons.Default.Check, null) },
                        text = { Text("End Cycle") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (cycles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No cycles yet")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.onAddNewCycleClicked() }) {
                            Text("Start first cycle")
                        }
                    }
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
private fun CycleItem(cycle: Cycle) {
    val status = if (cycle.endDate == null) "Ongoing" else "Ended"
    val lengthDays = cycle.endDate?.let { end -> end.toEpochDays() - cycle.startDate.toEpochDays() + 1 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cycle", style = MaterialTheme.typography.titleSmall)
                AssistChip(label = { Text(status) }, onClick = { /* noop */ })
            }
            Spacer(Modifier.height(8.dp))
            Text("Start: ${cycle.startDate}")
            Text("End: ${cycle.endDate ?: "—"}")
            if (lengthDays != null) {
                Spacer(Modifier.height(4.dp))
                Text("Length: $lengthDays days")
            }
        }
    }
}
