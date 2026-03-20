package com.veleda.cyclewise.ui.log.pages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildSymptom
import com.veleda.cyclewise.testutil.buildSymptomLog
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [SymptomsPage].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class SymptomsPageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val headache = buildSymptom(id = "s1", name = "Headache")
    private val cramps = buildSymptom(id = "s2", name = "Cramps")
    private val fatigue = buildSymptom(id = "s3", name = "Fatigue")

    private val library = listOf(headache, cramps, fatigue)

    private fun setContent(
        loggedSymptoms: List<SymptomLog> = emptyList(),
        symptomLibrary: List<Symptom> = library,
        onToggleSymptom: (Symptom) -> Unit = {},
        onCreateAndAddSymptom: (String) -> Unit = {},
        onShowEducationalSheet: (String) -> Unit = {},
        symptomForContextMenu: Symptom? = null,
        symptomRenaming: Symptom? = null,
        symptomToDelete: Symptom? = null,
        symptomDeleteLogCount: Int = 0,
        renameError: String? = null,
        onSymptomLongPressed: (Symptom) -> Unit = {},
        onRenameClicked: (Symptom) -> Unit = {},
        onRenameConfirmed: (String, String) -> Unit = { _, _ -> },
        onDeleteClicked: (Symptom) -> Unit = {},
        onDeleteConfirmed: (String) -> Unit = {},
        onEditDismissed: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    SymptomsPage(
                        loggedSymptoms = loggedSymptoms,
                        symptomLibrary = symptomLibrary,
                        onToggleSymptom = onToggleSymptom,
                        onCreateAndAddSymptom = onCreateAndAddSymptom,
                        onShowEducationalSheet = onShowEducationalSheet,
                        symptomForContextMenu = symptomForContextMenu,
                        symptomRenaming = symptomRenaming,
                        symptomToDelete = symptomToDelete,
                        symptomDeleteLogCount = symptomDeleteLogCount,
                        renameError = renameError,
                        onSymptomLongPressed = onSymptomLongPressed,
                        onRenameClicked = onRenameClicked,
                        onRenameConfirmed = onRenameConfirmed,
                        onDeleteClicked = onDeleteClicked,
                        onDeleteConfirmed = onDeleteConfirmed,
                        onEditDismissed = onEditDismissed,
                    )
                }
            }
        }
    }

    // region Count text

    @Test
    fun countText_WHEN_noLoggedSymptoms_THEN_notDisplayed() {
        // Given / When
        setContent(loggedSymptoms = emptyList())

        // Then — the count text "X symptoms logged" should not appear
        composeTestRule.onNodeWithText("symptoms logged", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun countText_WHEN_symptomsLogged_THEN_displayed() {
        // Given
        val logged = listOf(buildSymptomLog(symptomId = "s1"))

        // When
        setContent(loggedSymptoms = logged)

        // Then
        composeTestRule.onNodeWithText("1", substring = true).assertIsDisplayed()
    }

    // endregion

    // region Chip rendering

    @Test
    fun symptomChips_WHEN_libraryProvided_THEN_allChipsRendered() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithTag("chip-HEADACHE").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chip-CRAMPS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chip-FATIGUE").assertIsDisplayed()
    }

    @Test
    fun symptomChip_WHEN_logged_THEN_isSelected() {
        // Given
        val logged = listOf(buildSymptomLog(symptomId = "s1"))

        // When
        setContent(loggedSymptoms = logged)

        // Then
        composeTestRule.onNodeWithTag("chip-HEADACHE").assertIsSelected()
    }

    @Test
    fun symptomChip_WHEN_notLogged_THEN_isNotSelected() {
        // Given / When
        setContent(loggedSymptoms = emptyList())

        // Then
        composeTestRule.onNodeWithTag("chip-HEADACHE").assertIsNotSelected()
    }

    // endregion

    // region Chip toggle callback

    @Test
    fun symptomChip_WHEN_tapped_THEN_invokesToggleCallback() {
        // Given
        var captured: Symptom? = null
        setContent(onToggleSymptom = { captured = it })

        // When
        composeTestRule.onNodeWithTag("chip-HEADACHE").performClick()

        // Then
        assert(captured == headache) { "Expected Headache symptom, got $captured" }
    }

    // endregion

    // region Create symptom

    @Test
    fun createSymptomField_WHEN_rendered_THEN_hasTestTag() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithTag("create-symptom-textbox").assertIsDisplayed()
    }

    @Test
    fun createSymptomButton_WHEN_fieldBlank_THEN_isDisabled() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithTag("create-symptom-button").assertIsNotEnabled()
    }

    @Test
    fun createSymptomButton_WHEN_fieldHasText_THEN_isEnabled() {
        // Given
        setContent()

        // When
        composeTestRule.onNodeWithTag("create-symptom-textbox").performTextInput("Nausea")

        // Then
        composeTestRule.onNodeWithTag("create-symptom-button").assertIsEnabled()
    }

    @Test
    fun createSymptomButton_WHEN_tapped_THEN_invokesCallback() {
        // Given
        var captured: String? = null
        setContent(onCreateAndAddSymptom = { captured = it })
        composeTestRule.onNodeWithTag("create-symptom-textbox").performTextInput("Nausea")

        // When
        composeTestRule.onNodeWithTag("create-symptom-button").performClick()

        // Then
        assert(captured == "Nausea") { "Expected 'Nausea', got '$captured'" }
    }

    // endregion

    // region Long-press / Context menu

    @Test
    fun symptomChip_WHEN_longPressed_THEN_invokesLongPressCallback() {
        // Given
        var captured: Symptom? = null
        setContent(onSymptomLongPressed = { captured = it })

        // When
        composeTestRule.onNodeWithTag("chip-HEADACHE").performTouchInput { longClick() }

        // Then
        assert(captured == headache) { "Expected Headache symptom on long-press, got $captured" }
    }

    @Test
    fun contextMenu_WHEN_renameClicked_THEN_invokesRenameCallback() {
        // Given
        var captured: Symptom? = null
        setContent(
            symptomForContextMenu = headache,
            onRenameClicked = { captured = it },
        )

        // When — tap Rename in the context menu
        composeTestRule.onNodeWithText("Rename").performClick()

        // Then
        assert(captured == headache) { "Expected Headache symptom from Rename click, got $captured" }
    }

    @Test
    fun contextMenu_WHEN_symptomForContextMenuSet_THEN_renameAndDeleteVisible() {
        // Given / When
        setContent(symptomForContextMenu = headache)

        // Then
        composeTestRule.onNodeWithText("Rename").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    // endregion

    // region Rename dialog

    @Test
    fun renameDialog_WHEN_symptomRenamingSet_THEN_dialogVisible() {
        // Given / When
        setContent(symptomRenaming = headache)

        // Then — title is unique; "Headache" appears both as chip label and dialog text field
        composeTestRule.onNodeWithText("Rename Symptom").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Headache").assertCountEquals(2)
    }

    // endregion

    // region Help button

    @Test
    fun helpButton_WHEN_rendered_THEN_isDisplayed() {
        // Given / When
        setContent()

        // Then — SectionCard help CD is "Usage help for Symptoms"
        composeTestRule.onAllNodes(
            hasContentDescription("Usage help for Symptoms"),
            useUnmergedTree = true,
        )[0].assertIsDisplayed()
    }

    // endregion

    // region Delete dialog

    @Test
    fun deleteDialog_WHEN_symptomToDeleteWithLogs_THEN_showsLogCountWarning() {
        // Given / When
        setContent(symptomToDelete = headache, symptomDeleteLogCount = 5)

        // Then
        composeTestRule.onNodeWithText("Delete Symptom").assertIsDisplayed()
        composeTestRule.onNodeWithText("5", substring = true).assertIsDisplayed()
    }

    @Test
    fun deleteDialog_WHEN_symptomToDeleteWithNoLogs_THEN_showsNoLogsMessage() {
        // Given / When
        setContent(symptomToDelete = headache, symptomDeleteLogCount = 0)

        // Then
        composeTestRule.onNodeWithText("Delete Symptom").assertIsDisplayed()
        composeTestRule.onNodeWithText("no logged entries", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // endregion
}
