package com.veleda.cyclewise.ui.tracker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.veleda.cyclewise.domain.models.*
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.CycleRepository
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

    private lateinit var mockRepository: CycleRepository
    private lateinit var mockSymptomProvider: SymptomLibraryProvider
    private lateinit var mockMedicationProvider: MedicationLibraryProvider
    private lateinit var viewModel: CycleViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock all dependencies
        mockRepository = mockk(relaxed = true)
        mockSymptomProvider = mockk(relaxed = true)
        mockMedicationProvider = mockk(relaxed = true)

        // Mock the initial data flows to be empty
        every { mockRepository.getAllCycles() } returns flowOf(emptyList())
        every { mockSymptomProvider.symptoms } returns flowOf(emptyList())
        every { mockMedicationProvider.medications } returns flowOf(emptyList())

        // Create the ViewModel instance for tests
        viewModel = CycleViewModel(mockRepository, mockSymptomProvider, mockMedicationProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val pastDate = today.minus(5, DateTimeUnit.DAY)
    private val futureDate = today.plus(5, DateTimeUnit.DAY)

    // --- onDateClicked Tests ---

    @Test
    fun onDateClicked_WHEN_dateIsInExistingCycle_THEN_showsLogSheet() = runTest {
        // ARRANGE
        val testCycle = mockk<Cycle>()
        val fakeLog = mockk<FullDailyLog>()
        coEvery { mockRepository.getFullLogForDate(pastDate) } returns fakeLog

        // ACT
        viewModel.onDateClicked(pastDate, testCycle)

        // ASSERT
        viewModel.uiState.test {
            assertEquals(fakeLog, awaitItem().logForSheet, "Log for sheet should be set")
        }
    }

    @Test
    fun onDateClicked_WHEN_dateIsFuture_THEN_doesNothing() = runTest {
        val initialState = viewModel.uiState.value

        // ACT
        viewModel.onDateClicked(futureDate, null)

        // ASSERT
        assertEquals(initialState, viewModel.uiState.value, "State should not change for future date")
    }

    @Test
    fun onDateClicked_WHEN_cycleIsOngoing_THEN_doesNothing() = runTest {
        // ARRANGE
        val ongoingCycle = Cycle(
            id = "ongoing-id-1",
            startDate = today.minus(2, DateTimeUnit.DAY),
            endDate = null, // This is the crucial property
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        every { mockRepository.getAllCycles() } returns flowOf(listOf(ongoingCycle))

        // Re-create ViewModel to pick up the new initial state
        viewModel = CycleViewModel(mockRepository, mockSymptomProvider, mockMedicationProvider)

        advanceUntilIdle()

        val initialState = viewModel.uiState.value

        assertNotNull(initialState.ongoingCycle, "Precondition failed: ongoingCycle should be set")

        // ACT
        viewModel.onDateClicked(pastDate, null)

        // ASSERT
        val finalState = viewModel.uiState.value
        assertEquals(initialState.selectionStartDate, finalState.selectionStartDate, "Selection should not start if a cycle is ongoing")

        // Assert that the state object itself hasn't changed
        assertEquals(initialState, finalState)
    }

    @Test
    fun onDateClicked_WHEN_firstTapOnValidDate_THEN_setsSelectionStartDate() = runTest {
        // ACT
        viewModel.onDateClicked(pastDate, null)

        // ASSERT
        viewModel.uiState.test {
            assertEquals(pastDate, awaitItem().selectionStartDate)
        }
    }

    // --- onSaveSelection Tests ---

    @Test
    fun onSaveSelection_WHEN_rangeIsInPastAndAvailable_THEN_createsCompletedCycle() = runTest {
        // ARRANGE
        val startDate = today.minus(10, DateTimeUnit.DAY)
        val endDate = today.minus(5, DateTimeUnit.DAY)
        coEvery { mockRepository.isDateRangeAvailable(startDate, endDate) } returns true

        viewModel.onDateClicked(startDate, null)
        viewModel.onDateClicked(endDate, null)

        // ACT
        viewModel.onSaveSelection()

        // ASSERT
        coVerify(exactly = 1) { mockRepository.createCompletedCycle(startDate, endDate) }
        viewModel.uiState.test {
            assertNull(awaitItem().selectionStartDate, "Selection should be cleared after save")
        }
    }

    @Test
    fun onSaveSelection_WHEN_startDateIsToday_THEN_startsNewOngoingCycle() = runTest {
        // ARRANGE
        coEvery { mockRepository.isDateRangeAvailable(today, today) } returns true
        viewModel.onDateClicked(today, null)

        // ACT
        viewModel.onSaveSelection()

        // ASSERT
        coVerify(exactly = 1) { mockRepository.startNewCycle(today) }
    }

    @Test
    fun onSaveSelection_WHEN_rangeOverlaps_THEN_doesNotCreateCycle() = runTest {
        // ARRANGE
        val startDate = today.minus(10, DateTimeUnit.DAY)
        val endDate = today.minus(5, DateTimeUnit.DAY)
        coEvery { mockRepository.isDateRangeAvailable(startDate, endDate) } returns false // Simulate overlap

        viewModel.onDateClicked(startDate, null)
        viewModel.onDateClicked(endDate, null)

        // ACT
        viewModel.onSaveSelection()

        // ASSERT
        coVerify(exactly = 0) { mockRepository.createCompletedCycle(any(), any()) }
        coVerify(exactly = 0) { mockRepository.startNewCycle(any()) }
    }

    // --- onEndCurrentCycle Test ---

    @Test
    fun onEndCurrentCycle_WHEN_ongoingCycleExists_THEN_callsRepositoryEndCycle() = runTest {
        // ARRANGE
        val fakeOngoingCycle = Cycle(
            id = "ongoing-id",
            startDate = today.minus(5, DateTimeUnit.DAY),
            endDate = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        every { mockRepository.getAllCycles() } returns flowOf(listOf(fakeOngoingCycle))
        // Re-create ViewModel to pick up the new initial state
        viewModel = CycleViewModel(mockRepository, mockSymptomProvider, mockMedicationProvider)

        // ACT
        viewModel.onEndCurrentCycle()

        // ASSERT
        coVerify(exactly = 1) { mockRepository.endCycle(fakeOngoingCycle.id, today) }
    }
}