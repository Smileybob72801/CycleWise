package com.veleda.cyclewise.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.veleda.cyclewise.ui.nav.NavRoute
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.getKoin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    // App settings are app-scoped (available even when locked)
    val appSettings: AppSettings = getKoin().get()
    val autolock by appSettings.autolockMinutes.collectAsState(initial = 10)
    val scope = rememberCoroutineScope()

    // Session scope (only present when unlocked)
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
            // Auto-lock timeout
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

            // Lock Now (only if there’s an unlocked session)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text("Security", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Button(
                    enabled = session != null,
                    onClick = {
                        // Close the unlocked session and bounce to Passphrase
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
        }
    }
}