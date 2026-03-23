package com.veleda.cyclewise.ui.log.pages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.domain.models.CustomTag
import com.veleda.cyclewise.domain.models.CustomTagLog
import com.veleda.cyclewise.testutil.buildCustomTag
import com.veleda.cyclewise.testutil.buildCustomTagLog
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for [NotesTagsPage].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class NotesTagsPageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val library = listOf(
        buildCustomTag(id = "t1", name = "Exercise"),
        buildCustomTag(id = "t2", name = "Stress"),
        buildCustomTag(id = "t3", name = "Travel"),
    )

    private fun setContent(
        loggedCustomTags: List<CustomTagLog> = emptyList(),
        customTagLibrary: List<CustomTag> = library,
        onToggleCustomTag: (CustomTag) -> Unit = {},
        onCreateAndAddCustomTag: (String) -> Unit = {},
        note: String = "",
        onNoteChanged: (String) -> Unit = {},
        onDone: () -> Unit = {},
        customTagForContextMenu: CustomTag? = null,
        customTagRenaming: CustomTag? = null,
        customTagToDelete: CustomTag? = null,
        customTagDeleteLogCount: Int = 0,
        renameError: String? = null,
        onCustomTagLongPressed: (CustomTag) -> Unit = {},
        onRenameClicked: (CustomTag) -> Unit = {},
        onRenameConfirmed: (String, String) -> Unit = { _, _ -> },
        onDeleteClicked: (CustomTag) -> Unit = {},
        onDeleteConfirmed: (String) -> Unit = {},
        onEditDismissed: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    NotesTagsPage(
                        loggedCustomTags = loggedCustomTags,
                        customTagLibrary = customTagLibrary,
                        onToggleCustomTag = onToggleCustomTag,
                        onCreateAndAddCustomTag = onCreateAndAddCustomTag,
                        note = note,
                        onNoteChanged = onNoteChanged,
                        onDone = onDone,
                        customTagForContextMenu = customTagForContextMenu,
                        customTagRenaming = customTagRenaming,
                        customTagToDelete = customTagToDelete,
                        customTagDeleteLogCount = customTagDeleteLogCount,
                        renameError = renameError,
                        onCustomTagLongPressed = onCustomTagLongPressed,
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

    // region Tags section

    @Test
    fun tagsSection_WHEN_rendered_THEN_titleIsDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Tags", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun tagChips_WHEN_libraryProvided_THEN_allRendered() {
        setContent()
        composeTestRule.onNodeWithText("Exercise").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stress").assertIsDisplayed()
        composeTestRule.onNodeWithText("Travel").assertIsDisplayed()
    }

    @Test
    fun tagChip_WHEN_logged_THEN_isSelected() {
        val logged = listOf(buildCustomTagLog(tagId = "t1"))
        setContent(loggedCustomTags = logged)
        composeTestRule.onNodeWithTag("chip-EXERCISE").assertIsSelected()
    }

    @Test
    fun tagChip_WHEN_notLogged_THEN_isNotSelected() {
        setContent(loggedCustomTags = emptyList())
        composeTestRule.onNodeWithTag("chip-EXERCISE").assertIsNotSelected()
    }

    @Test
    fun tagChip_WHEN_tapped_THEN_invokesToggleCallback() {
        var toggled: CustomTag? = null
        setContent(onToggleCustomTag = { toggled = it })
        composeTestRule.onNodeWithTag("chip-EXERCISE").performClick()
        assert(toggled?.id == "t1") { "Expected tag t1, got ${toggled?.id}" }
    }

    @Test
    fun createTag_WHEN_textEnteredAndButtonTapped_THEN_invokesCallback() {
        var captured: String? = null
        setContent(onCreateAndAddCustomTag = { captured = it })
        composeTestRule.onNodeWithText("Add a custom tag", substring = true)
            .performTextInput("Workout")
        composeTestRule.onNodeWithTag("create-custom-tag-button").performClick()
        assert(captured == "Workout") { "Expected 'Workout', got '$captured'" }
    }

    @Test
    fun noLibrary_WHEN_emptyLibrary_THEN_noChipsRendered() {
        setContent(customTagLibrary = emptyList())
        composeTestRule.onNodeWithText("Exercise").assertDoesNotExist()
    }

    // endregion

    // region Help button

    @Test
    fun helpButton_WHEN_rendered_THEN_isDisplayedOnCustomTagsCard() {
        setContent()
        composeTestRule.onAllNodes(
            hasContentDescription("Usage help for Custom Tags"),
            useUnmergedTree = true,
        )[0].assertIsDisplayed()
    }

    // endregion

    // region Notes section

    @Test
    fun notesSection_WHEN_rendered_THEN_titleIsDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Notes").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun noteEditor_WHEN_existingText_THEN_displaysText() {
        setContent(note = "Feeling good today")
        composeTestRule.onNodeWithText("Feeling good today").assertIsDisplayed()
    }

    @Test
    fun noteEditor_WHEN_textEntered_THEN_invokesCallback() {
        var captured: String? = null
        setContent(onNoteChanged = { captured = it })
        composeTestRule.onNodeWithText("Add any notes", substring = true)
            .performScrollTo()
            .performTextInput("New note")
        assert(captured == "New note") { "Expected 'New note', got '$captured'" }
    }

    @Test
    fun characterCount_WHEN_rendered_THEN_displaysCount() {
        setContent(note = "Hello")
        composeTestRule.onNodeWithText("5", substring = true).assertIsDisplayed()
    }

    @Test
    fun characterCount_WHEN_emptyNote_THEN_displaysZero() {
        setContent(note = "")
        composeTestRule.onNodeWithText("0", substring = true).assertIsDisplayed()
    }

    @Test
    fun noteEditor_WHEN_noteIsEmpty_THEN_placeholderIsDisplayed() {
        setContent(note = "")
        composeTestRule.onNodeWithText("Add any notes", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    // endregion

    // region Done button

    @Test
    fun doneButton_WHEN_tapped_THEN_invokesCallback() {
        var invoked = false
        setContent(onDone = { invoked = true })
        composeTestRule.onNodeWithText("Done")
            .performScrollTo()
            .performClick()
        assert(invoked) { "onDone was not invoked" }
    }

    // endregion
}
