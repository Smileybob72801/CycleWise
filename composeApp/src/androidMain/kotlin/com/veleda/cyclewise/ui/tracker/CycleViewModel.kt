package com.veleda.cyclewise.ui.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.domain.repository.CycleRepository
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
    private val cycleRepositoryProvider: suspend () -> CycleRepository,
    private val startNewCycleUseCaseProvider: suspend () -> StartNewCycleUseCase
) : ViewModel() {

    private val _cycles = MutableStateFlow<List<com.veleda.cyclewise.domain.models.Cycle>>(emptyList())
    val cycles: StateFlow<List<com.veleda.cyclewise.domain.models.Cycle>> = _cycles.asStateFlow()

    private lateinit var cycleRepository: CycleRepository
    private lateinit var startNewCycleUseCase: StartNewCycleUseCase

    init {
        viewModelScope.launch {
            cycleRepository = cycleRepositoryProvider()
            startNewCycleUseCase = startNewCycleUseCaseProvider()
            _cycles.value = cycleRepository.getAllCycles()
        }
    }

    /** Called when the user taps the Add Cycle button. */
    @OptIn(ExperimentalTime::class)
    fun onAddNewCycleClicked() {
        viewModelScope.launch {
            // Wait until lazy services are initialized
            if (!::cycleRepository.isInitialized || !::startNewCycleUseCase.isInitialized) return@launch

            val now = Clock.System.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            startNewCycleUseCase(today)
            _cycles.value = cycleRepository.getAllCycles()
        }
    }
}
