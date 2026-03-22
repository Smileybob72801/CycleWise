package com.veleda.cyclewise.ui.log.pages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.veleda.cyclewise.RobolectricTestApp
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

    private fun setContent(
        tags: List<String> = emptyList(),
        note: String = "",
        onAddTag: (String) -> Unit = {},
        onRemoveTag: (String) -> Unit = {},
        onNoteChanged: (String) -> Unit = {},
        onDone: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    NotesTagsPage(
                        tags = tags,
                        note = note,
                        onAddTag = onAddTag,
                        onRemoveTag = onRemoveTag,
                        onNoteChanged = onNoteChanged,
                        onDone = onDone,
                    )
                }
            }
        }
    }

    // region Tags section

    @Test
    fun tagsSection_WHEN_rendered_THEN_titleIsDisplayed() {
        // Given / When
        setContent()

        // Then
        composeTestRule.onNodeWithText("Tags", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun tagChips_WHEN_tagsProvided_THEN_allRendered() {
        // Given / When
        setContent(tags = listOf("Exercise", "Stress", "Travel"))

        // Then
        composeTestRule.onNodeWithText("Exercise").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stress").assertIsDisplayed()
        composeTestRule.onNodeWithText("Travel").assertIsDisplayed()
    }

    @Test
    fun tagChip_WHEN_closeIconTapped_THEN_invokesRemoveCallback() {
        // Given
        var removed: String? = null
        setContent(tags = listOf("Exercise"), onRemoveTag = { removed = it })

        // When — tap the close icon on the "Exercise" chip
        composeTestRule.onAllNodes(
            androidx.compose.ui.test.hasContentDescription("Exercise", substring = true),
            useUnmergedTree = true,
        )[0].performClick()

        // Then
        assert(removed == "Exercise") { "Expected 'Exercise', got '$removed'" }
    }

    @Test
    fun addTagButton_WHEN_tappedWithText_THEN_invokesCallback() {
        // Given
        var captured: String? = null
        setContent(onAddTag = { captured = it })

        // When — type into the tag text field (label: "Add a custom tag…")
        composeTestRule.onNodeWithText("Add a custom tag", substring = true)
            .performTextInput("Workout")
        // Tap the add button (CD: "Add Tag")
        composeTestRule.onAllNodes(
            androidx.compose.ui.test.hasContentDescription("Add Tag"),
            useUnmergedTree = true,
        )[0].performClick()

        // Then
        assert(captured == "Workout") { "Expected 'Workout', got '$captured'" }
    }

    @Test
    fun noTags_WHEN_empty_THEN_noChipsRendered() {
        // Given / When
        setContent(tags = emptyList())

        // Then — only the text field label "Add" should exist, not any tag chips
        composeTestRule.onNodeWithText("Exercise").assertDoesNotExist()
    }

    // endregion

    // region Help button

    @Test
    fun helpButton_WHEN_rendered_THEN_isDisplayedOnCustomTagsCard() {
        // Given / When
        setContent()

        // Then — SectionCard help CD is "Usage help for Custom Tags"
        composeTestRule.onAllNodes(
            hasContentDescription("Usage help for Custom Tags"),
            useUnmergedTree = true,
        )[0].assertIsDisplayed()
    }

    // endregion

    // region Notes section

    @Test
    fun notesSection_WHEN_rendered_THEN_titleIsDisplayed() {
        // Given / When
        setContent()

        // Then — exact section title from R.string.daily_log_notes_title
        composeTestRule.onNodeWithText("Notes").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun noteEditor_WHEN_existingText_THEN_displaysText() {
        // Given / When
        setContent(note = "Feeling good today")

        // Then
        composeTestRule.onNodeWithText("Feeling good today").assertIsDisplayed()
    }

    @Test
    fun noteEditor_WHEN_textEntered_THEN_invokesCallback() {
        // Given
        var captured: String? = null
        setContent(onNoteChanged = { captured = it })

        // When — type into the note editor (label: "Add any notes…")
        composeTestRule.onNodeWithText("Add any notes", substring = true)
            .performScrollTo()
            .performTextInput("New note")

        // Then
        assert(captured == "New note") { "Expected 'New note', got '$captured'" }
    }

    @Test
    fun characterCount_WHEN_rendered_THEN_displaysCount() {
        // Given / When
        setContent(note = "Hello")

        // Then — character count should show "5"
        composeTestRule.onNodeWithText("5", substring = true).assertIsDisplayed()
    }

    @Test
    fun characterCount_WHEN_emptyNote_THEN_displaysZero() {
        // Given / When
        setContent(note = "")

        // Then
        composeTestRule.onNodeWithText("0", substring = true).assertIsDisplayed()
    }

    @Test
    fun noteEditor_WHEN_noteIsEmpty_THEN_placeholderIsDisplayed() {
        // Given / When
        setContent(note = "")

        // Then — label text from R.string.daily_log_add_notes is visible when empty
        composeTestRule.onNodeWithText("Add any notes", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
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
