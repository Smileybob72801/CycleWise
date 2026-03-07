package com.veleda.cyclewise.ui.utils

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Robolectric-based tests for [isReducedMotionEnabled].
 */
@RunWith(RobolectricTestRunner::class)
class AccessibilityUtilsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `isReducedMotionEnabled WHEN animator scale is default THEN returns false`() {
        // GIVEN — default animator duration scale (1.0)
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )

        // WHEN
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    val reduced = isReducedMotionEnabled()
                    Text(text = if (reduced) "reduced" else "normal")
                }
            }
        }

        // THEN
        composeTestRule.onNodeWithText("normal").assertIsDisplayed()
    }

    @Test
    fun `isReducedMotionEnabled WHEN animator scale is zero THEN returns true`() {
        // GIVEN — animator duration scale disabled
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )

        // WHEN
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    val reduced = isReducedMotionEnabled()
                    Text(text = if (reduced) "reduced" else "normal")
                }
            }
        }

        // THEN
        composeTestRule.onNodeWithText("reduced").assertIsDisplayed()
    }
}
