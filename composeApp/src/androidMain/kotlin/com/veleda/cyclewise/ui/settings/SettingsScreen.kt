package com.veleda.cyclewise.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
            // ... (Auto-lock and Security sections remain the same)

            // Developer Options - only shown in debug builds
            if (BuildConfig.DEBUG) {
                Divider()
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