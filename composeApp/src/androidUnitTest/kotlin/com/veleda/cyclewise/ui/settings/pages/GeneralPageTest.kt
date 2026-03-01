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
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.settings.SettingsUiState
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
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
        uiState: SettingsUiState = SettingsUiState(),
        onEvent: (SettingsEvent) -> Unit = {},
        session: Scope? = null,
        onLockNow: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    GeneralPage(
                        uiState = uiState,
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
        setContent(uiState = SettingsUiState(autolockMinutes = 10))
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
        composeTestRule.onAllNodesWithText("1")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("5")[0].assertIsDisplayed()
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
        setContent(uiState = SettingsUiState(showPrivacyPolicyDialog = true))
        composeTestRule.onNodeWithText("Close", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun termsOfServiceDialog_WHEN_showTrue_THEN_isDisplayed() {
        setContent(uiState = SettingsUiState(showTermsOfServiceDialog = true))
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
        setContent(uiState = SettingsUiState(showDeleteFirstConfirmation = true))
        composeTestRule.onAllNodesWithText("Cancel")[0].assertIsDisplayed()
    }

    @Test
    fun secondConfirmDialog_WHEN_showTrue_THEN_textFieldDisplayed() {
        setContent(uiState = SettingsUiState(showDeleteSecondConfirmation = true))
        composeTestRule.onNodeWithText("Type DELETE to confirm").assertIsDisplayed()
    }

    @Test
    fun secondConfirmDialog_WHEN_textNotDelete_THEN_confirmButtonDisabled() {
        setContent(
            uiState = SettingsUiState(
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
        setContent(uiState = SettingsUiState(isDeletingData = true))
        composeTestRule.onNodeWithText("Deleting Data", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // endregion
}
