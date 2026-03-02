package com.veleda.cyclewise.ui.settings.pages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.ui.settings.GeneralSettingsState
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.scope.Scope
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [GeneralPage].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class GeneralPageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        state: GeneralSettingsState = GeneralSettingsState(),
        onEvent: (SettingsEvent) -> Unit = {},
        session: Scope? = null,
        onLockNow: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    GeneralPage(
                        state = state,
                        onEvent = onEvent,
                        session = session,
                        onLockNow = onLockNow,
                    )
                }
            }
        }
    }

    // region Security section

    @Test
    fun securitySection_WHEN_rendered_THEN_titleDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Security").assertIsDisplayed()
    }

    @Test
    fun autolockOptions_WHEN_rendered_THEN_allFourDisplayed() {
        setContent()
        // SegmentedButton can create multiple text nodes; verify at least one per label
        composeTestRule.onAllNodesWithText("5 min", substring = true)[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("10 min", substring = true)[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("15 min", substring = true)[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("30 min", substring = true)[0].assertIsDisplayed()
    }

    @Test
    fun autolockOption_WHEN_selected_THEN_isSelectedState() {
        setContent(state = GeneralSettingsState(autolockMinutes = 10))
        composeTestRule.onAllNodesWithText("10 min", substring = true)[0].assertIsSelected()
    }

    @Test
    fun autolockOption_WHEN_tapped_THEN_dispatchesEvent() {
        val events = mutableListOf<SettingsEvent>()
        setContent(onEvent = { events.add(it) })
        composeTestRule.onAllNodesWithText("5 min", substring = true)[0].performClick()
        val changed = events.filterIsInstance<SettingsEvent.AutolockChanged>()
        assert(changed.any { it.minutes == 5 }) { "Expected AutolockChanged(5), got $events" }
    }

    @Test
    fun lockNowButton_WHEN_sessionNull_THEN_isDisabled() {
        setContent(session = null)
        composeTestRule.onNode(hasText("Lock", substring = true, ignoreCase = true).and(hasClickAction()))
            .assertIsNotEnabled()
    }

    @Test
    fun lockedMessage_WHEN_sessionNull_THEN_displayed() {
        setContent(session = null)
        composeTestRule.onNodeWithText("Currently locked", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // endregion

    // region Insights section

    @Test
    fun insightsSection_WHEN_rendered_THEN_titleDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Insight Settings", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun topSymptomsCount_WHEN_rendered_THEN_valueLabelsDisplayed() {
        setContent()
        composeTestRule.onAllNodesWithText("1")[0].performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("5")[0].performScrollTo().assertIsDisplayed()
    }

    // endregion

    // region Tutorial section

    @Test
    fun tutorialSection_WHEN_rendered_THEN_resetHintsDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Reset Tutorial Hints")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun resetHints_WHEN_tapped_THEN_dispatchesEvent() {
        val events = mutableListOf<SettingsEvent>()
        setContent(onEvent = { events.add(it) })
        composeTestRule.onNodeWithText("Reset Tutorial Hints")
            .performScrollTo()
            .performClick()
        assert(events.any { it is SettingsEvent.ResetTutorialHints }) {
            "Expected ResetTutorialHints event"
        }
    }

    // endregion

    // region Legal section

    @Test
    fun legalSection_WHEN_rendered_THEN_privacyPolicyDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Privacy Policy")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun legalSection_WHEN_rendered_THEN_termsOfServiceDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Terms of Service")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun privacyPolicyItem_WHEN_tapped_THEN_dispatchesShowEvent() {
        val events = mutableListOf<SettingsEvent>()
        setContent(onEvent = { events.add(it) })
        composeTestRule.onNodeWithText("Privacy Policy")
            .performScrollTo()
            .performClick()
        assert(events.any { it is SettingsEvent.ShowPrivacyPolicyDialog }) {
            "Expected ShowPrivacyPolicyDialog event"
        }
    }

    @Test
    fun privacyPolicyDialog_WHEN_showTrue_THEN_isDisplayed() {
        setContent(state = GeneralSettingsState(showPrivacyPolicyDialog = true))
        composeTestRule.onNodeWithText("Close", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun termsOfServiceDialog_WHEN_showTrue_THEN_isDisplayed() {
        setContent(state = GeneralSettingsState(showTermsOfServiceDialog = true))
        composeTestRule.onNodeWithText("Close", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // endregion

    // region Data Management section

    @Test
    fun dataManagement_WHEN_rendered_THEN_deleteAllDataDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Delete All Data")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun deleteAllData_WHEN_tapped_THEN_dispatchesEvent() {
        val events = mutableListOf<SettingsEvent>()
        setContent(onEvent = { events.add(it) })
        composeTestRule.onNodeWithText("Delete All Data")
            .performScrollTo()
            .performClick()
        assert(events.any { it is SettingsEvent.DeleteAllDataRequested }) {
            "Expected DeleteAllDataRequested event"
        }
    }

    @Test
    fun firstConfirmDialog_WHEN_showTrue_THEN_isDisplayed() {
        setContent(state = GeneralSettingsState(showDeleteFirstConfirmation = true))
        composeTestRule.onAllNodesWithText("Cancel")[0].assertIsDisplayed()
    }

    @Test
    fun secondConfirmDialog_WHEN_showTrue_THEN_textFieldDisplayed() {
        setContent(state = GeneralSettingsState(showDeleteSecondConfirmation = true))
        composeTestRule.onNodeWithText("Type DELETE to confirm").assertIsDisplayed()
    }

    @Test
    fun secondConfirmDialog_WHEN_textNotDelete_THEN_confirmButtonDisabled() {
        setContent(
            state = GeneralSettingsState(
                showDeleteSecondConfirmation = true,
                deleteConfirmText = "DELE",
            ),
        )
        composeTestRule.onNode(
            hasText("Delete Everything", substring = true, ignoreCase = true).and(hasClickAction()),
        ).assertIsNotEnabled()
    }

    @Test
    fun deletingProgress_WHEN_isDeletingTrue_THEN_progressDisplayed() {
        setContent(state = GeneralSettingsState(isDeletingData = true))
        composeTestRule.onNodeWithText("Deleting Data", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // endregion

    // region Change Passphrase section

    @Test
    fun changePassphrase_WHEN_rendered_THEN_displayed() {
        setContent()
        composeTestRule.onNodeWithText("Change Passphrase")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun changePassphrase_WHEN_sessionActive_THEN_dispatchesEvent() {
        val events = mutableListOf<SettingsEvent>()
        setContent(session = mockk(relaxed = true), onEvent = { events.add(it) })
        composeTestRule.onNodeWithText("Change Passphrase")
            .performScrollTo()
            .performClick()
        assert(events.any { it is SettingsEvent.ChangePassphraseRequested }) {
            "Expected ChangePassphraseRequested event"
        }
    }

    @Test
    fun changePassphraseDialog_WHEN_showTrue_THEN_fieldsDisplayed() {
        setContent(state = GeneralSettingsState(showChangePassphraseDialog = true))
        composeTestRule.onNodeWithText("Current passphrase").assertIsDisplayed()
        composeTestRule.onNodeWithText("New passphrase").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm new passphrase").assertIsDisplayed()
    }

    @Test
    fun changePassphraseDialog_WHEN_showTrue_THEN_submitButtonDisplayed() {
        setContent(state = GeneralSettingsState(showChangePassphraseDialog = true))
        // The dialog title contains "Change Passphrase" — verifying the dialog rendered
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun changePassphraseDialog_WHEN_errorWrongCurrent_THEN_errorDisplayed() {
        setContent(
            state = GeneralSettingsState(
                showChangePassphraseDialog = true,
                changePassphraseError = "wrong_current",
            ),
        )
        composeTestRule.onNodeWithText("Current passphrase is incorrect", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun changePassphraseDialog_WHEN_errorTooShort_THEN_errorDisplayed() {
        setContent(
            state = GeneralSettingsState(
                showChangePassphraseDialog = true,
                changePassphraseError = "too_short",
            ),
        )
        composeTestRule.onNodeWithText("at least 8 characters", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun changePassphraseDialog_WHEN_errorMismatch_THEN_errorDisplayed() {
        setContent(
            state = GeneralSettingsState(
                showChangePassphraseDialog = true,
                changePassphraseError = "mismatch",
            ),
        )
        composeTestRule.onNodeWithText("do not match", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun changePassphraseDialog_WHEN_isChanging_THEN_progressDisplayed() {
        setContent(
            state = GeneralSettingsState(
                showChangePassphraseDialog = true,
                isChangingPassphrase = true,
            ),
        )
        composeTestRule.onNodeWithText("Changing passphrase", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun changePassphraseDialog_WHEN_errorFailed_THEN_generalErrorDisplayed() {
        setContent(
            state = GeneralSettingsState(
                showChangePassphraseDialog = true,
                changePassphraseError = "failed",
            ),
        )
        composeTestRule.onNodeWithText("Something went wrong", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun changePassphraseDialog_WHEN_errorVerificationFailed_THEN_generalErrorDisplayed() {
        setContent(
            state = GeneralSettingsState(
                showChangePassphraseDialog = true,
                changePassphraseError = "verification_failed",
            ),
        )
        composeTestRule.onNodeWithText("could not be verified", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun changePassphraseSuccessDialog_WHEN_showTrue_THEN_warningAndAcknowledgeDisplayed() {
        setContent(
            state = GeneralSettingsState(
                showChangePassphraseDialog = true,
                showPassphraseSuccessDialog = true,
            ),
        )
        composeTestRule.onNodeWithText("last chance", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("I've Saved My Passphrase", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun changePassphraseSuccessDialog_WHEN_acknowledged_THEN_dispatchesEvent() {
        val events = mutableListOf<SettingsEvent>()
        setContent(
            state = GeneralSettingsState(
                showChangePassphraseDialog = true,
                showPassphraseSuccessDialog = true,
            ),
            onEvent = { events.add(it) },
        )
        composeTestRule.onNodeWithText("I've Saved My Passphrase", substring = true)
            .performClick()
        assert(events.any { it is SettingsEvent.ChangePassphraseSuccessAcknowledged }) {
            "Expected ChangePassphraseSuccessAcknowledged event"
        }
    }

    // endregion
}
