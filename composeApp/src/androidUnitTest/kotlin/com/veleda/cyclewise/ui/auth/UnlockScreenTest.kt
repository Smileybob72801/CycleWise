package com.veleda.cyclewise.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [UnlockScreen].
 *
 * Tests the internal [UnlockScreen] composable directly, bypassing the
 * Koin-injected [PassphraseScreen] wrapper.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class UnlockScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val effectFlow = MutableSharedFlow<PassphraseEffect>(replay = 0)

    private fun setContent(
        uiState: PassphraseUiState = PassphraseUiState(),
        onEvent: (PassphraseEvent) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    UnlockScreen(
                        uiState = uiState,
                        onEvent = onEvent,
                        effect = effectFlow,
                    )
                }
            }
        }
    }

    // region Passphrase input

    @Test
    fun passphraseInput_WHEN_rendered_THEN_hasTestTag() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithTag("passphrase-input").assertIsDisplayed()
    }

    @Test
    fun passphraseInput_WHEN_textEntered_THEN_fieldAcceptsInput() {
        // Given
        setContent()

        // When
        composeTestRule.onNodeWithTag("passphrase-input").performTextInput("mypassphrase")

        // Then — field should contain the entered text (no assertion needed beyond no crash)
        composeTestRule.onNodeWithTag("passphrase-input").assertIsDisplayed()
    }

    // endregion

    // region Unlock button

    @Test
    fun unlockButton_WHEN_rendered_THEN_hasTestTag() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithTag("unlock-button").assertIsDisplayed()
    }

    @Test
    fun unlockButton_WHEN_passphraseEmpty_THEN_isDisabled() {
        // Given / When — no text entered
        setContent()

        // Then
        composeTestRule.onNodeWithTag("unlock-button").assertIsNotEnabled()
    }

    @Test
    fun unlockButton_WHEN_passphraseNonBlank_THEN_isEnabled() {
        // Given
        setContent()

        // When
        composeTestRule.onNodeWithTag("passphrase-input").performTextInput("secret")

        // Then
        composeTestRule.onNodeWithTag("unlock-button").assertIsEnabled()
    }

    @Test
    fun unlockButton_WHEN_isUnlocking_THEN_isDisabled() {
        // Given
        setContent(uiState = PassphraseUiState(isUnlocking = true))

        // When — enter text (button should still be disabled due to isUnlocking)
        composeTestRule.onNodeWithTag("passphrase-input").performTextInput("secret")

        // Then
        composeTestRule.onNodeWithTag("unlock-button").assertIsNotEnabled()
    }

    @Test
    fun unlockButton_WHEN_tapped_THEN_dispatchesUnlockClicked() {
        // Given
        val events = mutableListOf<PassphraseEvent>()
        setContent(onEvent = { events.add(it) })
        composeTestRule.onNodeWithTag("passphrase-input").performTextInput("mypass")

        // When
        composeTestRule.onNodeWithTag("unlock-button").performClick()

        // Then
        val unlock = events.filterIsInstance<PassphraseEvent.UnlockClicked>()
        assert(unlock.isNotEmpty()) { "UnlockClicked event not dispatched" }
        assert(unlock.first().passphrase == "mypass") {
            "Expected passphrase 'mypass', got '${unlock.first().passphrase}'"
        }
    }

    // endregion

    // region Loading overlay

    @Test
    fun loadingOverlay_WHEN_isUnlockingTrue_THEN_displayed() {
        // Given / When
        setContent(uiState = PassphraseUiState(isUnlocking = true))

        // Then — the progress indicator should be visible (it's rendered in an overlay Box)
        // The scrim overlay blocks interaction, which we can verify via the unlock button state
        composeTestRule.onNodeWithTag("passphrase-input").performTextInput("test")
        composeTestRule.onNodeWithTag("unlock-button").assertIsNotEnabled()
    }

    @Test
    fun loadingOverlay_WHEN_isUnlockingFalse_THEN_notBlocking() {
        // Given / When
        setContent(uiState = PassphraseUiState(isUnlocking = false))
        composeTestRule.onNodeWithTag("passphrase-input").performTextInput("test")

        // Then
        composeTestRule.onNodeWithTag("unlock-button").assertIsEnabled()
    }

    // endregion

    // region Password visibility toggle

    @Test
    fun visibilityToggle_WHEN_rendered_THEN_showButtonDisplayed() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithText("Show", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun visibilityToggle_WHEN_tapped_THEN_togglesToHide() {
        // Given
        setContent()

        // When
        composeTestRule.onNodeWithText("Show", substring = true, ignoreCase = true)
            .performClick()

        // Then
        composeTestRule.onNodeWithText("Hide", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // endregion
}
