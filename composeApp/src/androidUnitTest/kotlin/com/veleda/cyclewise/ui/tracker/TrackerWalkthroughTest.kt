package com.veleda.cyclewise.ui.tracker

import com.veleda.cyclewise.ui.coachmark.HintKey
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the [TRACKER_HINTS] walkthrough definition.
 *
 * Validates the chain structure, step count, and terminal step configuration
 * without requiring any Android framework or Compose runtime.
 */
class TrackerWalkthroughTest {

    @Test
    fun `TRACKER_HINTS has 7 entries`() {
        assertEquals(7, TRACKER_HINTS.size)
    }

    @Test
    fun `chain links all 7 steps from WELCOME through TAP_DAY`() {
        val expectedChain = listOf(
            HintKey.TRACKER_WELCOME,
            HintKey.TRACKER_NAV,
            HintKey.TRACKER_PHASE_LEGEND,
            HintKey.TRACKER_LONG_PRESS,
            HintKey.TRACKER_DRAG,
            HintKey.TRACKER_ADJUST,
            HintKey.TRACKER_TAP_DAY,
        )

        val actualChain = mutableListOf<HintKey>()
        var current: HintKey? = HintKey.TRACKER_WELCOME
        while (current != null) {
            actualChain.add(current)
            current = TRACKER_HINTS[current]?.nextKey
        }

        assertEquals(expectedChain, actualChain, "Chain should walk through all 7 steps in order")
    }

    @Test
    fun `TRACKER_LONG_PRESS has requiresAction true`() {
        val def = TRACKER_HINTS[HintKey.TRACKER_LONG_PRESS]
        assertNotNull(def, "TRACKER_LONG_PRESS should exist")
        assertTrue(def.requiresAction, "TRACKER_LONG_PRESS should have requiresAction = true")
    }

    @Test
    fun `informational steps have requiresAction false`() {
        val infoKeys = listOf(
            HintKey.TRACKER_WELCOME,
            HintKey.TRACKER_NAV,
            HintKey.TRACKER_PHASE_LEGEND,
            HintKey.TRACKER_DRAG,
            HintKey.TRACKER_ADJUST,
            HintKey.TRACKER_TAP_DAY,
        )
        for (key in infoKeys) {
            val def = TRACKER_HINTS[key]
            assertNotNull(def, "$key should exist")
            assertFalse(def.requiresAction, "$key should have requiresAction = false")
        }
    }

    @Test
    fun `first step is TRACKER_WELCOME and last step nextKey is null`() {
        val first = TRACKER_HINTS[HintKey.TRACKER_WELCOME]
        assertNotNull(first, "TRACKER_WELCOME step should exist")
        assertEquals(HintKey.TRACKER_WELCOME, first.key)

        val last = TRACKER_HINTS[HintKey.TRACKER_TAP_DAY]
        assertNotNull(last, "TRACKER_TAP_DAY step should exist")
        assertNull(last.nextKey, "Last step should have null nextKey")
    }
}
