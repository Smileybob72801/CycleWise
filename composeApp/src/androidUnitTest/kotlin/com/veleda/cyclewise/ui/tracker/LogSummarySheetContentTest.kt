package com.veleda.cyclewise.ui.tracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodConsistency
import com.veleda.cyclewise.domain.models.CustomTag
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.testutil.buildCustomTag
import com.veleda.cyclewise.testutil.buildCustomTagLog
import com.veleda.cyclewise.testutil.buildDailyEntry
import com.veleda.cyclewise.testutil.buildFullDailyLog
import com.veleda.cyclewise.testutil.buildMedication
import com.veleda.cyclewise.testutil.buildMedicationLog
import com.veleda.cyclewise.testutil.buildPeriodLog
import com.veleda.cyclewise.testutil.buildSymptom
import com.veleda.cyclewise.testutil.buildSymptomLog
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [LogSummarySheetContent].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class LogSummarySheetContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val headache = buildSymptom(id = "s1", name = "Headache")
    private val cramps = buildSymptom(id = "s2", name = "Cramps")
    private val ibuprofen = buildMedication(id = "m1", name = "Ibuprofen")
    private val exercise = buildCustomTag(id = "t1", name = "Exercise")

    private fun setContent(
        log: FullDailyLog = buildFullDailyLog(),
        periodId: String? = null,
        cyclePhase: CyclePhase? = null,
        symptomLibrary: List<Symptom> = listOf(headache, cramps),
        medicationLibrary: List<Medication> = listOf(ibuprofen),
        customTagLibrary: List<CustomTag> = listOf(exercise),
        waterCups: Int? = null,
        showMood: Boolean = true,
        showEnergy: Boolean = true,
        showLibido: Boolean = true,
        onEditClick: (LocalDate) -> Unit = {},
        onDeleteClick: (String) -> Unit = {},
        onViewFullLogClick: (LocalDate) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    LogSummarySheetContent(
                        log = log,
                        periodId = periodId,
                        cyclePhase = cyclePhase,
                        symptomLibrary = symptomLibrary,
                        medicationLibrary = medicationLibrary,
                        customTagLibrary = customTagLibrary,
                        waterCups = waterCups,
                        showMood = showMood,
                        showEnergy = showEnergy,
                        showLibido = showLibido,
                        onEditClick = onEditClick,
                        onDeleteClick = onDeleteClick,
                        onViewFullLogClick = onViewFullLogClick,
                    )
                }
            }
        }
    }

    // region Header

    @Test
    fun dateHeader_WHEN_rendered_THEN_isDisplayed() {
        // Given / When
        setContent()

        // Then — the date string should appear in the header
        composeTestRule.onNodeWithText("Log for", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // endregion

    // region Edit button

    @Test
    fun editButton_WHEN_rendered_THEN_isDisplayed() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithTag("edit-log-button").assertIsDisplayed()
    }

    @Test
    fun editButton_WHEN_tapped_THEN_invokesCallback() {
        // Given
        var captured: LocalDate? = null
        setContent(onEditClick = { captured = it })

        // When
        composeTestRule.onNodeWithTag("edit-log-button").performClick()

        // Then
        assert(captured != null) { "onEditClick was not invoked" }
    }

    // endregion

    // region Delete button

    @Test
    fun deleteButton_WHEN_periodIdNull_THEN_notDisplayed() {
        // Given / When
        setContent(periodId = null)

        // Then
        composeTestRule.onNodeWithTag("delete-period-button").assertDoesNotExist()
    }

    @Test
    fun deleteButton_WHEN_periodIdNonNull_THEN_isDisplayed() {
        // Given / When
        setContent(periodId = "period-1")

        // Then
        composeTestRule.onNodeWithTag("delete-period-button").assertIsDisplayed()
    }

    @Test
    fun deleteButton_WHEN_tapped_THEN_invokesCallback() {
        // Given
        var captured: String? = null
        setContent(periodId = "period-1", onDeleteClick = { captured = it })

        // When
        composeTestRule.onNodeWithTag("delete-period-button").performClick()

        // Then
        assert(captured == "period-1") { "Expected 'period-1', got '$captured'" }
    }

    // endregion

    // region Phase card

    @Test
    fun phaseCard_WHEN_cyclePhaseNull_THEN_notDisplayed() {
        // Given / When
        setContent(cyclePhase = null)

        // Then
        composeTestRule.onNodeWithText("Phase", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun phaseCard_WHEN_menstruation_THEN_displayed() {
        // Given / When
        setContent(cyclePhase = CyclePhase.MENSTRUATION)

        // Then
        composeTestRule.onNodeWithText("Phase", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // endregion

    // region Flow card

    @Test
    fun flowCard_WHEN_flowIntensityNull_THEN_notDisplayed() {
        // Given
        val log = buildFullDailyLog(periodLog = null)

        // When
        setContent(log = log)

        // Then
        composeTestRule.onNodeWithText("Flow", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun flowCard_WHEN_flowIntensitySet_THEN_displayed() {
        // Given
        val log = buildFullDailyLog(
            periodLog = buildPeriodLog(flowIntensity = FlowIntensity.HEAVY),
        )

        // When
        setContent(log = log)

        // Then
        composeTestRule.onNodeWithText("Heavy").assertIsDisplayed()
    }

    // endregion

    // region Color card

    @Test
    fun colorCard_WHEN_periodColorSet_THEN_displayed() {
        // Given
        val log = buildFullDailyLog(
            periodLog = buildPeriodLog(periodColor = PeriodColor.DARK_RED),
        )

        // When
        setContent(log = log)

        // Then
        composeTestRule.onNodeWithText("Color", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // endregion

    // region Consistency card

    @Test
    fun consistencyCard_WHEN_periodConsistencySet_THEN_displayed() {
        // Given
        val log = buildFullDailyLog(
            periodLog = buildPeriodLog(periodConsistency = PeriodConsistency.THICK),
        )

        // When
        setContent(log = log)

        // Then
        composeTestRule.onNodeWithText("Thick").assertIsDisplayed()
    }

    // endregion

    // region Mood card

    @Test
    fun moodCard_WHEN_showMoodTrueAndScoreSet_THEN_displayed() {
        // Given
        val log = buildFullDailyLog(
            entry = buildDailyEntry(moodScore = 4),
        )

        // When
        setContent(log = log, showMood = true)

        // Then
        composeTestRule.onNodeWithText("4 / 5").assertIsDisplayed()
    }

    @Test
    fun moodCard_WHEN_showMoodFalse_THEN_notDisplayed() {
        // Given
        val log = buildFullDailyLog(
            entry = buildDailyEntry(moodScore = 4),
        )

        // When
        setContent(log = log, showMood = false)

        // Then
        composeTestRule.onNodeWithText("Mood", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    // endregion

    // region Energy card

    @Test
    fun energyCard_WHEN_showEnergyTrueAndLevelSet_THEN_displayed() {
        // Given
        val log = buildFullDailyLog(
            entry = buildDailyEntry(energyLevel = 3),
        )

        // When
        setContent(log = log, showEnergy = true)

        // Then
        composeTestRule.onNodeWithText("3 / 5").assertIsDisplayed()
    }

    @Test
    fun energyCard_WHEN_showEnergyFalse_THEN_notDisplayed() {
        // Given
        val log = buildFullDailyLog(
            entry = buildDailyEntry(energyLevel = 3),
        )

        // When
        setContent(log = log, showEnergy = false)

        // Then
        composeTestRule.onNodeWithText("Energy", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    // endregion

    // region Libido card

    @Test
    fun libidoCard_WHEN_showLibidoTrueAndScoreSet_THEN_displayed() {
        // Given
        val log = buildFullDailyLog(
            entry = buildDailyEntry(libidoScore = 2),
        )

        // When
        setContent(log = log, showLibido = true)

        // Then
        composeTestRule.onNodeWithText("2 / 5").assertIsDisplayed()
    }

    @Test
    fun libidoCard_WHEN_showLibidoFalse_THEN_notDisplayed() {
        // Given
        val log = buildFullDailyLog(
            entry = buildDailyEntry(libidoScore = 2),
        )

        // When
        setContent(log = log, showLibido = false)

        // Then
        composeTestRule.onNodeWithText("Libido", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    // endregion

    // region Water card

    @Test
    fun waterCard_WHEN_cupsGreaterThanZero_THEN_displayed() {
        // Given / When
        setContent(waterCups = 5)

        // Then
        composeTestRule.onNodeWithText("Water", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun waterCard_WHEN_cupsIsZero_THEN_notDisplayed() {
        // Given / When
        setContent(waterCups = 0)

        // Then
        composeTestRule.onNodeWithText("Water", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun waterCard_WHEN_cupsNull_THEN_notDisplayed() {
        // Given / When
        setContent(waterCups = null)

        // Then
        composeTestRule.onNodeWithText("Water", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    // endregion

    // region Notes card

    @Test
    fun notesCard_WHEN_notePresent_THEN_displayed() {
        // Given
        val log = buildFullDailyLog(
            entry = buildDailyEntry(note = "Feeling good"),
        )

        // When
        setContent(log = log)

        // Then
        composeTestRule.onNodeWithText("Feeling good").assertIsDisplayed()
    }

    @Test
    fun notesCard_WHEN_noteNull_THEN_notDisplayed() {
        // Given
        val log = buildFullDailyLog(
            entry = buildDailyEntry(note = null),
        )

        // When
        setContent(log = log)

        // Then
        composeTestRule.onNodeWithText("Notes", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    // endregion

    // region Symptom chips

    @Test
    fun symptomChips_WHEN_symptomsLogged_THEN_displayed() {
        // Given
        val log = buildFullDailyLog(
            symptomLogs = listOf(buildSymptomLog(symptomId = "s1")),
        )

        // When
        setContent(log = log)

        // Then
        composeTestRule.onNodeWithText("Headache").assertIsDisplayed()
    }

    // endregion

    // region Medication chips

    @Test
    fun medicationChips_WHEN_medicationsLogged_THEN_displayed() {
        // Given
        val log = buildFullDailyLog(
            medicationLogs = listOf(buildMedicationLog(medicationId = "m1")),
        )

        // When
        setContent(log = log)

        // Then
        composeTestRule.onNodeWithText("Ibuprofen").assertIsDisplayed()
    }

    // endregion

    // region Custom tag chips

    @Test
    fun customTagChips_WHEN_customTagsLogged_THEN_displayed() {
        // Given
        val log = buildFullDailyLog(
            customTagLogs = listOf(buildCustomTagLog(tagId = "t1")),
        )

        // When
        setContent(log = log)

        // Then
        composeTestRule.onNodeWithText("Exercise").assertIsDisplayed()
    }

    // endregion

    // region View Full Log button

    @Test
    fun viewFullLogButton_WHEN_tapped_THEN_invokesCallback() {
        // Given
        var captured: LocalDate? = null
        setContent(onViewFullLogClick = { captured = it })

        // When
        composeTestRule.onNodeWithText("View Full Log", substring = true, ignoreCase = true)
            .performClick()

        // Then
        assert(captured != null) { "onViewFullLogClick was not invoked" }
    }

    // endregion
}
