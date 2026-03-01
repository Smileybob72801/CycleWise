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
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.coroutines.flow.SharedFlow
import org.koin.androidx.compose.koinViewModel

/** Alpha for the scrim overlay shown while the passphrase is being verified. */
internal const val SCRIM_ALPHA = 0.5f

/**
 * Passphrase screen — entry point for both first-time setup and returning unlock.
 *
 * Branches on [PassphraseUiState.isFirstTime]:
 * - `true` → renders [SetupScreen] (onboarding pager with passphrase creation).
 * - `false` → renders the existing unlock UI with passphrase field and Unlock button.
 *
 * Both paths share the same [onPassphraseEntered] callback so navigation works
 * identically after a successful unlock.
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

    // Collect one-time effects from the ViewModel (shared by both paths)
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PassphraseEffect.NavigateToTracker -> onPassphraseEntered()
                is PassphraseEffect.ShowError -> {
                    // ShowError is handled within each sub-screen
                }
            }
        }
    }

    // Wait for DataStore to resolve before rendering to avoid a flash
    if (!uiState.isFirstTimeLoaded) return

    if (uiState.isFirstTime) {
        SetupScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent,
        )
    } else {
        UnlockScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            effect = viewModel.effect,
        )
    }
}

/**
 * Existing unlock UI for returning users.
 *
 * Displays the app logo with an entrance animation, a passphrase text field with
 * visibility toggle, inline error feedback, and a collapsible water-tracking widget.
 * A semi-transparent loading overlay prevents interaction during unlock.
 */
@Composable
private fun UnlockScreen(
    uiState: PassphraseUiState,
    onEvent: (PassphraseEvent) -> Unit,
    effect: SharedFlow<PassphraseEffect>,
) {
    val dims = LocalDimensions.current

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

    // Collect ShowError effects for the unlock screen
    LaunchedEffect(Unit) {
        effect.collect { e ->
            if (e is PassphraseEffect.ShowError) {
                errorMessage = errorString
            }
        }
    }

    val submit = {
        if (passphrase.isNotBlank() && !uiState.isUnlocking) {
            onEvent(PassphraseEvent.UnlockClicked(passphrase))
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
                .padding(dims.xl),
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
                    modifier = Modifier.size(dims.iconXl)
                )
            }
            Spacer(Modifier.height(dims.lg))

            // App name and instruction
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(R.string.passphrase_instruction),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(dims.lg))

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
            Spacer(Modifier.height(dims.lg))

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
                    contentDescription = stringResource(R.string.cd_passphrase_unlock),
                    modifier = Modifier.size(dims.md)
                )
                Spacer(Modifier.size(dims.sm))
                Text(stringResource(R.string.passphrase_unlock))
            }
            Spacer(Modifier.height(dims.md))

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
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
