package com.veleda.cyclewise.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import org.koin.androidx.compose.koinViewModel

/**
 * Passphrase unlock screen — the first screen the user sees.
 *
 * Displays the app logo with an entrance animation, a passphrase text field with
 * visibility toggle, inline error feedback, and a collapsible water-tracking widget.
 * A semi-transparent loading overlay prevents interaction during unlock.
 *
 * @param onPassphraseEntered Callback invoked when the passphrase is successfully verified
 *   and the encrypted session is ready.
 */
@Composable
fun PassphraseScreen(
    onPassphraseEntered: () -> Unit
) {
    val viewModel: PassphraseViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var passphrase by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var logoVisible by remember { mutableStateOf(false) }
    var showWater by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    val errorString = stringResource(R.string.passphrase_error)

    // Trigger logo entrance animation
    LaunchedEffect(Unit) {
        logoVisible = true
    }

    // Auto-focus the passphrase field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Collect one-time effects from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PassphraseEffect.NavigateToTracker -> onPassphraseEntered()
                is PassphraseEffect.ShowError -> {
                    errorMessage = errorString
                }
            }
        }
    }

    val submit = {
        if (passphrase.isNotBlank() && !uiState.isUnlocking) {
            viewModel.onEvent(PassphraseEvent.UnlockClicked(passphrase))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo with fade + scale animation
            AnimatedVisibility(
                visible = logoVisible,
                enter = fadeIn() + scaleIn(initialScale = 0.8f)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.passphrase_logo_description),
                    modifier = Modifier.size(96.dp)
                )
            }
            Spacer(Modifier.height(24.dp))

            // App name and instruction
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(R.string.passphrase_instruction),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))

            // Passphrase field with visibility toggle and inline error
            OutlinedTextField(
                value = passphrase,
                onValueChange = {
                    passphrase = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.passphrase_label)) },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = stringResource(
                                if (passwordVisible) R.string.passphrase_hide
                                else R.string.passphrase_show
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                isError = errorMessage != null,
                supportingText = if (errorMessage != null) {
                    {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("passphrase-input")
            )
            Spacer(Modifier.height(24.dp))

            // Unlock button
            Button(
                onClick = { submit() },
                enabled = passphrase.isNotBlank() && !uiState.isUnlocking,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("unlock-button")
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.passphrase_unlock))
            }
            Spacer(Modifier.height(16.dp))

            // Collapsible water tracker
            TextButton(onClick = { showWater = !showWater }) {
                Text(stringResource(R.string.passphrase_water_toggle))
            }
            AnimatedVisibility(visible = showWater) {
                val waterViewModel: WaterTrackerViewModel = koinViewModel()
                val waterState by waterViewModel.uiState.collectAsState()
                WaterTrackerCounter(
                    cups = waterState.todayCups,
                    onIncrement = waterViewModel::onIncrement,
                    onDecrement = waterViewModel::onDecrement,
                    yesterdayCupsForPrompt = waterState.yesterdayCupsForPrompt
                )
            }
        }

        // Loading overlay
        if (uiState.isUnlocking) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
