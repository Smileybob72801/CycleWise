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

    fun onIncrement() {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val current = _uiState.value.todayCups
            lockedWaterDraft.setCups(today, current + 1)
        }
    }

    fun onDecrement() {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val current = _uiState.value.todayCups
            if (current > 0) {
                lockedWaterDraft.setCups(today, current - 1)
            }
        }
    }
}
