package com.veleda.cyclewise.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.annotation.RawRes
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.components.ContentContainer
import com.veleda.cyclewise.ui.components.LottieAnimationBox
import com.veleda.cyclewise.ui.components.MarkdownText
import com.veleda.cyclewise.ui.components.MedicalDisclaimer
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.coroutines.launch

/** Total number of pages in the onboarding pager. */
private const val SETUP_PAGE_COUNT = 4

/**
 * First-time onboarding screen shown when [PassphraseUiState.isFirstTime] is `true`.
 *
 * Contains a [HorizontalPager] with 4 pages:
 * 1. Privacy explanation ("Your Data Stays on This Device")
 * 2. Passphrase guidance ("Choosing a Passphrase You Will Remember")
 * 3. No-recovery warning ("No Recovery, No Exceptions")
 * 4. Passphrase creation form with validation
 *
 * Navigation between pages is handled by Next/Back buttons and swipe gestures.
 * A page indicator (dots) shows the current position.
 *
 * @param uiState current [PassphraseUiState] for loading overlay and validation errors.
 * @param onEvent callback to dispatch [PassphraseEvent]s to the ViewModel.
 */
@Composable
fun SetupScreen(
    uiState: PassphraseUiState,
    onEvent: (PassphraseEvent) -> Unit,
) {
    val dims = LocalDimensions.current
    val pagerState = rememberPagerState(pageCount = { SETUP_PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()

    // Predictive back: navigate to the previous pager page instead of exiting the app.
    // Disabled on page 0 so the system handles back normally (minimize/exit).
    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .imePadding()
    ) {
        ContentContainer(maxWidth = dims.authMaxWidth) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dims.xl, vertical = dims.lg),
        ) {
            // Page indicator dots
            PageIndicator(
                pageCount = SETUP_PAGE_COUNT,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dims.md),
            )

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> InfoPage(
                        title = stringResource(R.string.setup_page1_title),
                        body = stringResource(R.string.setup_page1_body),
                        illustrationResId = R.raw.anim_onboarding_welcome,
                        illustrationContentDescription = stringResource(R.string.lottie_cd_onboarding_welcome),
                        trailingContent = {
                            MedicalDisclaimer(
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                    )
                    1 -> InfoPage(
                        title = stringResource(R.string.setup_page2_title),
                        body = stringResource(R.string.setup_page2_body),
                        illustrationResId = R.raw.anim_onboarding_privacy,
                        illustrationContentDescription = stringResource(R.string.lottie_cd_onboarding_privacy),
                    )
                    2 -> InfoPage(
                        title = stringResource(R.string.setup_page3_title),
                        body = stringResource(R.string.setup_page3_body),
                        illustrationResId = R.raw.anim_onboarding_tracking,
                        illustrationContentDescription = stringResource(R.string.lottie_cd_onboarding_tracking),
                    )
                    3 -> CreatePassphrasePage(
                        uiState = uiState,
                        onEvent = onEvent,
                    )
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dims.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Back button (hidden on page 1)
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier.testTag("setup-back"),
                    ) {
                        Text(stringResource(R.string.setup_back))
                    }
                } else {
                    Spacer(Modifier.width(dims.buttonMin))
                }

                // Next button (hidden on last page — the create button is on the form)
                if (pagerState.currentPage < SETUP_PAGE_COUNT - 1) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.testTag("setup-next"),
                    ) {
                        Text(stringResource(R.string.setup_next))
                    }
                } else {
                    Spacer(Modifier.width(dims.buttonMin))
                }
            }
        }
        }

        // Loading overlay (same pattern as unlock screen)
        if (uiState.isUnlocking) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimationBox(
                    animationResId = R.raw.anim_loading_general,
                    modifier = Modifier.size(dims.iconLg),
                    contentDescription = stringResource(R.string.lottie_cd_loading),
                )
            }
        }
    }
}

