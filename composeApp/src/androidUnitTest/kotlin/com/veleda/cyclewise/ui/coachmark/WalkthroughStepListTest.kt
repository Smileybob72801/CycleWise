package com.veleda.cyclewise.ui.coachmark

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [walkthroughStepList].
 *
 * Validates chain traversal, single-step chains, and missing start keys
 * without requiring any Android framework or Compose runtime.
 */
class WalkthroughStepListTest {

    private val chainDefs: Map<HintKey, CoachMarkDef> = mapOf(
        HintKey.DAILY_LOG_WELCOME to CoachMarkDef(
            key = HintKey.DAILY_LOG_WELCOME,
            titleRes = 0,
            bodyRes = 0,
            nextKey = HintKey.DAILY_LOG_MOOD,
            dismissLabelRes = 0,
        ),
        HintKey.DAILY_LOG_MOOD to CoachMarkDef(
            key = HintKey.DAILY_LOG_MOOD,
            titleRes = 0,
            bodyRes = 0,
            nextKey = HintKey.DAILY_LOG_ENERGY,
            dismissLabelRes = 0,
        ),
        HintKey.DAILY_LOG_ENERGY to CoachMarkDef(
            key = HintKey.DAILY_LOG_ENERGY,
            titleRes = 0,
            bodyRes = 0,
            nextKey = null,
            dismissLabelRes = 0,
        ),
    )

    @Test
    fun `walkthroughStepList WHEN validChain THEN returnsOrderedList`() {
        val result = walkthroughStepList(HintKey.DAILY_LOG_WELCOME, chainDefs)

        assertEquals(
            listOf(
                HintKey.DAILY_LOG_WELCOME,
                HintKey.DAILY_LOG_MOOD,
                HintKey.DAILY_LOG_ENERGY,
            ),
            result,
        )
    }

    @Test
    fun `walkthroughStepList WHEN singleStep THEN returnsSingleElementList`() {
        val singleDef = mapOf(
            HintKey.DAILY_LOG_ENERGY to CoachMarkDef(
                key = HintKey.DAILY_LOG_ENERGY,
                titleRes = 0,
                bodyRes = 0,
                nextKey = null,
                dismissLabelRes = 0,
            ),
        )

        val result = walkthroughStepList(HintKey.DAILY_LOG_ENERGY, singleDef)

        assertEquals(listOf(HintKey.DAILY_LOG_ENERGY), result)
    }

    @Test
    fun `walkthroughStepList WHEN startKeyMissing THEN returnsEmptyList`() {
        val result = walkthroughStepList(HintKey.TRACKER_WELCOME, chainDefs)

        assertEquals(emptyList(), result)
    }
}
