package com.veleda.cyclewise.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.androidData.local.database.CycleDatabase
import com.veleda.cyclewise.di.SESSION_SCOPE
import org.koin.java.KoinJavaComponent.getKoin
import org.koin.core.qualifier.named
import org.koin.*
import org.koin.core.parameter.parametersOf

/**
 * Simple screen to enter passphrase and unlock the DB.
 */
@Composable
fun PassphraseScreen(
    onPassphraseEntered: () -> Unit
) {
    var passphrase by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
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
                val koin = getKoin()

                // Create the unlocked session scope
                val scope = koin.getScopeOrNull("session")
                    ?: koin.createScope("session", SESSION_SCOPE)

                // Force DB creation inside this scope with the passphrase
                scope.get<CycleDatabase> { parametersOf(passphrase) }

                // Navigate to Tracker
                onPassphraseEntered()
            },
            enabled = passphrase.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unlock")
        }
    }
}