package com.veleda.cyclewise.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Simple screen to enter passphrase and unlock the DB.
 */
@Composable
fun PassphraseScreen(
    onPassphraseEntered: (String) -> Unit
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
            onClick = { onPassphraseEntered(passphrase) },
            enabled = passphrase.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unlock")
        }
    }
}