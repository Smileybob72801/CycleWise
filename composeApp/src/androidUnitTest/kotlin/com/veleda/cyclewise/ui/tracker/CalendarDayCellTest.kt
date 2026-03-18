package com.veleda.cyclewise.ui.tracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LightCyclePhasePalette
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Robolectric-based integration tests for [CalendarDayCell].
 *
 * Validates that the cell renders without crash under the key heatmap border
 * configurations: isolated, start, end, middle, combined with phase fill,
 * and combined with today border.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class CalendarDayCellTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDate = LocalDate.of(2026, 3, 15)
    private val palette = LightCyclePhasePalette
    private val testHeatmapColor = Color(0xFF2196F3).copy(alpha = 0.6f)

    private fun setContent(
        dayInfo: CalendarDayInfo? = null,
        isToday: Boolean = false,
        isStartDate: Boolean = false,
        isEndDate: Boolean = false,
        isInExistingRange: Boolean = false,
        isInSelectionRange: Boolean = false,
        displayPhase: CyclePhase? = null,
        isPhaseStart: Boolean = true,
        isPhaseEnd: Boolean = true,
        heatmapColor: Color? = null,
        isHeatmapStart: Boolean = true,
        isHeatmapEnd: Boolean = true,
        phaseBorderColor: Color? = null,
        isHeatmapModeActive: Boolean = false,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDimensions provides Dimensions(),
            ) {
                MaterialTheme {
                    CalendarDayCell(
                        day = CalendarDay(testDate, DayPosition.MonthDate),
                        dayInfo = dayInfo,
                        isToday = isToday,
                        isStartDate = isStartDate,
                        isEndDate = isEndDate,
                        isInExistingRange = isInExistingRange,
                        isInSelectionRange = isInSelectionRange,
                        isPhaseStart = isPhaseStart,
                        isPhaseEnd = isPhaseEnd,
                        palette = palette,
                        displayPhase = displayPhase,
                        onTap = {},
                        heatmapColor = heatmapColor,
                        isHeatmapStart = isHeatmapStart,
                        isHeatmapEnd = isHeatmapEnd,
                        phaseBorderColor = phaseBorderColor,
                        isHeatmapModeActive = isHeatmapModeActive,
                    )
                }
            }
        }
    }

    @Test
    fun `GIVEN no heatmap THEN renders day number normally`() {
        // GIVEN — baseline cell with no heatmap overlay
        setContent()

        // THEN — the day number is displayed
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
    }

    @Test
    fun `GIVEN isolated heatmap day THEN renders without crash`() {
        // GIVEN — a single heatmap day (both start and end)
        setContent(
            heatmapColor = testHeatmapColor,
            isHeatmapStart = true,
            isHeatmapEnd = true,
        )

        // THEN — the day number is displayed and cell is found by tag
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
        composeTestRule.onNodeWithTag("day-2026-03-15").assertIsDisplayed()
    }

    @Test
    fun `GIVEN heatmap with displayPhase THEN renders both layers`() {
        // GIVEN — a cell with both phase fill and heatmap border
        setContent(
            displayPhase = CyclePhase.FOLLICULAR,
            isPhaseStart = true,
            isPhaseEnd = false,
            heatmapColor = testHeatmapColor,
            isHeatmapStart = true,
            isHeatmapEnd = false,
        )

        // THEN — the day renders without crash
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
    }

    @Test
    fun `GIVEN today with heatmap THEN renders heatmap border instead of today border`() {
        // GIVEN — today cell with heatmap active (heatmap border takes priority)
        setContent(
            isToday = true,
            heatmapColor = testHeatmapColor,
            isHeatmapStart = true,
            isHeatmapEnd = true,
        )

        // THEN — renders without crash
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
    }

    @Test
    fun `GIVEN middle of heatmap band THEN renders without crash`() {
        // GIVEN — middle cell (not start, not end) — only top/bottom lines visible
        setContent(
            heatmapColor = testHeatmapColor,
            isHeatmapStart = false,
            isHeatmapEnd = false,
        )

        // THEN — renders without crash
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
    }

    @Test
    fun `GIVEN phaseBorderColor and no heatmap fill THEN renders without crash`() {
        // GIVEN — phase border only, no heatmap fill (edge case: no data for this day)
        setContent(
            phaseBorderColor = Color(0xFF80CBC4),
            isPhaseStart = true,
            isPhaseEnd = false,
        )

        // THEN — renders without crash
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
        composeTestRule.onNodeWithTag("day-2026-03-15").assertIsDisplayed()
    }

    @Test
    fun `GIVEN heatmap fill and phaseBorderColor THEN renders both layers`() {
        // GIVEN — the main use case: heatmap fill + phase border
        setContent(
            heatmapColor = testHeatmapColor,
            isHeatmapStart = true,
            isHeatmapEnd = false,
            phaseBorderColor = Color(0xFF80CBC4),
            isPhaseStart = true,
            isPhaseEnd = false,
        )

        // THEN — renders without crash with both layers
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
    }

    @Test
    fun `GIVEN today and phaseBorderColor THEN renders without crash`() {
        // GIVEN — today cell with phase border active (today ring suppressed)
        setContent(
            isToday = true,
            phaseBorderColor = Color(0xFF80CBC4),
            isPhaseStart = true,
            isPhaseEnd = true,
        )

        // THEN — renders without crash
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
    }

    @Test
    fun `GIVEN period day with heatmap active THEN uses heatmap fill with menstruation border`() {
        // GIVEN — period day with heatmap data and heatmap mode active:
        // heatmap fill takes over, menstruation shows as border only
        setContent(
            dayInfo = CalendarDayInfo(
                isPeriodDay = true,
                hasSymptoms = false,
                hasMedications = false,
                hasNotes = false,
                cyclePhase = CyclePhase.MENSTRUATION,
            ),
            isStartDate = true,
            isEndDate = true,
            isInExistingRange = true,
            heatmapColor = testHeatmapColor,
            isHeatmapStart = true,
            isHeatmapEnd = true,
            phaseBorderColor = Color(0xFFEF9A9A),
            isHeatmapModeActive = true,
        )

        // THEN — period day tag is still present and renders correctly
        composeTestRule.onNodeWithTag("period-day-2026-03-15", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `GIVEN period day in heatmap mode without heatmap data THEN renders border only`() {
        // GIVEN — period day with no heatmap data but heatmap mode active:
        // menstruation border only, no fill
        setContent(
            dayInfo = CalendarDayInfo(
                isPeriodDay = true,
                hasSymptoms = false,
                hasMedications = false,
                hasNotes = false,
                cyclePhase = CyclePhase.MENSTRUATION,
            ),
            isStartDate = true,
            isEndDate = true,
            isInExistingRange = true,
            heatmapColor = null,
            phaseBorderColor = palette.menstruation.fill,
            isHeatmapModeActive = true,
        )

        // THEN — renders without crash and period day tag is present
        composeTestRule.onNodeWithTag("period-day-2026-03-15", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
    }

    // ── Indicator presence / absence tests ───────────────────────────────

    @Test
    fun `GIVEN day with symptoms THEN symptom indicator exists`() {
        // GIVEN — a day with symptoms logged
        setContent(dayInfo = CalendarDayInfo(hasSymptoms = true))

        // THEN — the symptom indicator tag is present
        composeTestRule.onNodeWithTag("symptom-indicator-2026-03-15", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `GIVEN day with medications THEN medication indicator exists`() {
        // GIVEN — a day with medications logged
        setContent(dayInfo = CalendarDayInfo(hasMedications = true))

        // THEN — the medication indicator tag is present
        composeTestRule.onNodeWithTag("medication-indicator-2026-03-15", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `GIVEN day with notes THEN notes indicator exists`() {
        // GIVEN — a day with notes logged
        setContent(dayInfo = CalendarDayInfo(hasNotes = true))

        // THEN — the notes indicator tag is present
        composeTestRule.onNodeWithTag("notes-indicator-2026-03-15", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `GIVEN day with all indicators THEN all three indicator tags exist`() {
        // GIVEN — a day with symptoms, medications, and notes all logged
        setContent(
            dayInfo = CalendarDayInfo(
                hasSymptoms = true,
                hasMedications = true,
                hasNotes = true,
            )
        )

        // THEN — all three indicator tags are present
        composeTestRule.onNodeWithTag("symptom-indicator-2026-03-15", useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag("medication-indicator-2026-03-15", useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag("notes-indicator-2026-03-15", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `GIVEN day with no indicators THEN no indicator tags exist`() {
        // GIVEN — a day with no symptoms, medications, or notes
        setContent(dayInfo = CalendarDayInfo())

        // THEN — none of the indicator tags are present
        composeTestRule.onNodeWithTag("symptom-indicator-2026-03-15", useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag("medication-indicator-2026-03-15", useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag("notes-indicator-2026-03-15", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `GIVEN day with only medications THEN only medication indicator exists`() {
        // GIVEN — a day with only medications logged (no symptoms or notes)
        setContent(dayInfo = CalendarDayInfo(hasMedications = true))

        // THEN — only the medication indicator is present
        composeTestRule.onNodeWithTag("medication-indicator-2026-03-15", useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag("symptom-indicator-2026-03-15", useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag("notes-indicator-2026-03-15", useUnmergedTree = true)
            .assertDoesNotExist()
    }
}
