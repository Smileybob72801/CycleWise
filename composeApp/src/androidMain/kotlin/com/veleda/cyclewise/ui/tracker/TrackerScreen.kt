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
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import com.veleda.cyclewise.ui.nav.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

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
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Tracker",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(16.dp))

            // We will group all action buttons here
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { viewModel.onAddNewCycleClicked() }) {
                    Text("Add New Cycle")
                }
                Button(onClick = { viewModel.onEndCycleClicked() }) {
                    Text("End Cycle")
                }

                // vvv ADD THE NEW BUTTON HERE vvv
                Button(onClick = {
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    navController.navigate(NavRoute.DailyLog.createRoute(today))
                }) {
                    Text("Log Today")
                }
                // ^^^ ADD THE NEW BUTTON HERE ^^^
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            Spacer(Modifier.height(16.dp))

            // Display list of cycles
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(cycles) { cycle ->
                    Text(
                        "Cycle started: ${cycle.startDate} - Ended: ${cycle.endDate ?: "Ongoing"}",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
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
