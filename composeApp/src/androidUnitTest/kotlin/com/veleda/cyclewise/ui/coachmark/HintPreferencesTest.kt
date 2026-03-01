package com.veleda.cyclewise.ui.coachmark

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [HintPreferences].
 *
 * Uses Robolectric for a real [Context] backed by DataStore.
 * Calls [HintPreferences.resetAll] in [setUp] to ensure a clean slate,
 * since the DataStore singleton persists state across test methods.
 */
@RunWith(RobolectricTestRunner::class)
class HintPreferencesTest {

    private lateinit var hintPreferences: HintPreferences

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        hintPreferences = HintPreferences(context)
        // Clear any state left over from previous test methods.
        hintPreferences.resetAll()
    }

    @Test
    fun `isHintSeen WHEN neverMarked THEN returnsFalse`() = runTest {
        // GIVEN — a clean HintPreferences with no marks

        // WHEN / THEN
        val seen = hintPreferences.isHintSeen(HintKey.DAILY_LOG_WELCOME).first()
        assertFalse(seen, "A hint that was never marked should return false")
    }

    @Test
    fun `markHintSeen THEN isHintSeenReturnsTrue`() = runTest {
        // GIVEN — mark the hint as seen
        hintPreferences.markHintSeen(HintKey.DAILY_LOG_WELCOME)

        // WHEN / THEN
        val seen = hintPreferences.isHintSeen(HintKey.DAILY_LOG_WELCOME).first()
        assertTrue(seen, "A hint that was marked as seen should return true")
    }

    @Test
    fun `resetAll WHEN hintsMarked THEN allReturnFalse`() = runTest {
        // GIVEN — mark multiple hints as seen
        hintPreferences.markHintSeen(HintKey.DAILY_LOG_WELCOME)
        hintPreferences.markHintSeen(HintKey.DAILY_LOG_EXPLORE_TABS)
        hintPreferences.markHintSeen(HintKey.DAILY_LOG_PERIOD_TAB)

        // WHEN — reset all
        hintPreferences.resetAll()

        // THEN — all hints return false
        assertFalse(
            hintPreferences.isHintSeen(HintKey.DAILY_LOG_WELCOME).first(),
            "DAILY_LOG_WELCOME should be false after resetAll"
        )
        assertFalse(
            hintPreferences.isHintSeen(HintKey.DAILY_LOG_EXPLORE_TABS).first(),
            "DAILY_LOG_EXPLORE_TABS should be false after resetAll"
        )
        assertFalse(
            hintPreferences.isHintSeen(HintKey.DAILY_LOG_PERIOD_TAB).first(),
            "DAILY_LOG_PERIOD_TAB should be false after resetAll"
        )
    }
}
