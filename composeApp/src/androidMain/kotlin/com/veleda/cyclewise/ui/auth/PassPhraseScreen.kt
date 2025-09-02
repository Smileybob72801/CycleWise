package com.veleda.cyclewise.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.androidData.local.database.CycleDatabase
import com.veleda.cyclewise.di.SESSION_SCOPE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf
import com.veleda.cyclewise.domain.repository.CycleRepository
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.flow.first

@Composable
fun PassphraseScreen(
    onPassphraseEntered: () -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var isUnlocking by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val koin = getKoin()
    val appSettings: AppSettings = koin.get()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isUnlocking) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Unlocking...")
        } else {
            Text("Enter your passphrase", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("Passphrase") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        isUnlocking = true
                        try {
                            val needsPrepopulation = !appSettings.isPrepopulated.first()

                            withContext(Dispatchers.IO) {
                                val sessionScope = koin.getScopeOrNull("session")
                                    ?: koin.createScope(
                                        scopeId = "session",
                                        qualifier = SESSION_SCOPE
                                    )

                                sessionScope.get<CycleDatabase> { parametersOf(passphrase) }

                                if (needsPrepopulation) {
                                    val repository = sessionScope.get<CycleRepository>()
                                    repository.prepopulateSymptomLibrary()
                                    // IMPORTANT: Set the flag so this never runs again
                                    appSettings.setPrepopulated(true)
                                }
                            }

                            onPassphraseEntered()

                        } catch (e: Exception) {
                            e.printStackTrace()
                            koin.getScopeOrNull("session")?.close()
                            withContext(Dispatchers.Main) {
                                isUnlocking = false
                                Toast.makeText(context, "Failed to unlock. Wrong passphrase?", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                enabled = passphrase.isNotBlank() && !isUnlocking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Unlock")
            }
        }
    }
}