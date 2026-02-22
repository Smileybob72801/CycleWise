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
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun onEvent_PeriodMarkDay_WHEN_singleDayOngoingPeriodUnlogged_THEN_ongoingPeriodBecomesNull() = runTest {
        // GIVEN — a single-day ongoing period exists for today (no endDate),
        // backed by a mutable flow so the ViewModel's collector sees updates.
        val ongoingPeriod = Period(
            id = "ongoing-1",
            startDate = today,
            endDate = null,
            createdAt = TestData.INSTANT,
            updatedAt = TestData.INSTANT,
        )
        val periodsFlow = MutableStateFlow(listOf(ongoingPeriod))
        every { mockRepository.getAllPeriods() } returns periodsFlow

        val vm = TrackerViewModel(
            mockRepository, mockSymptomProvider, mockMedicationProvider,
            mockAutoCloseUseCase, mockAppSettings
        )
        advanceUntilIdle()

        // Verify precondition — ongoingPeriod is non-null
        assertNotNull(vm.uiState.value.ongoingPeriod, "ongoingPeriod should be non-null before unlog")

        // Simulate repository removing the period after unlog
        coEvery { mockRepository.unLogPeriodDay(today) } coAnswers {
            periodsFlow.value = emptyList()
        }

        // WHEN — user long-presses to unmark the period day
        vm.onEvent(TrackerEvent.PeriodMarkDay(today))
        advanceUntilIdle()

        // THEN — unLogPeriodDay was called and ongoingPeriod is null.
        // This transition drives the AnimatedVisibility exit animation in
        // TrackerScreen. The UI must handle ongoingPeriod being null during
        // the exit animation (no !! operator).
        coVerify(exactly = 1) { mockRepository.unLogPeriodDay(today) }
        assertNull(vm.uiState.value.ongoingPeriod, "ongoingPeriod should be null after unlogging the only period day")
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

    // --- PeriodRangeDragged tests ---

    /**
     * Creates a [TrackerViewModel] pre-loaded with the given [periods] list.
     */
    private fun viewModelWithPeriods(periods: List<Period>): TrackerViewModel {
        every { mockRepository.getAllPeriods() } returns flowOf(periods)
        return TrackerViewModel(
            mockRepository, mockSymptomProvider, mockMedicationProvider,
            mockAutoCloseUseCase, mockAppSettings
        )
    }

    @Test
    fun onEvent_PeriodRangeDragged_WHEN_noPeriodExists_THEN_logsAllDaysInRange() = runTest {
        // GIVEN — no existing periods
        val vm = viewModelWithPeriods(emptyList())
        advanceUntilIdle()

        val anchor = LocalDate(2025, 6, 10)
        val release = LocalDate(2025, 6, 13)

        // WHEN
        vm.onEvent(TrackerEvent.PeriodRangeDragged(anchor, release))
        advanceUntilIdle()

        // THEN — logPeriodDay called for June 10, 11, 12, 13
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 10)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 11)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 12)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 13)) }
    }

    @Test
    fun onEvent_PeriodRangeDragged_WHEN_dragReversed_THEN_logsAllDaysInRange() = runTest {
        // GIVEN — no existing periods, drag from later to earlier date
        val vm = viewModelWithPeriods(emptyList())
        advanceUntilIdle()

        val anchor = LocalDate(2025, 6, 13)
        val release = LocalDate(2025, 6, 10)

        // WHEN
        vm.onEvent(TrackerEvent.PeriodRangeDragged(anchor, release))
        advanceUntilIdle()

        // THEN — logPeriodDay called for June 10, 11, 12, 13
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 10)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 11)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 12)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 13)) }
    }

    @Test
    fun onEvent_PeriodRangeDragged_WHEN_anchorIsStartDate_THEN_shrinksFromStart() = runTest {
        // GIVEN — period exists June 10–15, anchor at start
        val period = Period("p1", LocalDate(2025, 6, 10), LocalDate(2025, 6, 15), TestData.INSTANT, TestData.INSTANT)
        val vm = viewModelWithPeriods(listOf(period))
        advanceUntilIdle()

        val anchor = LocalDate(2025, 6, 10) // start of period
        val release = LocalDate(2025, 6, 12)

        // WHEN
        vm.onEvent(TrackerEvent.PeriodRangeDragged(anchor, release))
        advanceUntilIdle()

        // THEN — unLogPeriodDay called for June 10, 11 (not 12)
        coVerify(exactly = 1) { mockRepository.unLogPeriodDay(LocalDate(2025, 6, 10)) }
        coVerify(exactly = 1) { mockRepository.unLogPeriodDay(LocalDate(2025, 6, 11)) }
        coVerify(exactly = 0) { mockRepository.unLogPeriodDay(LocalDate(2025, 6, 12)) }
    }

    @Test
    fun onEvent_PeriodRangeDragged_WHEN_anchorIsEndDate_THEN_shrinksFromEnd() = runTest {
        // GIVEN — period exists June 10–15, anchor at end
        val period = Period("p1", LocalDate(2025, 6, 10), LocalDate(2025, 6, 15), TestData.INSTANT, TestData.INSTANT)
        val vm = viewModelWithPeriods(listOf(period))
        advanceUntilIdle()

        val anchor = LocalDate(2025, 6, 15) // end of period
        val release = LocalDate(2025, 6, 13)

        // WHEN
        vm.onEvent(TrackerEvent.PeriodRangeDragged(anchor, release))
        advanceUntilIdle()

        // THEN — unLogPeriodDay called for June 15, 14 (not 13)
        coVerify(exactly = 1) { mockRepository.unLogPeriodDay(LocalDate(2025, 6, 15)) }
        coVerify(exactly = 1) { mockRepository.unLogPeriodDay(LocalDate(2025, 6, 14)) }
        coVerify(exactly = 0) { mockRepository.unLogPeriodDay(LocalDate(2025, 6, 13)) }
    }

    @Test
    fun onEvent_PeriodRangeDragged_WHEN_singleDayPeriodDraggedLater_THEN_marksRange() = runTest {
        // GIVEN — single-day period June 10 (start == end)
        val period = Period("p1", LocalDate(2025, 6, 10), LocalDate(2025, 6, 10), TestData.INSTANT, TestData.INSTANT)
        val vm = viewModelWithPeriods(listOf(period))
        advanceUntilIdle()

        val anchor = LocalDate(2025, 6, 10)
        val release = LocalDate(2025, 6, 12)

        // WHEN — drag later from a single-day period
        vm.onEvent(TrackerEvent.PeriodRangeDragged(anchor, release))
        advanceUntilIdle()

        // THEN — falls to default mark; logs June 10, 11, 12
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 10)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 11)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 12)) }
    }

    @Test
    fun onEvent_PeriodRangeDragged_WHEN_singleDayPeriodDraggedEarlier_THEN_marksRange() = runTest {
        // GIVEN — single-day period June 10
        val period = Period("p1", LocalDate(2025, 6, 10), LocalDate(2025, 6, 10), TestData.INSTANT, TestData.INSTANT)
        val vm = viewModelWithPeriods(listOf(period))
        advanceUntilIdle()

        val anchor = LocalDate(2025, 6, 10)
        val release = LocalDate(2025, 6, 8)

        // WHEN — drag earlier from a single-day period
        vm.onEvent(TrackerEvent.PeriodRangeDragged(anchor, release))
        advanceUntilIdle()

        // THEN — falls to default mark; logs June 8, 9, 10
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 8)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 9)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 10)) }
    }

    @Test
    fun onEvent_PeriodRangeDragged_WHEN_anchorIsStartAndDragOutward_THEN_marksRange() = runTest {
        // GIVEN — period June 10–15, anchor at start, release before start (outward)
        val period = Period("p1", LocalDate(2025, 6, 10), LocalDate(2025, 6, 15), TestData.INSTANT, TestData.INSTANT)
        val vm = viewModelWithPeriods(listOf(period))
        advanceUntilIdle()

        val anchor = LocalDate(2025, 6, 10)
        val release = LocalDate(2025, 6, 7)

        // WHEN — drag outward from start (earlier)
        vm.onEvent(TrackerEvent.PeriodRangeDragged(anchor, release))
        advanceUntilIdle()

        // THEN — does not match shrink (release < anchor fails shrink-from-start guard)
        // Falls to default: logs June 7, 8, 9, 10
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 7)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 8)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 9)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 10)) }
    }

    @Test
    fun onEvent_PeriodRangeDragged_WHEN_anchorIsEndAndDragOutward_THEN_marksRange() = runTest {
        // GIVEN — period June 10–15, anchor at end, release after end (outward)
        val period = Period("p1", LocalDate(2025, 6, 10), LocalDate(2025, 6, 15), TestData.INSTANT, TestData.INSTANT)
        val vm = viewModelWithPeriods(listOf(period))
        advanceUntilIdle()

        val anchor = LocalDate(2025, 6, 15)
        val release = LocalDate(2025, 6, 18)

        // WHEN — drag outward from end (later)
        vm.onEvent(TrackerEvent.PeriodRangeDragged(anchor, release))
        advanceUntilIdle()

        // THEN — does not match shrink (release > anchor fails shrink-from-end guard)
        // Falls to default: logs June 15, 16, 17, 18
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 15)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 16)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 17)) }
        coVerify(exactly = 1) { mockRepository.logPeriodDay(LocalDate(2025, 6, 18)) }
    }
}
