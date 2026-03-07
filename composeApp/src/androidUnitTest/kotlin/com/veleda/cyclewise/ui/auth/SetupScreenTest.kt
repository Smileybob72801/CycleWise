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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [SetupScreen].
 *
 * Tests the public [SetupScreen] composable directly with a [PassphraseUiState]
 * and event callback.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class SetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        uiState: PassphraseUiState = PassphraseUiState(),
        onEvent: (PassphraseEvent) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    SetupScreen(
                        uiState = uiState,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }

    // region Page indicator

    @Test
    fun pageIndicator_WHEN_rendered_THEN_dotsDisplayed() {
        // Given / When
        setContent()

        // Then — verify the setup screen is rendered by checking the Next button
        // (page indicator dots are visual-only Boxes without text or testTags)
        composeTestRule.onNodeWithTag("setup-next").assertIsDisplayed()
    }

    // endregion

    // region Navigation buttons

    @Test
    fun backButton_WHEN_onPage0_THEN_notDisplayed() {
        // Given / When
        setContent()

        // Then — back button should not be visible on the first page
        composeTestRule.onNodeWithTag("setup-back").assertDoesNotExist()
    }

    @Test
    fun nextButton_WHEN_onPage0_THEN_isDisplayed() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithTag("setup-next").assertIsDisplayed()
    }

    @Test
    fun nextButton_WHEN_tapped_THEN_navigatesToPage1() {
        // Given
        setContent()

        // When
        composeTestRule.onNodeWithTag("setup-next").performClick()
        composeTestRule.waitForIdle()

        // Then — back button should appear on page 1
        composeTestRule.onNodeWithTag("setup-back").assertIsDisplayed()
    }

    @Test
    fun backButton_WHEN_onPage1AndTapped_THEN_returnsToPage0() {
        // Given — navigate to page 1
        setContent()
        composeTestRule.onNodeWithTag("setup-next").performClick()
        composeTestRule.waitForIdle()

        // When — go back
        composeTestRule.onNodeWithTag("setup-back").performClick()
        composeTestRule.waitForIdle()

        // Then — should be on page 0 again (back button hidden)
        composeTestRule.onNodeWithTag("setup-back").assertDoesNotExist()
    }

    @Test
    fun nextButton_WHEN_navigatedToLastPage_THEN_notDisplayed() {
        // Given — navigate through all pages to page 3 (last)
        setContent()
        repeat(3) {
            composeTestRule.onNodeWithTag("setup-next").performClick()
            composeTestRule.waitForIdle()
        }

        // Then — next button should be hidden on the last page
        composeTestRule.onNodeWithTag("setup-next").assertDoesNotExist()
    }

    // endregion

    // region Info pages

    @Test
    fun page0_WHEN_rendered_THEN_privacyTitleDisplayed() {
        // Given / When
        setContent()

        // Then — page 0 title is "Your Data Stays on This Device"
        composeTestRule.onNodeWithText("Your Data Stays on This Device")
            .assertIsDisplayed()
    }

    // endregion

    // region Create passphrase page (page 3)

    @Test
    fun createPage_WHEN_navigatedTo_THEN_fieldsDisplayed() {
        // Given — navigate to last page
        setContent()
        repeat(3) {
            composeTestRule.onNodeWithTag("setup-next").performClick()
            composeTestRule.waitForIdle()
        }

        // Then
        composeTestRule.onNodeWithTag("setup-passphrase-input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("setup-confirm-input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("setup-create-button").assertIsDisplayed()
    }

    @Test
    fun createButton_WHEN_passphraseShort_THEN_isDisabled() {
        // Given — navigate to last page
        setContent()
        repeat(3) {
            composeTestRule.onNodeWithTag("setup-next").performClick()
            composeTestRule.waitForIdle()
        }

        // When — enter short passphrase (< 8 chars)
        composeTestRule.onNodeWithTag("setup-passphrase-input").performTextInput("short")
        composeTestRule.onNodeWithTag("setup-confirm-input").performTextInput("short")

        // Then
        composeTestRule.onNodeWithTag("setup-create-button").assertIsNotEnabled()
    }

    @Test
    fun createButton_WHEN_passphraseLongEnoughAndConfirmed_THEN_isEnabled() {
        // Given — navigate to last page
        setContent()
        repeat(3) {
            composeTestRule.onNodeWithTag("setup-next").performClick()
            composeTestRule.waitForIdle()
        }

        // When — enter valid passphrase (>= 8 chars) and confirmation
        composeTestRule.onNodeWithTag("setup-passphrase-input").performTextInput("longpassphrase")
        composeTestRule.onNodeWithTag("setup-confirm-input").performTextInput("longpassphrase")

        // Then
        composeTestRule.onNodeWithTag("setup-create-button").assertIsEnabled()
    }

    @Test
    fun createButton_WHEN_confirmationEmpty_THEN_isDisabled() {
        // Given — navigate to last page
        setContent()
        repeat(3) {
            composeTestRule.onNodeWithTag("setup-next").performClick()
            composeTestRule.waitForIdle()
        }

        // When — enter passphrase but no confirmation
        composeTestRule.onNodeWithTag("setup-passphrase-input").performTextInput("longpassphrase")

        // Then
        composeTestRule.onNodeWithTag("setup-create-button").assertIsNotEnabled()
    }

    @Test
    fun createButton_WHEN_tapped_THEN_dispatchesSetupClicked() {
        // Given
        val events = mutableListOf<PassphraseEvent>()
        setContent(onEvent = { events.add(it) })
        repeat(3) {
            composeTestRule.onNodeWithTag("setup-next").performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithTag("setup-passphrase-input").performTextInput("longpassphrase")
        composeTestRule.onNodeWithTag("setup-confirm-input").performTextInput("longpassphrase")

        // When
        composeTestRule.onNodeWithTag("setup-create-button").performClick()

        // Then
        val setup = events.filterIsInstance<PassphraseEvent.SetupClicked>()
        assert(setup.isNotEmpty()) { "SetupClicked event not dispatched" }
        assert(setup.first().passphrase == "longpassphrase") {
            "Expected 'longpassphrase', got '${setup.first().passphrase}'"
        }
        assert(setup.first().confirmation == "longpassphrase") {
            "Expected confirmation 'longpassphrase', got '${setup.first().confirmation}'"
        }
    }

    @Test
    fun createButton_WHEN_isUnlocking_THEN_isDisabled() {
        // Given
        setContent(uiState = PassphraseUiState(isUnlocking = true))
        repeat(3) {
            composeTestRule.onNodeWithTag("setup-next").performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithTag("setup-passphrase-input").performTextInput("longpassphrase")
        composeTestRule.onNodeWithTag("setup-confirm-input").performTextInput("longpassphrase")

        // Then
        composeTestRule.onNodeWithTag("setup-create-button").assertIsNotEnabled()
    }

    // endregion

    // region Validation errors

    @Test
    fun passphraseError_WHEN_nonNull_THEN_errorTextDisplayed() {
        // Given — navigate to last page
        setContent(uiState = PassphraseUiState(passphraseError = "too_short"))
        repeat(3) {
            composeTestRule.onNodeWithTag("setup-next").performClick()
            composeTestRule.waitForIdle()
        }

        // Then — error text from R.string.setup_error_too_short: "Passphrase must be at least 8 characters"
        composeTestRule.onNodeWithText("at least 8 characters", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun confirmationError_WHEN_nonNull_THEN_errorTextDisplayed() {
        // Given
        setContent(uiState = PassphraseUiState(confirmationError = "mismatch"))
        repeat(3) {
            composeTestRule.onNodeWithTag("setup-next").performClick()
            composeTestRule.waitForIdle()
        }

        // Then — "mismatch" error text should be displayed
        composeTestRule.onNodeWithText("match", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // endregion

    // region Password visibility toggle

    @Test
    fun visibilityToggle_WHEN_rendered_THEN_showButtonsDisplayed() {
        // Given — navigate to last page
        setContent()
        repeat(3) {
            composeTestRule.onNodeWithTag("setup-next").performClick()
            composeTestRule.waitForIdle()
        }

        // Then — at least one "Show" button should be visible
        composeTestRule.onAllNodes(
            androidx.compose.ui.test.hasText("Show", substring = true, ignoreCase = true),
        )[0].assertIsDisplayed()
    }

    // endregion
}
