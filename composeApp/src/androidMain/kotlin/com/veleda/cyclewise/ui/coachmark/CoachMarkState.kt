package com.veleda.cyclewise.ui.coachmark

import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Snapshot of the currently active coach mark and the screen-space bounds of its target.
 *
 * @property def          The coach mark definition being displayed.
 * @property targetBounds The target composable's bounds in root coordinates.
 */
data class ActiveCoachMark(
    val def: CoachMarkDef,
    val targetBounds: Rect,
)

/**
 * Per-screen state holder that drives the [CoachMarkOverlay].
 *
 * Created locally via `remember` inside the composable that hosts the overlay.
 * Target composables register their bounds via [registerTarget], and the walkthrough
 * definition calls [showHint] to begin a chain.
 *
 * @param hintPreferences Singleton persistence for seen/unseen flags.
 * @param scope           Coroutine scope for fire-and-forget persistence writes.
 */
class CoachMarkState(
    private val hintPreferences: HintPreferences,
    private val scope: CoroutineScope,
) {
    private val _active = MutableStateFlow<ActiveCoachMark?>(null)

    /** The currently active coach mark, or `null` when no hint is showing. */
    val active: StateFlow<ActiveCoachMark?> = _active.asStateFlow()

    private val _pendingHintKey = MutableStateFlow<HintKey?>(null)

    /**
     * The [HintKey] of a hint whose target composable has not yet reported bounds.
     *
     * Hosting screens observe this to trigger navigation (e.g. scrolling a pager)
     * so the target becomes visible and can register its bounds via [registerTarget].
     */
    val pendingHintKey: StateFlow<HintKey?> = _pendingHintKey.asStateFlow()

    private val targetBoundsMap = mutableMapOf<HintKey, Rect>()
    private var pendingDef: CoachMarkDef? = null

    /**
     * When `true`, [registerTarget] stores bounds but defers activating pending hints
     * until [release] is called. This prevents the overlay from appearing mid-animation
     * (e.g., while a pager is scrolling to a new page).
     */
    private var held = false

    /**
     * Called by [coachMarkTarget] modifiers to report the screen-space bounds of a
     * target composable. If a pending hint matches [key] and the state is not [held],
     * the overlay activates immediately. Otherwise the bounds are stored for later
     * resolution when [release] is called.
     *
     * When the active hint's key matches [key], the active bounds are updated so that
     * the highlight tracks the target during scroll animations.
     */
    fun registerTarget(key: HintKey, bounds: Rect) {
        targetBoundsMap[key] = bounds

        // Update active bounds when the target moves (e.g. during scroll animation).
        val current = _active.value
        if (current != null && current.def.key == key) {
            _active.value = current.copy(targetBounds = bounds)
        }

        if (held) return
        val pending = pendingDef
        if (pending != null && pending.key == key) {
            _active.value = ActiveCoachMark(pending, bounds)
            pendingDef = null
            _pendingHintKey.value = null
        }
    }

    /**
     * Removes a previously registered target's bounds from [targetBoundsMap].
     *
     * Used by [coachMarkTarget] when its `enabled` parameter is `false`, so that
     * off-screen or inactive targets are not considered registered. This forces
     * [showHint] to take the pending path, triggering navigation (e.g. pager scroll)
     * to bring the target into view before activating the hint.
     */
    fun unregisterTarget(key: HintKey) {
        targetBoundsMap.remove(key)
    }

    /**
     * Activates a coach mark hint. If the target's bounds are already registered,
     * the overlay shows immediately; otherwise the definition is held as pending
     * until the target composable reports bounds via [registerTarget].
     */
    fun showHint(def: CoachMarkDef) {
        val bounds = targetBoundsMap[def.key]
        if (bounds != null) {
            _active.value = ActiveCoachMark(def, bounds)
            pendingDef = null
            _pendingHintKey.value = null
        } else {
            pendingDef = def
            _pendingHintKey.value = def.key
            _active.value = null
        }
    }

    /**
     * Marks the current hint as seen and advances to the next hint in the chain.
     * If there is no next hint, clears the overlay.
     *
     * @param allDefs Full walkthrough map so the next [CoachMarkDef] can be resolved.
     */
    fun advanceOrDismiss(allDefs: Map<HintKey, CoachMarkDef>) {
        val current = _active.value?.def ?: return
        scope.launch { hintPreferences.markHintSeen(current.key) }
        val nextKey = current.nextKey
        if (nextKey != null) {
            val nextDef = allDefs[nextKey]
            if (nextDef != null) {
                showHint(nextDef)
                return
            }
        }
        _active.value = null
        pendingDef = null
    }

    /**
     * Prevents pending hints from activating until [release] is called.
     *
     * Call this before starting an animation or transition that would cause target
     * composables to register bounds prematurely (e.g., a pager scroll). Pair with
     * [release] after the animation completes so the next hint appears only once the
     * UI has settled.
     */
    fun hold() {
        held = true
    }

    /**
     * Resumes normal activation after a previous [hold].
     *
     * If a pending hint's target bounds were registered while held, the hint activates
     * immediately upon release. Safe to call when not held (no-op).
     */
    fun release() {
        held = false
        val pending = pendingDef ?: return
        val bounds = targetBoundsMap[pending.key] ?: return
        _active.value = ActiveCoachMark(pending, bounds)
        pendingDef = null
        _pendingHintKey.value = null
    }

    /**
     * Skips forward in the walkthrough chain to the step identified by [targetKey].
     *
     * Walks the [CoachMarkDef.nextKey] chain starting from the currently active hint,
     * marking each intermediate step (including the current one) as seen. When
     * [targetKey] is found, it is activated via [showHint]. If [targetKey] is not
     * found in the remaining chain, all remaining steps are marked seen and the
     * overlay is cleared.
     *
     * Used by the "I don't have periods" skip button to jump past period-specific
     * steps without terminating the entire walkthrough.
     *
     * @param targetKey The [HintKey] to skip to.
     * @param allDefs   Full walkthrough map so definitions can be resolved by key.
     */
    fun skipToKey(targetKey: HintKey, allDefs: Map<HintKey, CoachMarkDef>) {
        val current = _active.value?.def ?: return
        var cursor: CoachMarkDef? = current
        while (cursor != null) {
            if (cursor.key == targetKey) {
                showHint(cursor)
                return
            }
            val keyToMark = cursor.key
            scope.launch { hintPreferences.markHintSeen(keyToMark) }
            cursor = cursor.nextKey?.let { allDefs[it] }
        }
        // Target not found in chain — clear everything.
        _active.value = null
        pendingDef = null
        _pendingHintKey.value = null
    }

    /**
     * Marks **all** hints in the walkthrough as seen and clears the overlay.
     *
     * Used by the long-press "Hold to skip" button to terminate the entire
     * walkthrough in one action.
     *
     * @param allDefs Full walkthrough map whose keys will all be persisted as seen.
     */
    fun skipAll(allDefs: Map<HintKey, CoachMarkDef>) {
        scope.launch {
            allDefs.keys.forEach { hintPreferences.markHintSeen(it) }
        }
        _active.value = null
        pendingDef = null
        _pendingHintKey.value = null
    }
}
