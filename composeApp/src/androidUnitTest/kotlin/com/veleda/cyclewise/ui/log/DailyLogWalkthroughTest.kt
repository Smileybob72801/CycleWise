package com.veleda.cyclewise.ui.log

import com.veleda.cyclewise.ui.coachmark.HintKey
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the [DAILY_LOG_HINTS] walkthrough definition.
 *
 * Validates the chain structure, step count, and skip button configuration
 * without requiring any Android framework or Compose runtime.
 */
class DailyLogWalkthroughTest {

    @Test
    fun `DAILY_LOG_HINTS has 10 entries`() {
        assertEquals(10, DAILY_LOG_HINTS.size)
    }

    @Test
    fun `chain links all 10 steps from WELCOME through NOTES_TAB`() {
        val expectedChain = listOf(
            HintKey.DAILY_LOG_WELCOME,
            HintKey.DAILY_LOG_MOOD,
            HintKey.DAILY_LOG_ENERGY,
            HintKey.DAILY_LOG_WATER,
            HintKey.DAILY_LOG_EXPLORE_TABS,
            HintKey.DAILY_LOG_PERIOD_TAB,
            HintKey.DAILY_LOG_PERIOD_TOGGLE,
            HintKey.DAILY_LOG_SYMPTOMS_TAB,
            HintKey.DAILY_LOG_MEDICATIONS_TAB,
            HintKey.DAILY_LOG_NOTES_TAB,
        )

        val actualChain = mutableListOf<HintKey>()
        var current: HintKey? = HintKey.DAILY_LOG_WELCOME
        while (current != null) {
            actualChain.add(current)
            current = DAILY_LOG_HINTS[current]?.nextKey
        }

        assertEquals(expectedChain, actualChain, "Chain should walk through all 10 steps in order")
    }

    @Test
    fun `first step is WELCOME and last step nextKey is null`() {
        val first = DAILY_LOG_HINTS[HintKey.DAILY_LOG_WELCOME]
        assertNotNull(first, "WELCOME step should exist")
        assertEquals(HintKey.DAILY_LOG_WELCOME, first.key)

        val last = DAILY_LOG_HINTS[HintKey.DAILY_LOG_NOTES_TAB]
        assertNotNull(last, "NOTES_TAB step should exist")
        assertNull(last.nextKey, "Last step should have null nextKey")
    }

    @Test
    fun `task steps have requiresAction true`() {
        val taskKeys = listOf(
            HintKey.DAILY_LOG_MOOD,
            HintKey.DAILY_LOG_ENERGY,
            HintKey.DAILY_LOG_WATER,
            HintKey.DAILY_LOG_PERIOD_TAB,
            HintKey.DAILY_LOG_PERIOD_TOGGLE,
            HintKey.DAILY_LOG_SYMPTOMS_TAB,
            HintKey.DAILY_LOG_MEDICATIONS_TAB,
        )
        for (key in taskKeys) {
            val def = DAILY_LOG_HINTS[key]
            assertNotNull(def, "$key should exist")
            assertTrue(def.requiresAction, "$key should have requiresAction = true")
        }
    }

    @Test
    fun `informational steps have requiresAction false`() {
        val infoKeys = listOf(
            HintKey.DAILY_LOG_WELCOME,
            HintKey.DAILY_LOG_EXPLORE_TABS,
            HintKey.DAILY_LOG_NOTES_TAB,
        )
        for (key in infoKeys) {
            val def = DAILY_LOG_HINTS[key]
            assertNotNull(def, "$key should exist")
            assertFalse(def.requiresAction, "$key should have requiresAction = false")
        }
    }

    @Test
    fun `PERIOD_TAB step has skipButtonRes and skipTargetKey targeting SYMPTOMS_TAB`() {
        val periodTab = DAILY_LOG_HINTS[HintKey.DAILY_LOG_PERIOD_TAB]
        assertNotNull(periodTab, "PERIOD_TAB step should exist")
        assertNotNull(periodTab.skipButtonRes, "PERIOD_TAB should have a skipButtonRes")
        assertEquals(
            HintKey.DAILY_LOG_SYMPTOMS_TAB,
            periodTab.skipTargetKey,
            "PERIOD_TAB skip should target SYMPTOMS_TAB"
        )
        assertNotNull(periodTab.skipToastRes, "PERIOD_TAB should have a skipToastRes")
    }
}
