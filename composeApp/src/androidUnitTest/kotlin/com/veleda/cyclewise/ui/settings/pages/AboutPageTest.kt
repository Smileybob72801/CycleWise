package com.veleda.cyclewise.ui.settings.pages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.ui.settings.AboutSettingsState
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [AboutPage].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class AboutPageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        state: AboutSettingsState = AboutSettingsState(),
        onEvent: (SettingsEvent) -> Unit = {},
        isSessionActive: Boolean = false,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    AboutPage(
                        state = state,
                        onEvent = onEvent,
                        isSessionActive = isSessionActive,
                    )
                }
            }
        }
    }

    // region About section

    @Test
    fun aboutSection_WHEN_rendered_THEN_appNameDisplayed() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithText("RhythmWise", substring = true).assertIsDisplayed()
    }

    @Test
    fun aboutSection_WHEN_rendered_THEN_descriptionDisplayed() {
        // Given / When
        setContent()

        // Then — the about description should be visible
        composeTestRule.onNodeWithText("privacy-first", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun aboutItem_WHEN_tapped_THEN_dispatchesShowAboutDialog() {
        // Given
        val events = mutableListOf<SettingsEvent>()
        setContent(onEvent = { events.add(it) })

        // When
        composeTestRule.onNodeWithText("RhythmWise", substring = true).performClick()

        // Then
        assert(events.any { it is SettingsEvent.ShowAboutDialog }) {
            "Expected ShowAboutDialog event"
        }
    }

    @Test
    fun aboutDialog_WHEN_showTrue_THEN_versionDisplayed() {
        // Given / When
        setContent(state = AboutSettingsState(showAboutDialog = true))

        // Then — dialog content with version should be visible
        composeTestRule.onNodeWithText("Version", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun aboutDialog_WHEN_closeButtonTapped_THEN_dispatchesDismiss() {
        // Given
        val events = mutableListOf<SettingsEvent>()
        setContent(
            state = AboutSettingsState(showAboutDialog = true),
            onEvent = { events.add(it) },
        )

        // When
        composeTestRule.onNodeWithText("Close", substring = true, ignoreCase = true)
            .performClick()

        // Then
        assert(events.any { it is SettingsEvent.DismissAboutDialog }) {
            "Expected DismissAboutDialog event"
        }
    }

    // endregion

    // region Health content section

    @Test
    fun healthContentSection_WHEN_rendered_THEN_disclaimerDisplayed() {
        // Given / When
        setContent()

        // Then — section title is "About Health Content"
        composeTestRule.onNodeWithText("About Health Content").assertIsDisplayed()
    }

    // endregion

    // region Developer section

    @Test
    fun developerSection_WHEN_sessionNull_THEN_lockedMessageDisplayed() {
        // Given / When
        setContent(isSessionActive = false)

        // Then — in debug builds, the developer section shows a locked message (may need scroll)
        composeTestRule.onNodeWithText("Unlock the app to use developer tools.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // endregion
}
