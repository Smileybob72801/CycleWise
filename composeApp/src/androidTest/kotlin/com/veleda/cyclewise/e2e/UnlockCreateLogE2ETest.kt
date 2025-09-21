package com.veleda.cyclewise.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veleda.cyclewise.MainActivity
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest

@RunWith(AndroidJUnit4::class)
class UnlockCreateLogE2ETest : KoinTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // This must match the passphrase used by PassphraseScreen to derive the key
    private val testPassphrase = "E2E_TEST_PASSPHRASE"

    @Test
    fun unlock_createCycle_logSymptom_showsCorrectlyOnCalendar() {
        // --- 1. UNLOCK THE APP ---
        // The app starts on the PassphraseScreen. We find the input field by its test tag,
        // enter the passphrase, and click the unlock button.
        composeTestRule.onNodeWithTag("passphrase-input").performTextInput(testPassphrase)
        composeTestRule.onNodeWithTag("unlock-button").performClick()

        // Wait until the main tracker screen (with the calendar) is visible.
        // `waitUntil` is crucial for making E2E tests stable.
        composeTestRule.waitUntil(timeoutMillis = 40_000) {
            composeTestRule.onAllNodesWithTag("calendar-root").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("calendar-root").assertIsDisplayed()

        // --- 2. CREATE A NEW CYCLE FOR TODAY ---
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayTag = "day-$today"

        // Click on today's date in the calendar and then click the "Save Cycle" button.
        composeTestRule.onNodeWithTag(todayTag).performClick()
        composeTestRule.onNodeWithTag("save-cycle-button").performClick()

        // Verify the UI has updated to the "in a cycle" state.
        // The "End Cycle Today" button should now be visible.
        composeTestRule.onNodeWithTag("end-cycle-button").assertIsDisplayed()
        // An invisible tag confirms this day is marked as part of a period.
        composeTestRule.onNodeWithTag("period-day-$today").assertExists()

        // --- 3. LOG A NEW SYMPTOM FOR TODAY ---
        // Click today's date again to open the log summary bottom sheet.
        composeTestRule.onNodeWithTag(todayTag).performClick()
        // Click the "Edit" button to navigate to the full DailyLogScreen.
        composeTestRule.onNodeWithTag("edit-log-button").performClick()

        // On the DailyLogScreen, type a new symptom name and click the add button.
        composeTestRule.onNodeWithTag("create-symptom-textbox").performTextInput("TestSymptom")
        composeTestRule.onNodeWithTag("create-symptom-button").performClick()

        // Verify that the chip for the new symptom has been created and is selected.
        composeTestRule.onNodeWithTag("chip-TESTSYMPTOM", useUnmergedTree = true).assertIsSelected()

        // Save the log.
        composeTestRule.onNodeWithTag("save_log_button").performClick()

        // --- 4. VERIFY THE FINAL STATE ON THE CALENDAR ---
        // We are now back on the TrackerScreen. The calendar must reflect the new symptom.
        // We wait for the UI to update and show the symptom decorator dot.
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodesWithTag("symptom-dot-$today").fetchSemanticsNodes().isNotEmpty()
        }
        // Assert that the symptom dot for today now exists and is visible.
        composeTestRule.onNodeWithTag("symptom-dot-$today").assertIsDisplayed()
    }
}