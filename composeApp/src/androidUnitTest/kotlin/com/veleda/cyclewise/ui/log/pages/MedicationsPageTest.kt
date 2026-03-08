package com.veleda.cyclewise.ui.log.pages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.testutil.buildMedication
import com.veleda.cyclewise.testutil.buildMedicationLog
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [MedicationsPage].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class MedicationsPageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val ibuprofen = buildMedication(id = "m1", name = "Ibuprofen")
    private val acetaminophen = buildMedication(id = "m2", name = "Acetaminophen")

    private val library = listOf(ibuprofen, acetaminophen)

    private fun setContent(
        loggedMedications: List<MedicationLog> = emptyList(),
        medicationLibrary: List<Medication> = library,
        onToggleMedication: (Medication) -> Unit = {},
        onCreateAndAddMedication: (String) -> Unit = {},
        onShowEducationalSheet: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    MedicationsPage(
                        loggedMedications = loggedMedications,
                        medicationLibrary = medicationLibrary,
                        onToggleMedication = onToggleMedication,
                        onCreateAndAddMedication = onCreateAndAddMedication,
                        onShowEducationalSheet = onShowEducationalSheet,
                    )
                }
            }
        }
    }

    // region Count text

    @Test
    fun countText_WHEN_noLoggedMedications_THEN_notDisplayed() {
        // Given / When
        setContent(loggedMedications = emptyList())

        // Then — the count text "X medications logged" should not appear
        composeTestRule.onNodeWithText("medications logged", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun countText_WHEN_medicationsLogged_THEN_displayed() {
        // Given
        val logged = listOf(buildMedicationLog(medicationId = "m1"))

        // When
        setContent(loggedMedications = logged)

        // Then
        composeTestRule.onNodeWithText("1", substring = true).assertIsDisplayed()
    }

    // endregion

    // region Chip rendering

    @Test
    fun medicationChips_WHEN_libraryProvided_THEN_allChipsRendered() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithText("Ibuprofen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Acetaminophen").assertIsDisplayed()
    }

    @Test
    fun medicationChip_WHEN_emptyLibrary_THEN_noChipsRendered() {
        // Given / When
        setContent(medicationLibrary = emptyList())

        // Then
        composeTestRule.onNodeWithText("Ibuprofen").assertDoesNotExist()
        composeTestRule.onNodeWithText("Acetaminophen").assertDoesNotExist()
    }

    // endregion

    // region Chip toggle callback

    @Test
    fun medicationChip_WHEN_tapped_THEN_invokesToggleCallback() {
        // Given
        var captured: Medication? = null
        setContent(onToggleMedication = { captured = it })

        // When
        composeTestRule.onNodeWithText("Ibuprofen").performClick()

        // Then
        assert(captured == ibuprofen) { "Expected Ibuprofen medication, got $captured" }
    }

    // endregion

    // region Create medication

    @Test
    fun createMedicationField_WHEN_rendered_THEN_isDisplayed() {
        // Given / When
        setContent()

        // Then — the text field label should be visible
        composeTestRule.onNodeWithText("Add", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun createMedicationButton_WHEN_fieldHasText_THEN_tappingInvokesCallback() {
        // Given
        var captured: String? = null
        setContent(onCreateAndAddMedication = { captured = it })

        // When — type into the text field and tap the add button
        composeTestRule.onNodeWithText("Add", substring = true, ignoreCase = true)
            .performTextInput("Aspirin")

        // Find and click the add icon button
        composeTestRule.onAllNodes(
            androidx.compose.ui.test.hasContentDescription("Create", substring = true, ignoreCase = true),
            useUnmergedTree = true,
        )[0].performClick()

        // Then
        assert(captured == "Aspirin") { "Expected 'Aspirin', got '$captured'" }
    }

    // endregion

    // region Info button

    @Test
    fun infoButton_WHEN_tapped_THEN_invokesEducationalSheet() {
        // Given
        var capturedTag: String? = null
        setContent(onShowEducationalSheet = { capturedTag = it })

        // When — SectionCard info CD is "Learn more about Medications"
        composeTestRule.onAllNodes(
            androidx.compose.ui.test.hasContentDescription("Learn more about Medications"),
            useUnmergedTree = true,
        )[0].performClick()

        // Then
        assert(capturedTag == "Medication") { "Expected 'Medication', got '$capturedTag'" }
    }

    // endregion
}
