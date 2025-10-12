package com.veleda.cyclewise.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.veleda.cyclewise.BuildConfig
import com.veleda.cyclewise.domain.usecases.DebugSeederUseCase
import com.veleda.cyclewise.ui.nav.NavRoute
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val appSettings: AppSettings = getKoin().get()
    val autolock by appSettings.autolockMinutes.collectAsState(initial = 10)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val session = getKoin().getScopeOrNull("session")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val appSettings: AppSettings = getKoin().get()
            val autolock by appSettings.autolockMinutes.collectAsState(initial = 10)
            val topSymptomsCount by appSettings.topSymptomsCount.collectAsState(initial = 3)

            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            val session = getKoin().getScopeOrNull("session")

            Scaffold(
                topBar = { TopAppBar(title = { Text("Settings") }) }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text("Auto-lock timeout (minutes)", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 30).forEach { m ->
                            FilterChip(
                                selected = autolock == m,
                                onClick = { scope.launch { appSettings.setAutolockMinutes(m) } },
                                label = { Text("$m") }
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("Security", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            enabled = session != null,
                            onClick = {
                                session?.close()
                                navController.navigate(NavRoute.Passphrase.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        ) {
                            Text("Lock Now")
                        }
                        if (session == null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Currently locked — unlock to access secured data.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // ** 2. Add the Insight Settings section with the Slider **
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                    Column {
                        Text("Insight Settings", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        Text("Top Frequent Symptoms to Display: $topSymptomsCount")
                        Slider(
                            value = topSymptomsCount.toFloat(),
                            onValueChange = { newValue ->
                                // This updates the UI immediately as the user drags
                                scope.launch { appSettings.setTopSymptomsCount(newValue.roundToInt()) }
                            },
                            valueRange = 1f..5f, // Let the user choose between 1 and 5 symptoms
                            steps = 3 // Creates 4 steps for values 1, 2, 3, 4, 5
                        )
                    }

                    if (BuildConfig.DEBUG) {
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Developer Options", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                enabled = session != null,
                                onClick = {
                                    session?.let {
                                        val seeder: DebugSeederUseCase = it.get()
                                        scope.launch {
                                            Toast.makeText(context, "Seeding database...", Toast.LENGTH_SHORT).show()
                                            seeder()
                                            Toast.makeText(context, "Seeding complete!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("Seed Database")
                            }
                            if (session == null) {
                                Text(
                                    "Unlock the app to use developer tools.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}