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

    private val targetBoundsMap = mutableMapOf<HintKey, Rect>()
    private var pendingDef: CoachMarkDef? = null

    /**
     * Called by [coachMarkTarget] modifiers to report the screen-space bounds of a
     * target composable. If a pending hint matches [key], the overlay activates immediately.
     */
    fun registerTarget(key: HintKey, bounds: Rect) {
        targetBoundsMap[key] = bounds
        val pending = pendingDef
        if (pending != null && pending.key == key) {
            _active.value = ActiveCoachMark(pending, bounds)
            pendingDef = null
        }
    }

    /**
     * Activates a coach mark hint. If the target's bounds are already registered, the
     * overlay shows immediately; otherwise the definition is held as pending until the
     * target composable reports its bounds via [registerTarget].
     */
    fun showHint(def: CoachMarkDef) {
        val bounds = targetBoundsMap[def.key]
        if (bounds != null) {
            _active.value = ActiveCoachMark(def, bounds)
            pendingDef = null
        } else {
            pendingDef = def
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
    }
}
