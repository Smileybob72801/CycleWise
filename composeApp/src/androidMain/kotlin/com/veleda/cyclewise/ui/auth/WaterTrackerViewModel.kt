package com.veleda.cyclewise.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

/**
 * UI state for the pre-authentication water tracker on the lock screen.
 *
 * @property todayCups           Number of water cups logged today (pre-auth draft).
 * @property yesterdayCupsForPrompt Yesterday's cup count when > 0, prompting the user to log in
 *                               and sync drafts to the encrypted database. Null when there are
 *                               no yesterday drafts to surface.
 */
data class WaterTrackerUiState(
    val todayCups: Int = 0,
    val yesterdayCupsForPrompt: Int? = null
)

/**
 * Pre-authentication water intake tracker for the lock screen.
 *
 * Reads and writes to [LockedWaterDraft] (plaintext DataStore) since the encrypted
 * database is not yet available. On init, ensures the day has rolled over (resets
 * today's count and prunes old entries), then continuously collects draft changes.
 *
 * Uses a pure [reduce] function for optimistic state updates, fixing a race condition
 * where rapid taps could read stale state before the DataStore flow re-emitted.
 * Side effects (DataStore persistence) are launched in [onEvent] after the state update.
 *
 * Exposes today's cup count and an optional yesterday cup count prompting the user
 * to log in so drafts can be synced to the encrypted database.
 */
class WaterTrackerViewModel(
    private val lockedWaterDraft: LockedWaterDraft
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaterTrackerUiState())
    val uiState: StateFlow<WaterTrackerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            lockedWaterDraft.ensureRolledOver(today)

            lockedWaterDraft.drafts.collect { drafts ->
                val currentToday = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val yesterday = currentToday.minus(1, DateTimeUnit.DAY)
                val todayCups = drafts[currentToday] ?: 0
                val yesterdayCups = drafts[yesterday]
                _uiState.update {
                    it.copy(
                        todayCups = todayCups,
                        yesterdayCupsForPrompt = if (yesterdayCups != null && yesterdayCups > 0) {
                            yesterdayCups
                        } else null
                    )
                }
            }
        }
    }

    /**
     * Single entry-point for all water tracker user interactions.
     *
     * Applies an optimistic state update via [reduce], then persists the new cup count
     * to [LockedWaterDraft]. The DataStore flow will eventually re-emit and reconcile,
     * but the optimistic update prevents stale reads on rapid taps.
     */
    fun onEvent(event: WaterTrackerEvent) {
        val previousCups = _uiState.value.todayCups
        _uiState.update { reduce(it, event) }
        val newCups = _uiState.value.todayCups

        if (newCups != previousCups) {
            viewModelScope.launch {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                lockedWaterDraft.setCups(today, newCups)
            }
        }
    }

    /**
     * Pure function that returns the new [WaterTrackerUiState] for a given event.
     *
     * Contains no side effects — DataStore persistence is handled in [onEvent]
     * after the state has been updated.
     */
    private fun reduce(state: WaterTrackerUiState, event: WaterTrackerEvent): WaterTrackerUiState {
        return when (event) {
            is WaterTrackerEvent.Increment -> state.copy(todayCups = state.todayCups + 1)
            is WaterTrackerEvent.Decrement -> {
                if (state.todayCups > 0) state.copy(todayCups = state.todayCups - 1)
                else state
            }
        }
    }
}