/**
 * Page indicator showing dots for each page, with the current page highlighted.
 *
 * @param pageCount   total number of pages.
 * @param currentPage zero-based index of the current page.
 * @param modifier    modifier applied to the indicator row.
 */
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val color by animateColorAsState(
                targetValue = if (index == currentPage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                label = "page_indicator_$index",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * Educational information page used for pages 1-3 of the onboarding flow.
 *
 * Displays an optional Lottie illustration, a title, and body text. The body is
 * rendered via [MarkdownText] to support `**bold**` and bullet list formatting.
 *
 * @param title                          page title displayed as a headline.
 * @param body                           Markdown-formatted body text.
 * @param illustrationResId              optional raw resource ID for a Lottie animation
 *                                       displayed above the title.
 * @param illustrationContentDescription accessibility description for the illustration.
 * @param trailingContent                optional composable rendered below the body text,
 *                                       e.g. a [MedicalDisclaimer] banner on the first page.
 */
@Composable
private fun InfoPage(
    title: String,
    body: String,
    @RawRes illustrationResId: Int? = null,
    illustrationContentDescription: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val dims = LocalDimensions.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = dims.md),
    ) {
        if (illustrationResId != null) {
            LottieAnimationBox(
                animationResId = illustrationResId,
                modifier = Modifier
                    .size(dims.iconXl)
                    .align(Alignment.CenterHorizontally),
                contentDescription = illustrationContentDescription,
            )
            Spacer(Modifier.height(dims.md))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(dims.md))
        MarkdownText(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (trailingContent != null) {
            Spacer(Modifier.height(dims.md))
            trailingContent()
        }
    }
}

/**
 * Passphrase creation form (page 4 of the onboarding flow).
 *
 * Contains two password fields with visibility toggles, inline validation errors,
 * and a "Create and Unlock" button. Validation is performed by the ViewModel via
 * [PassphraseEvent.SetupClicked].
 *
 * @param uiState current state for validation errors and loading indicator.
 * @param onEvent callback to dispatch [PassphraseEvent.SetupClicked].
 */
@Composable
private fun CreatePassphrasePage(
    uiState: PassphraseUiState,
    onEvent: (PassphraseEvent) -> Unit,
) {
    val dims = LocalDimensions.current
    val focusManager = LocalFocusManager.current
    val confirmFocusRequester = remember { FocusRequester() }

    var passphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var passphraseVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }

    val passphraseErrorText = if (uiState.passphraseError != null) {
        stringResource(R.string.setup_error_too_short)
    } else {
        null
    }
    val confirmationErrorText = if (uiState.confirmationError != null) {
        stringResource(R.string.setup_error_mismatch)
    } else {
        null
    }

    val canSubmit = passphrase.length >= MIN_PASSPHRASE_LENGTH &&
        confirmation.isNotBlank() &&
        !uiState.isUnlocking

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = dims.md),
    ) {
        Text(
            text = stringResource(R.string.setup_page4_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(dims.lg))

        // Create passphrase field
        OutlinedTextField(
            value = passphrase,
            onValueChange = { if (it.length <= MAX_PASSPHRASE_LENGTH) passphrase = it },
            label = { Text(stringResource(R.string.setup_passphrase_label)) },
            visualTransformation = if (passphraseVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(onClick = { passphraseVisible = !passphraseVisible }) {
                    Text(
                        text = stringResource(
                            if (passphraseVisible) R.string.passphrase_hide
                            else R.string.passphrase_show
                        ),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            },
            isError = passphraseErrorText != null,
            supportingText = if (passphraseErrorText != null) {
                {
                    Text(
                        text = passphraseErrorText,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { confirmFocusRequester.requestFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("setup-passphrase-input"),
        )
        Spacer(Modifier.height(dims.md))

        // Confirm passphrase field
        OutlinedTextField(
            value = confirmation,
            onValueChange = { if (it.length <= MAX_PASSPHRASE_LENGTH) confirmation = it },
            label = { Text(stringResource(R.string.setup_confirm_label)) },
            visualTransformation = if (confirmVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(onClick = { confirmVisible = !confirmVisible }) {
                    Text(
                        text = stringResource(
                            if (confirmVisible) R.string.passphrase_hide
                            else R.string.passphrase_show
                        ),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            },
            isError = confirmationErrorText != null,
            supportingText = if (confirmationErrorText != null) {
                {
                    Text(
                        text = confirmationErrorText,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(confirmFocusRequester)
                .testTag("setup-confirm-input"),
        )
        Spacer(Modifier.height(dims.lg))

        // Create and Unlock button
        Button(
            onClick = {
                onEvent(PassphraseEvent.SetupClicked(passphrase, confirmation))
            },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("setup-create-button"),
        ) {
            Text(stringResource(R.string.setup_create_button))
        }
    }
}
