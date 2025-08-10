package com.veleda.cyclewise.ui.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.domain.repository.CycleRepository
import com.veleda.cyclewise.domain.usecases.EndCycleUseCase
import com.veleda.cyclewise.domain.usecases.StartNewCycleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * ViewModel for the TrackerScreen.
 * Exposes a list of cycles and handles adding a new cycle.
 */
class CycleViewModel(
    private val cycleRepository: CycleRepository,
    private val startNewCycleUseCase: StartNewCycleUseCase,
    private val endCycleUseCase: EndCycleUseCase,
) : ViewModel() {

    private val _cycles = MutableStateFlow<List<com.veleda.cyclewise.domain.models.Cycle>>(emptyList())
    val cycles: StateFlow<List<com.veleda.cyclewise.domain.models.Cycle>> = _cycles.asStateFlow()

    init {
        viewModelScope.launch {
            // Load existing cycles on start
            _cycles.value = cycleRepository.getAllCycles()
        }
    }

    /** Called when the user taps the Add Cycle button. */
    @OptIn(ExperimentalTime::class)
    fun onAddNewCycleClicked() {
        viewModelScope.launch {
            // Add a new cycle and refresh list
            val now = Clock.System.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            startNewCycleUseCase(today)
            _cycles.value = cycleRepository.getAllCycles()
        }
    }

    /** Called when the user taps the End Cycle button. */
    @OptIn(ExperimentalTime::class)
    fun onEndCycleClicked() {
        viewModelScope.launch {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            endCycleUseCase(endDate = today)
            _cycles.value = cycleRepository.getAllCycles()
        }
    }
}
