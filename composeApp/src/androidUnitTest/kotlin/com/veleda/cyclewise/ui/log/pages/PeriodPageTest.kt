package com.veleda.cyclewise.ui.log.pages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.ui.coachmark.HintKey
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodConsistency
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [PeriodPage].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class PeriodPageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        isPeriodDay: Boolean = false,
        flowIntensity: FlowIntensity? = null,
        periodColor: PeriodColor? = null,
        periodConsistency: PeriodConsistency? = null,
        onPeriodToggled: (Boolean) -> Unit = {},
        onFlowChanged: (FlowIntensity?) -> Unit = {},
        onColorChanged: (PeriodColor?) -> Unit = {},
        onConsistencyChanged: (PeriodConsistency?) -> Unit = {},
        onDone: () -> Unit = {},
        onShowEducationalSheet: (String) -> Unit = {},
        activeHintKey: HintKey? = null,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    PeriodPage(
                        isPeriodDay = isPeriodDay,
                        flowIntensity = flowIntensity,
                        periodColor = periodColor,
                        periodConsistency = periodConsistency,
                        onPeriodToggled = onPeriodToggled,
                        onFlowChanged = onFlowChanged,
                        onColorChanged = onColorChanged,
                        onConsistencyChanged = onConsistencyChanged,
                        onDone = onDone,
                        onShowEducationalSheet = onShowEducationalSheet,
                        activeHintKey = activeHintKey,
                    )
                }
            }
        }
    }

    // region Period toggle

    @Test
    fun periodToggle_WHEN_isPeriodDayFalse_THEN_switchIsOff() {
        setContent(isPeriodDay = false)
        composeTestRule.onNodeWithTag("period_toggle").assertIsOff()
    }

    @Test
    fun periodToggle_WHEN_isPeriodDayTrue_THEN_switchIsOn() {
        setContent(isPeriodDay = true)
        composeTestRule.onNodeWithTag("period_toggle").assertIsOn()
    }

    @Test
    fun periodToggle_WHEN_tapped_THEN_invokesCallback() {
        var captured: Boolean? = null
        setContent(isPeriodDay = false, onPeriodToggled = { captured = it })
        composeTestRule.onNodeWithTag("period_toggle").performClick()
        assert(captured == true) { "Expected onPeriodToggled(true), got $captured" }
    }

    // endregion

    // region Detail sections visibility

    @Test
    fun flowSection_WHEN_isPeriodDayFalse_THEN_notDisplayed() {
        setContent(isPeriodDay = false)
        // FlowIntensity chip labels are ALL CAPS enum names
        composeTestRule.onNodeWithText("LIGHT").assertDoesNotExist()
        composeTestRule.onNodeWithText("MEDIUM").assertDoesNotExist()
        composeTestRule.onNodeWithText("HEAVY").assertDoesNotExist()
    }

    @Test
    fun flowSection_WHEN_isPeriodDayTrue_THEN_allChipsDisplayed() {
        setContent(isPeriodDay = true)
        // FlowIntensity chip labels: intensity.name.replaceFirstChar { it.uppercase() } → ALL CAPS
        composeTestRule.onNodeWithText("LIGHT").assertIsDisplayed()
        composeTestRule.onNodeWithText("MEDIUM").assertIsDisplayed()
        composeTestRule.onNodeWithText("HEAVY").assertIsDisplayed()
    }

    @Test
    fun colorSection_WHEN_isPeriodDayTrue_THEN_chipLabelsDisplayed() {
        setContent(isPeriodDay = true)
        // PeriodColor uses string resources
        composeTestRule.onNodeWithText("Pink").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Brown").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun consistencySection_WHEN_isPeriodDayTrue_THEN_chipLabelsDisplayed() {
        setContent(isPeriodDay = true)
        // PeriodConsistency uses string resources — may be below fold
        composeTestRule.onNodeWithText("Thin").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Moderate").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Thick").performScrollTo().assertIsDisplayed()
    }

    // endregion

    // region Empty state message

    @Test
    fun emptyMessage_WHEN_isPeriodDayFalse_THEN_displayed() {
        setContent(isPeriodDay = false)
        composeTestRule.onNodeWithText("Toggle above to log period details")
            .assertIsDisplayed()
    }

    @Test
    fun emptyMessage_WHEN_isPeriodDayTrue_THEN_notDisplayed() {
        setContent(isPeriodDay = true)
        composeTestRule.onNodeWithText("Toggle above to log period details")
            .assertDoesNotExist()
    }

    // endregion

    // region Flow chip callbacks

    @Test
    fun flowChip_WHEN_lightTapped_THEN_invokesCallbackWithLight() {
        var captured: FlowIntensity? = null
        setContent(isPeriodDay = true, onFlowChanged = { captured = it })
        composeTestRule.onNodeWithText("LIGHT").performClick()
        assert(captured == FlowIntensity.LIGHT) { "Expected LIGHT, got $captured" }
    }

    @Test
    fun flowChip_WHEN_alreadySelectedAndTapped_THEN_invokesCallbackWithNull() {
        var captured: FlowIntensity? = FlowIntensity.MEDIUM
        setContent(
            isPeriodDay = true,
            flowIntensity = FlowIntensity.MEDIUM,
            onFlowChanged = { captured = it },
        )
        composeTestRule.onNodeWithText("MEDIUM").performClick()
        assert(captured == null) { "Expected null (deselect), got $captured" }
    }

    // endregion

    // region Color chip callbacks

    @Test
    fun colorChip_WHEN_pinkTapped_THEN_invokesCallback() {
        var captured: PeriodColor? = null
        setContent(isPeriodDay = true, onColorChanged = { captured = it })
        composeTestRule.onNodeWithText("Pink").performScrollTo().performClick()
        assert(captured == PeriodColor.PINK) { "Expected PINK, got $captured" }
    }

    // endregion

    // region Consistency chip callbacks

    @Test
    fun consistencyChip_WHEN_thinTapped_THEN_invokesCallback() {
        var captured: PeriodConsistency? = null
        setContent(isPeriodDay = true, onConsistencyChanged = { captured = it })
        composeTestRule.onNodeWithText("Thin").performScrollTo().performClick()
        assert(captured == PeriodConsistency.THIN) { "Expected THIN, got $captured" }
    }

    // endregion

    // region Info buttons

    @Test
    fun flowInfoButton_WHEN_tapped_THEN_invokesEducationalSheet() {
        var capturedTag: String? = null
        setContent(isPeriodDay = true, onShowEducationalSheet = { capturedTag = it })
        // SectionCard info button CD is "Learn more about Flow"
        composeTestRule.onNode(
            hasContentDescription("Learn more about Flow"),
            useUnmergedTree = true,
        ).performClick()
        assert(capturedTag == "FlowIntensity") { "Expected 'FlowIntensity', got '$capturedTag'" }
    }

    // endregion

    // region Toggle label display

    @Test
    fun periodToggleLabel_WHEN_rendered_THEN_isDisplayed() {
        setContent()
        // Exact string from R.string.daily_log_period_toggle
        composeTestRule.onNodeWithText("I\u0027m on my period today").assertIsDisplayed()
    }

    // endregion

    // region Walkthrough disabled toggle

    @Test
    fun periodToggle_WHEN_activeHintIsNotToggle_THEN_switchIsDisabled() {
        // Given — walkthrough active on a non-toggle step
        setContent(isPeriodDay = false, activeHintKey = HintKey.DAILY_LOG_MOOD)

        // Then
        composeTestRule.onNodeWithTag("period_toggle").assertIsNotEnabled()
    }

    @Test
    fun periodToggle_WHEN_activeHintIsToggle_THEN_switchIsEnabled() {
        // Given — walkthrough active on the PERIOD_TOGGLE step
        var captured: Boolean? = null
        setContent(
            isPeriodDay = false,
            activeHintKey = HintKey.DAILY_LOG_PERIOD_TOGGLE,
            onPeriodToggled = { captured = it },
        )

        // When
        composeTestRule.onNodeWithTag("period_toggle").performClick()

        // Then
        assert(captured == true) { "Expected onPeriodToggled(true), got $captured" }
    }

    // endregion

    // region Done button

    @Test
    fun doneButton_WHEN_tapped_THEN_invokesCallback() {
        // Given
        var invoked = false
        setContent(onDone = { invoked = true })

        // When
        composeTestRule.onNodeWithText("Done")
            .performScrollTo()
            .performClick()

        // Then
        assert(invoked) { "onDone was not invoked" }
    }

    // endregion
}
