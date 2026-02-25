package com.veleda.cyclewise.ui.coachmark

import androidx.compose.ui.geometry.Rect
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for [CoachMarkState].
 *
 * Uses a [TestScope] for coroutine control and mockk for [HintPreferences].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoachMarkStateTest {

    private lateinit var mockHintPreferences: HintPreferences
    private lateinit var testScope: TestScope
    private lateinit var state: CoachMarkState

    private val testBounds = Rect(10f, 20f, 200f, 80f)

    private val defWelcome = CoachMarkDef(
        key = HintKey.DAILY_LOG_WELCOME,
        titleRes = 0,
        bodyRes = 0,
        nextKey = HintKey.DAILY_LOG_EXPLORE_TABS,
        dismissLabelRes = 0,
    )
    private val defExploreTabs = CoachMarkDef(
        key = HintKey.DAILY_LOG_EXPLORE_TABS,
        titleRes = 0,
        bodyRes = 0,
        nextKey = HintKey.DAILY_LOG_PERIOD_TAB,
        dismissLabelRes = 0,
    )
    private val defPeriodToggle = CoachMarkDef(
        key = HintKey.DAILY_LOG_PERIOD_TOGGLE,
        titleRes = 0,
        bodyRes = 0,
        nextKey = null,
        dismissLabelRes = 0,
    )

    private val allDefs = mapOf(
        HintKey.DAILY_LOG_WELCOME to defWelcome,
        HintKey.DAILY_LOG_EXPLORE_TABS to defExploreTabs,
        HintKey.DAILY_LOG_PERIOD_TOGGLE to defPeriodToggle,
    )

    @Before
    fun setUp() {
        mockHintPreferences = mockk(relaxed = true)
        testScope = TestScope()
        state = CoachMarkState(mockHintPreferences, testScope)
    }

    @Test
    fun `showHint WHEN boundsRegistered THEN setsActive`() = runTest {
        // GIVEN — target bounds are registered
        state.registerTarget(HintKey.DAILY_LOG_WELCOME, testBounds)

        // WHEN — showHint is called
        state.showHint(defWelcome)

        // THEN — active hint is set
        val active = state.active.value
        assertNotNull(active, "Active coach mark should be non-null after showHint")
        assertEquals(HintKey.DAILY_LOG_WELCOME, active.def.key)
        assertEquals(testBounds, active.targetBounds)
    }

    @Test
    fun `showHint WHEN boundsNotYetRegistered THEN pendingUntilRegistered`() = runTest {
        // WHEN — showHint is called before bounds are registered
        state.showHint(defWelcome)

        // THEN — active is null (pending)
        assertNull(state.active.value, "Active should be null until bounds are registered")

        // WHEN — bounds are registered
        state.registerTarget(HintKey.DAILY_LOG_WELCOME, testBounds)

        // THEN — active is set
        val active = state.active.value
        assertNotNull(active, "Active should resolve once bounds are registered")
        assertEquals(HintKey.DAILY_LOG_WELCOME, active.def.key)
    }

    @Test
    fun `advanceOrDismiss WHEN nextKeyExists THEN chainsToNext`() {
        // GIVEN — welcome hint is active, explore tabs bounds are registered
        state.registerTarget(HintKey.DAILY_LOG_WELCOME, testBounds)
        state.registerTarget(HintKey.DAILY_LOG_EXPLORE_TABS, Rect(10f, 100f, 300f, 150f))
        state.showHint(defWelcome)

        // WHEN — advance
        state.advanceOrDismiss(allDefs)
        testScope.advanceUntilIdle()

        // THEN — active is now the next hint
        val active = state.active.value
        assertNotNull(active, "Active should be the next hint in the chain")
        assertEquals(HintKey.DAILY_LOG_EXPLORE_TABS, active.def.key)

        // AND — markHintSeen was called for the previous hint
        coVerify { mockHintPreferences.markHintSeen(HintKey.DAILY_LOG_WELCOME) }
    }

    @Test
    fun `advanceOrDismiss WHEN lastHint THEN clearsActive`() {
        // GIVEN — period toggle hint (last in chain) is active
        state.registerTarget(HintKey.DAILY_LOG_PERIOD_TOGGLE, testBounds)
        state.showHint(defPeriodToggle)

        // WHEN — advance
        state.advanceOrDismiss(allDefs)
        testScope.advanceUntilIdle()

        // THEN — active is cleared
        assertNull(state.active.value, "Active should be null after the last hint is dismissed")
        coVerify { mockHintPreferences.markHintSeen(HintKey.DAILY_LOG_PERIOD_TOGGLE) }
    }

    @Test
    fun `dismiss WHEN hintHasNext THEN clearsActiveWithoutAdvancing`() {
        // GIVEN — welcome hint is active (has nextKey)
        state.registerTarget(HintKey.DAILY_LOG_WELCOME, testBounds)
        state.showHint(defWelcome)

        // WHEN — dismiss (not advance)
        state.dismiss()
        testScope.advanceUntilIdle()

        // THEN — active is cleared and markHintSeen is called
        assertNull(state.active.value, "Active should be null after dismiss")
        coVerify { mockHintPreferences.markHintSeen(HintKey.DAILY_LOG_WELCOME) }
    }
}
