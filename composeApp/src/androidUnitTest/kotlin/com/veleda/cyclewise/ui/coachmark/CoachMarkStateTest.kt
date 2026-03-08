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
    private val defPeriodTab = CoachMarkDef(
        key = HintKey.DAILY_LOG_PERIOD_TAB,
        titleRes = 0,
        bodyRes = 0,
        nextKey = HintKey.DAILY_LOG_PERIOD_TOGGLE,
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
        HintKey.DAILY_LOG_PERIOD_TAB to defPeriodTab,
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

        // AND — no pending hint key
        assertNull(state.pendingHintKey.value, "pendingHintKey should be null when bounds are available")
    }

    @Test
    fun `showHint WHEN boundsNotYetRegistered THEN pendingUntilRegistered`() = runTest {
        // WHEN — showHint is called before bounds are registered
        state.showHint(defWelcome)

        // THEN — active is null (pending) and pendingHintKey is set
        assertNull(state.active.value, "Active should be null until bounds are registered")
        assertEquals(
            HintKey.DAILY_LOG_WELCOME,
            state.pendingHintKey.value,
            "pendingHintKey should be set when bounds are not available"
        )

        // WHEN — bounds are registered
        state.registerTarget(HintKey.DAILY_LOG_WELCOME, testBounds)

        // THEN — active is set and pendingHintKey is cleared
        val active = state.active.value
        assertNotNull(active, "Active should resolve once bounds are registered")
        assertEquals(HintKey.DAILY_LOG_WELCOME, active.def.key)
        assertNull(state.pendingHintKey.value, "pendingHintKey should be cleared after resolve")
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
    fun `skipAll WHEN walkthroughActive THEN marksAllSeenAndClearsActive`() {
        // GIVEN — welcome hint is active
        state.registerTarget(HintKey.DAILY_LOG_WELCOME, testBounds)
        state.showHint(defWelcome)

        // WHEN — skip the entire walkthrough
        state.skipAll(allDefs)
        testScope.advanceUntilIdle()

        // THEN — active and pendingHintKey are cleared
        assertNull(state.active.value, "Active should be null after skipAll")
        assertNull(state.pendingHintKey.value, "pendingHintKey should be null after skipAll")

        // AND — markHintSeen was called for every hint key in the walkthrough
        coVerify { mockHintPreferences.markHintSeen(HintKey.DAILY_LOG_WELCOME) }
        coVerify { mockHintPreferences.markHintSeen(HintKey.DAILY_LOG_EXPLORE_TABS) }
        coVerify { mockHintPreferences.markHintSeen(HintKey.DAILY_LOG_PERIOD_TAB) }
        coVerify { mockHintPreferences.markHintSeen(HintKey.DAILY_LOG_PERIOD_TOGGLE) }
    }

    @Test
    fun `advanceOrDismiss WHEN nextTargetNotComposed THEN setsPendingAndClearsActive`() {
        // GIVEN — PERIOD_TAB hint is active, but PERIOD_TOGGLE bounds are NOT registered
        state.registerTarget(HintKey.DAILY_LOG_WELCOME, testBounds)
        state.registerTarget(HintKey.DAILY_LOG_EXPLORE_TABS, Rect(10f, 100f, 300f, 150f))
        state.registerTarget(HintKey.DAILY_LOG_PERIOD_TAB, Rect(10f, 100f, 200f, 140f))
        state.showHint(defWelcome)
        state.advanceOrDismiss(allDefs) // → EXPLORE_TABS
        testScope.advanceUntilIdle()
        state.advanceOrDismiss(allDefs) // → PERIOD_TAB
        testScope.advanceUntilIdle()

        assertEquals(HintKey.DAILY_LOG_PERIOD_TAB, state.active.value?.def?.key)

        // WHEN — advance from PERIOD_TAB → PERIOD_TOGGLE (bounds not registered)
        state.advanceOrDismiss(allDefs)
        testScope.advanceUntilIdle()

        // THEN — active is cleared and pendingHintKey is set
        assertNull(state.active.value, "Active should be null when next target is not composed")
        assertEquals(
            HintKey.DAILY_LOG_PERIOD_TOGGLE,
            state.pendingHintKey.value,
            "pendingHintKey should be PERIOD_TOGGLE"
        )

        // WHEN — the Period page composes and registers the toggle target
        val toggleBounds = Rect(20f, 200f, 350f, 260f)
        state.registerTarget(HintKey.DAILY_LOG_PERIOD_TOGGLE, toggleBounds)

        // THEN — pending resolves: active shows the PERIOD_TOGGLE hint
        val active = state.active.value
        assertNotNull(active, "Active should resolve after registerTarget")
        assertEquals(HintKey.DAILY_LOG_PERIOD_TOGGLE, active.def.key)
        assertEquals(toggleBounds, active.targetBounds)
        assertNull(state.pendingHintKey.value, "pendingHintKey should be cleared after resolve")
    }

    @Test
    fun `hold WHEN registerTargetCalledWhileHeld THEN doesNotActivatePending`() {
        // GIVEN — PERIOD_TAB is active and we advance to PERIOD_TOGGLE (pending)
        state.registerTarget(HintKey.DAILY_LOG_PERIOD_TAB, testBounds)
        state.showHint(defPeriodTab)
        state.hold()
        state.advanceOrDismiss(allDefs)
        testScope.advanceUntilIdle()

        // Preconditions: active is null, pending is PERIOD_TOGGLE
        assertNull(state.active.value, "Precondition: active should be null while pending")
        assertEquals(
            HintKey.DAILY_LOG_PERIOD_TOGGLE,
            state.pendingHintKey.value,
            "Precondition: pendingHintKey should be PERIOD_TOGGLE"
        )

        // WHEN — target registers bounds while held
        val toggleBounds = Rect(20f, 200f, 350f, 260f)
        state.registerTarget(HintKey.DAILY_LOG_PERIOD_TOGGLE, toggleBounds)

        // THEN — activation is deferred; active remains null
        assertNull(state.active.value, "Active should remain null while held")
        assertEquals(
            HintKey.DAILY_LOG_PERIOD_TOGGLE,
            state.pendingHintKey.value,
            "pendingHintKey should remain set while held"
        )
    }

    @Test
    fun `release WHEN pendingBoundsRegisteredWhileHeld THEN activatesPendingHint`() {
        // GIVEN — PERIOD_TAB is active and we advance to PERIOD_TOGGLE (pending) while held
        state.registerTarget(HintKey.DAILY_LOG_PERIOD_TAB, testBounds)
        state.showHint(defPeriodTab)
        state.hold()
        state.advanceOrDismiss(allDefs)
        testScope.advanceUntilIdle()

        // Register bounds while held (deferred activation)
        val toggleBounds = Rect(20f, 200f, 350f, 260f)
        state.registerTarget(HintKey.DAILY_LOG_PERIOD_TOGGLE, toggleBounds)
        assertNull(state.active.value, "Precondition: active should be null while held")

        // WHEN — release the hold
        state.release()

        // THEN — pending hint activates with the stored bounds
        val active = state.active.value
        assertNotNull(active, "Active should resolve after release")
        assertEquals(HintKey.DAILY_LOG_PERIOD_TOGGLE, active.def.key)
        assertEquals(toggleBounds, active.targetBounds)
        assertNull(state.pendingHintKey.value, "pendingHintKey should be cleared after release")
    }

    @Test
    fun `release WHEN noPending THEN noOp`() {
        // GIVEN — a hint is active with no pending
        state.registerTarget(HintKey.DAILY_LOG_WELCOME, testBounds)
        state.showHint(defWelcome)
        state.hold()

        // WHEN — release with no pending hint
        state.release()

        // THEN — active is unchanged
        val active = state.active.value
        assertNotNull(active, "Active should remain set")
        assertEquals(HintKey.DAILY_LOG_WELCOME, active.def.key)
        assertNull(state.pendingHintKey.value, "pendingHintKey should remain null")
    }

    @Test
    fun `registerTarget WHEN pendingResolves THEN clearsPendingHintKey`() {
        // GIVEN — a hint is pending (bounds not yet available)
        state.showHint(defPeriodToggle)
        assertEquals(
            HintKey.DAILY_LOG_PERIOD_TOGGLE,
            state.pendingHintKey.value,
            "Precondition: pendingHintKey should be set"
        )
        assertNull(state.active.value, "Precondition: active should be null")

        // WHEN — target registers its bounds
        val bounds = Rect(50f, 300f, 400f, 360f)
        state.registerTarget(HintKey.DAILY_LOG_PERIOD_TOGGLE, bounds)

        // THEN — pending clears and active is set
        assertNull(state.pendingHintKey.value, "pendingHintKey should be null after resolve")
        val active = state.active.value
        assertNotNull(active, "Active should be set after pending resolves")
        assertEquals(HintKey.DAILY_LOG_PERIOD_TOGGLE, active.def.key)
        assertEquals(bounds, active.targetBounds)
    }

    @Test
    fun `skipToKey WHEN targetInChain THEN marksIntermediatesSeenAndActivatesTarget`() {
        // GIVEN — welcome is active, period tab bounds are registered
        state.registerTarget(HintKey.DAILY_LOG_WELCOME, testBounds)
        state.registerTarget(HintKey.DAILY_LOG_EXPLORE_TABS, Rect(10f, 100f, 300f, 150f))
        val periodTabBounds = Rect(10f, 100f, 200f, 140f)
        state.registerTarget(HintKey.DAILY_LOG_PERIOD_TAB, periodTabBounds)
        state.showHint(defWelcome)

        // WHEN — skip from WELCOME to PERIOD_TAB
        state.skipToKey(HintKey.DAILY_LOG_PERIOD_TAB, allDefs)
        testScope.advanceUntilIdle()

        // THEN — active is PERIOD_TAB
        val active = state.active.value
        assertNotNull(active, "Active should be the skip target")
        assertEquals(HintKey.DAILY_LOG_PERIOD_TAB, active.def.key)

        // AND — intermediate steps (WELCOME, EXPLORE_TABS) were marked seen
        coVerify { mockHintPreferences.markHintSeen(HintKey.DAILY_LOG_WELCOME) }
        coVerify { mockHintPreferences.markHintSeen(HintKey.DAILY_LOG_EXPLORE_TABS) }

        // AND — target step was NOT marked seen (it's now active, user still needs to dismiss)
        coVerify(exactly = 0) { mockHintPreferences.markHintSeen(HintKey.DAILY_LOG_PERIOD_TAB) }
    }

    @Test
    fun `skipToKey WHEN targetNotInChain THEN marksAllSeenAndClearsActive`() {
        // GIVEN — PERIOD_TAB is active (chain: PERIOD_TAB → PERIOD_TOGGLE → null)
        state.registerTarget(HintKey.DAILY_LOG_PERIOD_TAB, testBounds)
        state.showHint(defPeriodTab)

        // WHEN — skip to a key that doesn't exist in the remaining chain
        state.skipToKey(HintKey.DAILY_LOG_WELCOME, allDefs) // WELCOME is not after PERIOD_TAB
        testScope.advanceUntilIdle()

        // THEN — active and pending are cleared
        assertNull(state.active.value, "Active should be null when target not found in chain")
        assertNull(state.pendingHintKey.value, "pendingHintKey should be null when target not found")

        // AND — all chain steps from current onward are marked seen
        coVerify { mockHintPreferences.markHintSeen(HintKey.DAILY_LOG_PERIOD_TAB) }
        coVerify { mockHintPreferences.markHintSeen(HintKey.DAILY_LOG_PERIOD_TOGGLE) }
    }

    // --- Unregister target tests ---

    @Test
    fun `unregisterTarget WHEN boundsWereRegistered THEN showHintGoesPending`() {
        // GIVEN — bounds are registered and then unregistered
        state.registerTarget(HintKey.DAILY_LOG_WELCOME, testBounds)
        state.unregisterTarget(HintKey.DAILY_LOG_WELCOME)

        // WHEN — showHint is called
        state.showHint(defWelcome)

        // THEN — hint is pending because bounds were removed
        assertNull(state.active.value, "Active should be null after bounds were unregistered")
        assertEquals(
            HintKey.DAILY_LOG_WELCOME,
            state.pendingHintKey.value,
            "pendingHintKey should be set when bounds were unregistered"
        )
    }

    @Test
    fun `unregisterTarget WHEN keyNeverRegistered THEN noOp`() {
        // WHEN — unregister a key that was never registered
        state.unregisterTarget(HintKey.DAILY_LOG_WELCOME)

        // THEN — no crash, state is unchanged
        assertNull(state.active.value, "Active should remain null")
        assertNull(state.pendingHintKey.value, "pendingHintKey should remain null")
    }

    @Test
    fun `registerTarget WHEN activeKeyMatchesAndBoundsChange THEN updatesActiveBounds`() {
        // GIVEN — a hint is active
        state.registerTarget(HintKey.DAILY_LOG_WELCOME, testBounds)
        state.showHint(defWelcome)
        assertEquals(testBounds, state.active.value?.targetBounds)

        // WHEN — the same target reports new bounds (e.g. during scroll animation)
        val newBounds = Rect(50f, 100f, 250f, 160f)
        state.registerTarget(HintKey.DAILY_LOG_WELCOME, newBounds)

        // THEN — active bounds are updated
        val active = state.active.value
        assertNotNull(active, "Active should still be set")
        assertEquals(HintKey.DAILY_LOG_WELCOME, active.def.key)
        assertEquals(newBounds, active.targetBounds, "Active bounds should update to the new position")
    }
}
