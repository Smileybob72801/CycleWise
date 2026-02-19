package com.veleda.cyclewise.ui.log

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.domain.usecases.GetOrCreateDailyLogUseCase
import com.veleda.cyclewise.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DailyLogViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockRepository: PeriodRepository
    private lateinit var mockGetOrCreateDailyLog: GetOrCreateDailyLogUseCase
    private lateinit var mockSymptomProvider: SymptomLibraryProvider
    private lateinit var mockMedicationProvider: MedicationLibraryProvider

    private val testDate = TestData.DATE
    private val testInstant = TestData.INSTANT

    private val testEntry = DailyEntry(
        id = "entry-1",
        entryDate = testDate,
        dayInCycle = 1,
        createdAt = testInstant,
        updatedAt = testInstant,
    )
    private val testLog = FullDailyLog(entry = testEntry)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockRepository = mockk(relaxed = true)
        mockGetOrCreateDailyLog = mockk(relaxed = true)
        mockSymptomProvider = mockk(relaxed = true)
        mockMedicationProvider = mockk(relaxed = true)

        every { mockSymptomProvider.symptoms } returns flowOf(emptyList())
        every { mockMedicationProvider.medications } returns flowOf(emptyList())
        every { mockRepository.getAllPeriods() } returns flowOf(emptyList())
        coEvery { mockGetOrCreateDailyLog(any()) } returns testLog
        coEvery { mockRepository.getWaterIntakeForDates(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(entryDate: LocalDate = testDate): DailyLogViewModel {
        return DailyLogViewModel(
            entryDate = entryDate,
            periodRepository = mockRepository,
            getOrCreateDailyLog = mockGetOrCreateDailyLog,
            symptomLibraryProvider = mockSymptomProvider,
            medicationLibraryProvider = mockMedicationProvider,
        )
    }

    // ── isPeriodDay init tests ───────────────────────────────────────

    @Test
    fun `init WHEN dateIsWithinPeriod THEN isPeriodDayTrue`() = runTest {
        // GIVEN — a period covers the test date
        val period = Period(
            id = "period-1",
            startDate = testDate.minus(2, DateTimeUnit.DAY),
            endDate = testDate.plus(2, DateTimeUnit.DAY),
            createdAt = testInstant,
            updatedAt = testInstant,
        )
        every { mockRepository.getAllPeriods() } returns flowOf(listOf(period))

        // WHEN — ViewModel is created
        val vm = createViewModel()
        advanceUntilIdle()

        // THEN — isPeriodDay is true
        assertTrue(vm.uiState.value.isPeriodDay, "isPeriodDay should be true when date falls within a period")
    }

    @Test
    fun `init WHEN dateIsNotWithinPeriod THEN isPeriodDayFalse`() = runTest {
        // GIVEN — no periods cover the test date
        every { mockRepository.getAllPeriods() } returns flowOf(emptyList())

        // WHEN — ViewModel is created
        val vm = createViewModel()
        advanceUntilIdle()

        // THEN — isPeriodDay is false
        assertFalse(vm.uiState.value.isPeriodDay, "isPeriodDay should be false when no period covers the date")
    }

    @Test
    fun `init WHEN dateIsWithinOngoingPeriod THEN isPeriodDayTrue`() = runTest {
        // GIVEN — an ongoing period (null endDate) that started before the test date
        val ongoingPeriod = Period(
            id = "period-ongoing",
            startDate = testDate.minus(3, DateTimeUnit.DAY),
            endDate = null,
            createdAt = testInstant,
            updatedAt = testInstant,
        )
        every { mockRepository.getAllPeriods() } returns flowOf(listOf(ongoingPeriod))

        // WHEN — ViewModel is created
        val vm = createViewModel()
        advanceUntilIdle()

        // THEN — isPeriodDay is true (null endDate means ongoing)
        assertTrue(vm.uiState.value.isPeriodDay, "isPeriodDay should be true for dates within an ongoing period")
    }

    // ── PeriodToggled tests ──────────────────────────────────────────

    @Test
    fun `onEvent PeriodToggled WHEN onTrue THEN callsLogPeriodDayAndUpdatesState`() = runTest {
        // GIVEN — ViewModel is initialized with no period
        val vm = createViewModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isPeriodDay)

        // WHEN — user toggles period ON
        vm.onEvent(DailyLogEvent.PeriodToggled(isOnPeriod = true))
        advanceUntilIdle()

        // THEN — logPeriodDay was called and state updated
        coVerify(atLeast = 1) { mockRepository.logPeriodDay(testDate) }
        assertTrue(vm.uiState.value.isPeriodDay, "isPeriodDay should be true after toggling ON")
    }

    @Test
    fun `onEvent PeriodToggled WHEN onFalse THEN callsUnLogPeriodDayAndUpdatesState`() = runTest {
        // GIVEN — ViewModel is initialized with a period covering the date
        val period = Period(
            id = "period-1",
            startDate = testDate.minus(2, DateTimeUnit.DAY),
            endDate = null,
            createdAt = testInstant,
            updatedAt = testInstant,
        )
        every { mockRepository.getAllPeriods() } returns flowOf(listOf(period))

        val vm = createViewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isPeriodDay)

        // WHEN — user toggles period OFF
        vm.onEvent(DailyLogEvent.PeriodToggled(isOnPeriod = false))
        advanceUntilIdle()

        // THEN — unLogPeriodDay was called and state updated
        coVerify(atLeast = 1) { mockRepository.unLogPeriodDay(testDate) }
        assertFalse(vm.uiState.value.isPeriodDay, "isPeriodDay should be false after toggling OFF")
    }

    @Test
    fun `onEvent PeriodToggled WHEN onTrueAndPeriodAlreadyLogged THEN stillSetsPeriodDayTrue`() = runTest {
        // GIVEN — ViewModel initialized, logPeriodDay is a no-op for already-logged dates
        val vm = createViewModel()
        advanceUntilIdle()

        // WHEN — user toggles period ON
        vm.onEvent(DailyLogEvent.PeriodToggled(isOnPeriod = true))
        advanceUntilIdle()

        // THEN — isPeriodDay is true and logPeriodDay was called
        assertTrue(vm.uiState.value.isPeriodDay, "isPeriodDay should be true even when date is already logged")
        coVerify(atLeast = 1) { mockRepository.logPeriodDay(testDate) }
    }

    // ── Auto-save tests ──────────────────────────────────────────────

    @Test
    fun `onEvent MoodScoreChanged THEN autoSavesLog`() = runTest {
        // GIVEN — ViewModel with a loaded log
        val vm = createViewModel()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.log)

        // WHEN — mood is changed
        vm.onEvent(DailyLogEvent.MoodScoreChanged(score = 4))
        advanceUntilIdle()

        // THEN — saveFullLog is called (auto-save)
        coVerify(atLeast = 1) { mockRepository.saveFullLog(any()) }
    }

    @Test
    fun `onEvent NoteChanged THEN debouncesAutoSave`() = runTest {
        // GIVEN — ViewModel with a loaded log
        val vm = createViewModel()
        advanceUntilIdle()

        // WHEN — note is changed
        vm.onEvent(DailyLogEvent.NoteChanged(text = "Test note"))

        // THEN — saveFullLog is NOT called immediately (debounced)
        coVerify(exactly = 0) { mockRepository.saveFullLog(any()) }

        // WHEN — debounce delay passes
        advanceTimeBy(DailyLogViewModel.NOTE_DEBOUNCE_MS + 100)
        advanceUntilIdle()

        // THEN — saveFullLog is called after debounce
        coVerify(atLeast = 1) { mockRepository.saveFullLog(any()) }
    }
}
