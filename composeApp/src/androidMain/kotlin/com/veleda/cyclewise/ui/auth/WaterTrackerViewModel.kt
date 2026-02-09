package com.veleda.cyclewise.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

data class WaterTrackerUiState(
    val todayCups: Int = 0,
    val yesterdayMessage: String? = null
)

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
                _uiState.value = WaterTrackerUiState(
                    todayCups = todayCups,
                    yesterdayMessage = if (yesterdayCups != null && yesterdayCups > 0) {
                        "You logged $yesterdayCups cups yesterday. Log in to save."
                    } else null
                )
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
