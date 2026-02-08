package com.veleda.cyclewise.ui.tracker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.veleda.cyclewise.domain.models.*
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.domain.usecases.AutoCloseOngoingPeriodUseCase
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
    private lateinit var mockAutoCloseUseCase: AutoCloseOngoingPeriodUseCase
    private lateinit var viewModel: TrackerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockRepository = mockk(relaxed = true)
        mockSymptomProvider = mockk(relaxed = true)
        mockMedicationProvider = mockk(relaxed = true)
        mockAutoCloseUseCase = mockk(relaxed = true)

        // Mock initial data flows
        every { mockRepository.observeDayDetails() } returns flowOf(emptyMap())
        every { mockRepository.getAllPeriods() } returns flowOf(emptyList())
        every { mockSymptomProvider.symptoms } returns flowOf(emptyList())
        every { mockMedicationProvider.medications } returns flowOf(emptyList())

        viewModel = TrackerViewModel(mockRepository, mockSymptomProvider, mockMedicationProvider, mockAutoCloseUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val pastDate = today.minus(5, DateTimeUnit.DAY)

    @Test
    fun onEvent_DayTapped_WHEN_logExists_THEN_showsLogSheet() = runTest {
        val fakeLog = mockk<FullDailyLog>()
        coEvery { mockRepository.getFullLogForDate(pastDate) } returns fakeLog

        viewModel.onEvent(TrackerEvent.DayTapped(pastDate))

        viewModel.uiState.test {
            assertEquals(fakeLog, awaitItem().logForSheet, "Log for sheet should be set")
        }
    }

    @Test
    fun onEvent_DayTapped_WHEN_noLogExists_THEN_emitsNavigateEffect() = runTest {
        coEvery { mockRepository.getFullLogForDate(pastDate) } returns null

        viewModel.effect.test {
            viewModel.onEvent(TrackerEvent.DayTapped(pastDate))
            val effect = awaitItem()
            assertIs<TrackerEffect.NavigateToDailyLog>(effect)
            assertEquals(pastDate, effect.date)
        }
    }

    @Test
    fun onEvent_DeletePeriodConfirmed_THEN_callsRepositoryDeleteAndClearsState() = runTest {
        val periodId = "period-to-delete-id"
        val fakePeriod = Period(periodId, pastDate, today, Clock.System.now(), Clock.System.now())
        val fakeLog = FullDailyLog(DailyEntry("log-id", pastDate, 5, createdAt = Clock.System.now(), updatedAt = Clock.System.now()))

        // Simulate the sheet being open for the log that is part of this period
        every { mockRepository.getAllPeriods() } returns flowOf(listOf(fakePeriod))
        viewModel = TrackerViewModel(mockRepository, mockSymptomProvider, mockMedicationProvider, mockAutoCloseUseCase)
        advanceUntilIdle()

        coEvery { mockRepository.getFullLogForDate(pastDate) } returns fakeLog
        viewModel.onEvent(TrackerEvent.DayTapped(pastDate))
        advanceUntilIdle()

        // Request deletion (shows confirmation dialog, clears sheet)
        viewModel.onEvent(TrackerEvent.DeletePeriodRequested(periodId))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.logForSheet)

        // Confirm deletion
        viewModel.onEvent(TrackerEvent.DeletePeriodConfirmed(periodId))
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.deletePeriod(periodId) }
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.periodIdToDelete)
    }

    @Test
    fun onEvent_DeletePeriodDismissed_THEN_clearsConfirmationState() = runTest {
        val periodId = "period-to-dismiss-id"

        viewModel.onEvent(TrackerEvent.DeletePeriodRequested(periodId))
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.onEvent(TrackerEvent.DeletePeriodDismissed)

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.periodIdToDelete)
    }
}
