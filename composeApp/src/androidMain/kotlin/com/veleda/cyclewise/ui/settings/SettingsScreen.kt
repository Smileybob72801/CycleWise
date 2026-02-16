package com.veleda.cyclewise.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.BuildConfig
import com.veleda.cyclewise.R
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
    val topSymptomsCount by appSettings.topSymptomsCount.collectAsState(initial = 3)

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val session = getKoin().getScopeOrNull("session")

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp), // Only apply horizontal padding here
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Auto-lock Section ---
            Text(stringResource(R.string.settings_autolock_title), style = MaterialTheme.typography.titleMedium)
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

            // --- Security Section ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(stringResource(R.string.settings_security_title), style = MaterialTheme.typography.titleMedium)
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
                    Text(stringResource(R.string.settings_lock_button))
                }
                if (session == null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_locked_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // --- Insight Settings Section ---
            HorizontalDivider()
            Column {
                Text(stringResource(R.string.settings_insights_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.settings_top_symptoms, topSymptomsCount))
                Slider(
                    value = topSymptomsCount.toFloat(),
                    onValueChange = { newValue ->
                        scope.launch { appSettings.setTopSymptomsCount(newValue.roundToInt()) }
                    },
                    valueRange = 1f..5f,
                    steps = 3
                )
            }

            // --- Log Summary Display Section ---
            HorizontalDivider()
            Column {
                Text(
                    stringResource(R.string.log_summary_display_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                val showMood by appSettings.showMoodInSummary.collectAsState(initial = true)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = showMood,
                        onCheckedChange = { scope.launch { appSettings.setShowMoodInSummary(it) } }
                    )
                    Text(stringResource(R.string.show_mood_label))
                }

                val showEnergy by appSettings.showEnergyInSummary.collectAsState(initial = true)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = showEnergy,
                        onCheckedChange = { scope.launch { appSettings.setShowEnergyInSummary(it) } }
                    )
                    Text(stringResource(R.string.show_energy_label))
                }

                val showLibido by appSettings.showLibidoInSummary.collectAsState(initial = true)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = showLibido,
                        onCheckedChange = { scope.launch { appSettings.setShowLibidoInSummary(it) } }
                    )
                    Text(stringResource(R.string.show_libido_label))
                }
            }

            // --- Phase Visibility Section ---
            HorizontalDivider()
            PhaseVisibilitySettings(appSettings)

            // --- Phase Colors Section ---
            HorizontalDivider()
            PhaseColorSettings(appSettings)

            // --- Reminders Section ---
            HorizontalDivider()
            ReminderSettings(appSettings, getKoin().get())

            // --- About Section ---
            HorizontalDivider()
            var showAboutDialog by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    stringResource(R.string.about_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showAboutDialog = true }) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.app_name))
                }
            }

            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    title = { Text(stringResource(R.string.app_name)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.about_description))
                            Text(stringResource(R.string.about_version_label, BuildConfig.VERSION_NAME))
                            Text(stringResource(R.string.about_license))
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAboutDialog = false }) {
                            Text(stringResource(R.string.about_close))
                        }
                    }
                )
            }

            // --- Developer Options Section ---
            if (BuildConfig.DEBUG) {
                HorizontalDivider()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(stringResource(R.string.settings_developer_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.height(8.dp))

                    Button(
                        enabled = session != null,
                        onClick = {
                            session?.let {
                                val seeder: DebugSeederUseCase = it.get()
                                scope.launch {
                                    Toast.makeText(context, context.getString(R.string.settings_seeding), Toast.LENGTH_SHORT).show()
                                    seeder()
                                    Toast.makeText(context, context.getString(R.string.settings_seeding_complete), Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_seed_button))
                    }
                    if (session == null) {
                        Text(
                            stringResource(R.string.settings_developer_locked),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp)) // Final spacer for scrollability
        }
    }
}