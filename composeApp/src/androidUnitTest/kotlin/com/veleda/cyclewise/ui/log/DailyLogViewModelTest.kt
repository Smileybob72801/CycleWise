package com.veleda.cyclewise.ui.log

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodLog
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.models.ArticleCategory
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.domain.providers.EducationalContentProvider
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

/**
 * Unit tests for [DailyLogViewModel].
 *
 * Covers isPeriodDay initialization from repository state, period toggle
 * dispatching to the repository, and auto-save behavior including note debounce.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyLogViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockRepository: PeriodRepository
    private lateinit var mockGetOrCreateDailyLog: GetOrCreateDailyLogUseCase
    private lateinit var mockSymptomProvider: SymptomLibraryProvider
    private lateinit var mockMedicationProvider: MedicationLibraryProvider
    private lateinit var mockEducationalContentProvider: EducationalContentProvider

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
        mockEducationalContentProvider = mockk(relaxed = true)

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
            educationalContentProvider = mockEducationalContentProvider,
        )
    }

    // ── Log loading tests ────────────────────────────────────────────

    @Test
    fun `init WHEN noPeriodsExistAndUseCaseReturnsLog THEN noErrorAndLogIsSet`() = runTest {
        // GIVEN — use case returns a log with dayInCycle = 0 (no parent period)
        val noPeriodEntry = DailyEntry(
            id = "entry-no-period",
            entryDate = testDate,
            dayInCycle = 0,
            createdAt = testInstant,
            updatedAt = testInstant,
        )
        val noPeriodLog = FullDailyLog(entry = noPeriodEntry)
        coEvery { mockGetOrCreateDailyLog(any()) } returns noPeriodLog

        // WHEN — ViewModel is created
        val vm = createViewModel()
        advanceUntilIdle()

        // THEN — error is null and log is set
        assertNull(vm.uiState.value.error, "error should be null even when no periods exist")
        assertNotNull(vm.uiState.value.log, "log should be non-null after loading")
        assertEquals(0, vm.uiState.value.log!!.entry.dayInCycle, "dayInCycle should be 0 for no parent period")
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

    @Test
    fun `onEvent PeriodToggled WHEN true THEN createsPeriodLogWithNullFlow`() = runTest {
        // GIVEN — ViewModel initialized with no period
        val vm = createViewModel()
        advanceUntilIdle()
        assertNull(vm.uiState.value.log!!.periodLog, "PeriodLog should be null before toggle")

        // WHEN — user toggles period ON
        vm.onEvent(DailyLogEvent.PeriodToggled(isOnPeriod = true))
        advanceUntilIdle()

        // THEN — a PeriodLog exists with null flowIntensity
        val periodLog = vm.uiState.value.log!!.periodLog
        assertNotNull(periodLog, "PeriodLog should be created when period is toggled ON")
        assertNull(periodLog.flowIntensity, "flowIntensity should be null for a newly toggled PeriodLog")
    }

    @Test
    fun `onEvent FlowIntensityChanged WHEN null THEN clearsFlowButKeepsPeriodLog`() = runTest {
        // GIVEN — ViewModel with a PeriodLog that has flow set
        val vm = createViewModel()
        advanceUntilIdle()

        // Toggle period ON to create a PeriodLog, then set flow
        vm.onEvent(DailyLogEvent.PeriodToggled(isOnPeriod = true))
        advanceUntilIdle()
        vm.onEvent(DailyLogEvent.FlowIntensityChanged(intensity = FlowIntensity.HEAVY))
        advanceUntilIdle()
        assertEquals(FlowIntensity.HEAVY, vm.uiState.value.log!!.periodLog!!.flowIntensity)

        // WHEN — flow intensity is deselected (set to null)
        vm.onEvent(DailyLogEvent.FlowIntensityChanged(intensity = null))
        advanceUntilIdle()

        // THEN — PeriodLog still exists but flow is null
        val periodLog = vm.uiState.value.log!!.periodLog
        assertNotNull(periodLog, "PeriodLog should still exist after clearing flow")
        assertNull(periodLog.flowIntensity, "flowIntensity should be null after deselection")
    }

    @Test
    fun `onEvent PeriodColorChanged WHEN periodLogExists THEN updatesColor`() = runTest {
        // GIVEN — ViewModel with a PeriodLog (no flow selected yet)
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onEvent(DailyLogEvent.PeriodToggled(isOnPeriod = true))
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.log!!.periodLog)

        // WHEN — user selects a period color (without having selected flow first)
        vm.onEvent(DailyLogEvent.PeriodColorChanged(color = PeriodColor.DARK_RED))
        advanceUntilIdle()

        // THEN — color is saved on the existing PeriodLog
        val periodLog = vm.uiState.value.log!!.periodLog
        assertNotNull(periodLog, "PeriodLog should still exist")
        assertEquals(PeriodColor.DARK_RED, periodLog.periodColor)
        assertNull(periodLog.flowIntensity, "flowIntensity should still be null")
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

    // ── Educational sheet tests ──────────────────────────────────────

    private val testArticle = EducationalArticle(
        id = "test-article",
        title = "Test",
        body = "Body",
        category = ArticleCategory.CYCLE_BASICS,
        contentTags = listOf("Mood"),
        sourceName = "Test Source",
        sourceUrl = "https://example.com",
        sortOrder = 1,
    )

    @Test
    fun `onEvent ShowEducationalSheet WHEN tagHasArticles THEN educationalArticlesPopulated`() = runTest {
        // GIVEN
        every { mockEducationalContentProvider.getByTag("Mood") } returns listOf(testArticle)
        val vm = createViewModel()
        advanceUntilIdle()

        // WHEN
        vm.onEvent(DailyLogEvent.ShowEducationalSheet("Mood"))
        advanceUntilIdle()

        // THEN
        assertNotNull(vm.uiState.value.educationalArticles)
        assertEquals(1, vm.uiState.value.educationalArticles!!.size)
    }

    @Test
    fun `onEvent ShowEducationalSheet WHEN tagHasNoArticles THEN educationalArticlesRemainsNull`() = runTest {
        // GIVEN
        every { mockEducationalContentProvider.getByTag("NonExistent") } returns emptyList()
        val vm = createViewModel()
        advanceUntilIdle()

        // WHEN
        vm.onEvent(DailyLogEvent.ShowEducationalSheet("NonExistent"))
        advanceUntilIdle()

        // THEN
        assertNull(vm.uiState.value.educationalArticles)
    }

    @Test
    fun `onEvent DismissEducationalSheet WHEN shown THEN educationalArticlesNull`() = runTest {
        // GIVEN
        every { mockEducationalContentProvider.getByTag("Mood") } returns listOf(testArticle)
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onEvent(DailyLogEvent.ShowEducationalSheet("Mood"))
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.educationalArticles)

        // WHEN
        vm.onEvent(DailyLogEvent.DismissEducationalSheet)
        advanceUntilIdle()

        // THEN
        assertNull(vm.uiState.value.educationalArticles)
    }
}
