package com.veleda.cyclewise.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun PassphraseScreen(
    onPassphraseEntered: () -> Unit
) {
    val viewModel: PassphraseViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Local state for the text field; it doesn't need to live in the ViewModel.
    var passphrase by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PassphraseEffect.NavigateToTracker -> onPassphraseEntered()
                is PassphraseEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isUnlocking) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.passphrase_unlocking))
        } else {
            val waterViewModel: WaterTrackerViewModel = koinViewModel()
            val waterState by waterViewModel.uiState.collectAsState()
            WaterTrackerCounter(
                cups = waterState.todayCups,
                onIncrement = waterViewModel::onIncrement,
                onDecrement = waterViewModel::onDecrement,
                yesterdayMessage = waterState.yesterdayMessage
            )
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.passphrase_instruction), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text(stringResource(R.string.passphrase_label)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("passphrase-input")
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.onEvent(PassphraseEvent.UnlockClicked(passphrase))
                },
                enabled = passphrase.isNotBlank() && !uiState.isUnlocking,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("unlock-button")
            ) {
                Text(stringResource(R.string.passphrase_unlock))
            }
        }
    }
}