package com.veleda.cyclewise.ui.tracker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.veleda.cyclewise.domain.models.*
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.datetime.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.*
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class CycleViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockRepository: PeriodRepository
    private lateinit var mockSymptomProvider: SymptomLibraryProvider
    private lateinit var mockMedicationProvider: MedicationLibraryProvider
    private lateinit var viewModel: TrackerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockRepository = mockk(relaxed = true)
        mockSymptomProvider = mockk(relaxed = true)
        mockMedicationProvider = mockk(relaxed = true)

        // Mock initial data flows
        every { mockRepository.observeDayDetails() } returns flowOf(emptyMap())
        every { mockRepository.getAllPeriods() } returns flowOf(emptyList())
        every { mockSymptomProvider.symptoms } returns flowOf(emptyList())
        every { mockMedicationProvider.medications } returns flowOf(emptyList())

        viewModel = TrackerViewModel(mockRepository, mockSymptomProvider, mockMedicationProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val pastDate = today.minus(5, DateTimeUnit.DAY)
    private val futureDate = today.plus(5, DateTimeUnit.DAY)

    @Test
    fun onEvent_DateClicked_WHEN_dateIsInExistingCycle_THEN_showsLogSheet() = runTest {
        val testPeriod = mockk<Period>()
        val fakeLog = mockk<FullDailyLog>()
        coEvery { mockRepository.getFullLogForDate(pastDate) } returns fakeLog

        viewModel.onEvent(TrackerEvent.DateClicked(pastDate, testPeriod))

        viewModel.uiState.test {
            assertEquals(fakeLog, awaitItem().logForSheet, "Log for sheet should be set")
        }
    }

    @Test
    fun onEvent_DateClicked_WHEN_dateIsFuture_THEN_doesNothing() = runTest {
        val initialState = viewModel.uiState.value

        viewModel.onEvent(TrackerEvent.DateClicked(futureDate, null))

        assertEquals(initialState, viewModel.uiState.value, "State should not change for future date")
    }

    @Test
    fun onEvent_DateClicked_WHEN_cycleIsOngoing_THEN_doesNothing() = runTest {
        val ongoingPeriod = Period("ongoing-id", today.minus(2, DateTimeUnit.DAY), null, Clock.System.now(), Clock.System.now())
        every { mockRepository.getAllPeriods() } returns flowOf(listOf(ongoingPeriod))
        viewModel = TrackerViewModel(mockRepository, mockSymptomProvider, mockMedicationProvider)
        advanceUntilIdle() // Ensure the new state is collected
        val initialState = viewModel.uiState.value

        assertNotNull(initialState.ongoingPeriod, "Precondition failed: ongoingPeriod should be set")

        viewModel.onEvent(TrackerEvent.DateClicked(pastDate, null))

        val finalState = viewModel.uiState.value
        assertEquals(initialState.selectionStartDate, finalState.selectionStartDate, "Selection should not start if a cycle is ongoing")
        assertEquals(initialState, finalState)
    }

    @Test
    fun onEvent_DateClicked_WHEN_firstTapOnValidDate_THEN_setsSelectionStartDate() = runTest {
        viewModel.onEvent(TrackerEvent.DateClicked(pastDate, null))

        viewModel.uiState.test {
            assertEquals(pastDate, awaitItem().selectionStartDate)
        }
    }

    @Test
    fun onEvent_SaveSelectionClicked_WHEN_rangeIsInPastAndAvailable_THEN_createsCompletedCycle() = runTest {
        val startDate = today.minus(10, DateTimeUnit.DAY)
        val endDate = today.minus(5, DateTimeUnit.DAY)
        coEvery { mockRepository.isDateRangeAvailable(startDate, endDate) } returns true

        viewModel.onEvent(TrackerEvent.DateClicked(startDate, null))
        viewModel.onEvent(TrackerEvent.DateClicked(endDate, null))

        viewModel.onEvent(TrackerEvent.SaveSelectionClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.createCompletedPeriod(startDate, endDate) }
        viewModel.uiState.test {
            assertNull(awaitItem().selectionStartDate, "Selection should be cleared after save")
        }
    }

    @Test
    fun onEvent_SaveSelectionClicked_WHEN_startDateIsToday_THEN_startsNewOngoingCycle() = runTest {
        coEvery { mockRepository.isDateRangeAvailable(today, today) } returns true
        viewModel.onEvent(TrackerEvent.DateClicked(today, null))

        viewModel.onEvent(TrackerEvent.SaveSelectionClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.startNewPeriod(today) }
    }

    @Test
    fun onEvent_SaveSelectionClicked_WHEN_rangeOverlaps_THEN_doesNotCreateCycle() = runTest {
        val startDate = today.minus(10, DateTimeUnit.DAY)
        val endDate = today.minus(5, DateTimeUnit.DAY)
        coEvery { mockRepository.isDateRangeAvailable(startDate, endDate) } returns false

        viewModel.onEvent(TrackerEvent.DateClicked(startDate, null))
        viewModel.onEvent(TrackerEvent.DateClicked(endDate, null))

        viewModel.onEvent(TrackerEvent.SaveSelectionClicked)
        advanceUntilIdle()

        coVerify(exactly = 0) { mockRepository.createCompletedPeriod(any(), any()) }
        coVerify(exactly = 0) { mockRepository.startNewPeriod(any()) }
    }

    @Test
    fun onEvent_EndCycleClicked_WHEN_ongoingCycleExists_THEN_callsRepositoryEndCycle() = runTest {
        val fakeOngoingPeriod = Period("ongoing-id", today.minus(5, DateTimeUnit.DAY), null, Clock.System.now(), Clock.System.now())
        every { mockRepository.getAllPeriods() } returns flowOf(listOf(fakeOngoingPeriod))
        viewModel = TrackerViewModel(mockRepository, mockSymptomProvider, mockMedicationProvider)
        advanceUntilIdle()

        viewModel.onEvent(TrackerEvent.EndPeriodClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.endPeriod(fakeOngoingPeriod.id, today) }
    }
}