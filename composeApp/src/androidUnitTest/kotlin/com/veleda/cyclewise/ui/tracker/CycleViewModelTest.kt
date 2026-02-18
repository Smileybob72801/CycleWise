package com.veleda.cyclewise.ui.tracker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.veleda.cyclewise.domain.models.*
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.domain.usecases.AutoCloseOngoingPeriodUseCase
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.testutil.TestData
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

@OptIn(ExperimentalCoroutinesApi::class)
class CycleViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockRepository: PeriodRepository
    private lateinit var mockSymptomProvider: SymptomLibraryProvider
    private lateinit var mockMedicationProvider: MedicationLibraryProvider
    private lateinit var mockAutoCloseUseCase: AutoCloseOngoingPeriodUseCase
    private lateinit var mockAppSettings: AppSettings
    private lateinit var viewModel: TrackerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockRepository = mockk(relaxed = true)
        mockSymptomProvider = mockk(relaxed = true)
        mockMedicationProvider = mockk(relaxed = true)
        mockAutoCloseUseCase = mockk(relaxed = true)
        mockAppSettings = mockk(relaxed = true)

        every { mockRepository.observeDayDetails() } returns flowOf(emptyMap())
        every { mockRepository.getAllPeriods() } returns flowOf(emptyList())
        every { mockSymptomProvider.symptoms } returns flowOf(emptyList())
        every { mockMedicationProvider.medications } returns flowOf(emptyList())

        viewModel = TrackerViewModel(mockRepository, mockSymptomProvider, mockMedicationProvider, mockAutoCloseUseCase, mockAppSettings)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val today = TestData.DATE
    private val pastDate = TestData.DATE.minus(5, DateTimeUnit.DAY)

    @Test
    fun onEvent_DayTapped_WHEN_logExists_THEN_showsLogSheet() = runTest {
        // ARRANGE
        val fakeLog = mockk<FullDailyLog>()
        coEvery { mockRepository.getFullLogForDate(pastDate) } returns fakeLog

        // ACT
        viewModel.onEvent(TrackerEvent.DayTapped(pastDate))

        // ASSERT
        viewModel.uiState.test {
            assertEquals(fakeLog, awaitItem().logForSheet, "Log for sheet should be set")
        }
    }

    @Test
    fun onEvent_DayTapped_WHEN_noLogExists_THEN_emitsNavigateEffect() = runTest {
        // ARRANGE
        coEvery { mockRepository.getFullLogForDate(pastDate) } returns null

        // ACT & ASSERT
        viewModel.effect.test {
            viewModel.onEvent(TrackerEvent.DayTapped(pastDate))
            val effect = awaitItem()
            assertIs<TrackerEffect.NavigateToDailyLog>(effect)
            assertEquals(pastDate, effect.date)
        }
    }

    @Test
    fun onEvent_DeletePeriodConfirmed_THEN_callsRepositoryDeleteAndClearsState() = runTest {
        // ARRANGE
        val periodId = "period-to-delete-id"
        val fakePeriod = Period(periodId, pastDate, today, TestData.INSTANT, TestData.INSTANT)
        val fakeLog = FullDailyLog(DailyEntry("log-id", pastDate, 5, createdAt = TestData.INSTANT, updatedAt = TestData.INSTANT))

        every { mockRepository.getAllPeriods() } returns flowOf(listOf(fakePeriod))
        viewModel = TrackerViewModel(mockRepository, mockSymptomProvider, mockMedicationProvider, mockAutoCloseUseCase, mockAppSettings)
        advanceUntilIdle()

        coEvery { mockRepository.getFullLogForDate(pastDate) } returns fakeLog
        viewModel.onEvent(TrackerEvent.DayTapped(pastDate))
        advanceUntilIdle()

        viewModel.onEvent(TrackerEvent.DeletePeriodRequested(periodId))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.logForSheet)

        // ACT
        viewModel.onEvent(TrackerEvent.DeletePeriodConfirmed(periodId))
        advanceUntilIdle()

        // ASSERT
        coVerify(exactly = 1) { mockRepository.deletePeriod(periodId) }
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.periodIdToDelete)
    }

    @Test
    fun init_WHEN_dayDetailsHaveNotes_THEN_calendarDayInfoHasNotesTrue() = runTest {
        // GIVEN — repository emits day details where hasNotes is true
        val date = TestData.DATE
        val dayDetails = DayDetails(hasNotes = true)
        every { mockRepository.observeDayDetails() } returns flowOf(mapOf(date to dayDetails))

        // WHEN — a new ViewModel is created and collects the flow
        val vm = TrackerViewModel(mockRepository, mockSymptomProvider, mockMedicationProvider, mockAutoCloseUseCase, mockAppSettings)
        advanceUntilIdle()

        // THEN — the mapped CalendarDayInfo has hasNotes = true
        val mapped = vm.uiState.value.dayDetails[date]
        assertNotNull(mapped)
        assertTrue(mapped.hasNotes, "CalendarDayInfo.hasNotes should be true when DayDetails.hasNotes is true")
    }

    @Test
    fun onEvent_DeletePeriodDismissed_THEN_clearsConfirmationState() = runTest {
        // ARRANGE
        val periodId = "period-to-dismiss-id"
        viewModel.onEvent(TrackerEvent.DeletePeriodRequested(periodId))
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        // ACT
        viewModel.onEvent(TrackerEvent.DeletePeriodDismissed)

        // ASSERT
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.periodIdToDelete)
    }
}
