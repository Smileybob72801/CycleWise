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
import com.veleda.cyclewise.domain.usecases.DeleteMedicationUseCase
import com.veleda.cyclewise.domain.usecases.DeleteSymptomUseCase
import com.veleda.cyclewise.domain.usecases.GetOrCreateDailyLogUseCase
import com.veleda.cyclewise.domain.usecases.RenameMedicationUseCase
import com.veleda.cyclewise.domain.usecases.RenameResult
import com.veleda.cyclewise.domain.usecases.RenameSymptomUseCase
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildMedication
import com.veleda.cyclewise.testutil.buildMedicationLog
import com.veleda.cyclewise.testutil.buildSymptom
import com.veleda.cyclewise.testutil.buildSymptomLog
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.mockk
import io.mockk.unmockkStatic
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
 * dispatching to the repository, auto-save behavior including note debounce,
 * and symptom/medication library edit and delete operations.
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
    private lateinit var mockRenameSymptomUseCase: RenameSymptomUseCase
    private lateinit var mockDeleteSymptomUseCase: DeleteSymptomUseCase
    private lateinit var mockRenameMedicationUseCase: RenameMedicationUseCase
    private lateinit var mockDeleteMedicationUseCase: DeleteMedicationUseCase

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
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any()) } returns 0

        mockRepository = mockk(relaxed = true)
        mockGetOrCreateDailyLog = mockk(relaxed = true)
        mockSymptomProvider = mockk(relaxed = true)
        mockMedicationProvider = mockk(relaxed = true)
        mockEducationalContentProvider = mockk(relaxed = true)
        mockRenameSymptomUseCase = mockk(relaxed = true)
        mockDeleteSymptomUseCase = mockk(relaxed = true)
        mockRenameMedicationUseCase = mockk(relaxed = true)
        mockDeleteMedicationUseCase = mockk(relaxed = true)

        every { mockSymptomProvider.symptoms } returns flowOf(emptyList())
        every { mockMedicationProvider.medications } returns flowOf(emptyList())
        every { mockRepository.getAllPeriods() } returns flowOf(emptyList())
        coEvery { mockGetOrCreateDailyLog(any()) } returns testLog
        coEvery { mockRepository.getWaterIntakeForDates(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    private fun createViewModel(entryDate: LocalDate = testDate): DailyLogViewModel {
        return DailyLogViewModel(
            entryDate = entryDate,
            periodRepository = mockRepository,
            getOrCreateDailyLog = mockGetOrCreateDailyLog,
            symptomLibraryProvider = mockSymptomProvider,
            medicationLibraryProvider = mockMedicationProvider,
            educationalContentProvider = mockEducationalContentProvider,
            renameSymptomUseCase = mockRenameSymptomUseCase,
            deleteSymptomUseCase = mockDeleteSymptomUseCase,
            renameMedicationUseCase = mockRenameMedicationUseCase,
            deleteMedicationUseCase = mockDeleteMedicationUseCase,
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

    // ── Error handling tests ──────────────────────────────────────────

    @Test
    fun `onEvent CreateAndAddSymptom WHEN repositoryThrows THEN errorMessageIsEmitted`() = runTest {
        // GIVEN — repository throws when creating a symptom
        coEvery { mockRepository.createOrGetSymptomInLibrary(any(), any()) } throws RuntimeException("DB error")
        val vm = createViewModel()
        advanceUntilIdle()

        // WHEN — user creates a new symptom
        vm.onEvent(DailyLogEvent.CreateAndAddSymptom("Headache"))
        advanceUntilIdle()

        // THEN — errorMessage is set
        assertNotNull(vm.uiState.value.errorMessage, "errorMessage should be non-null when symptom save fails")
        assertTrue(
            vm.uiState.value.errorMessage!!.contains("symptom", ignoreCase = true),
            "errorMessage should mention symptom"
        )
    }

    @Test
    fun `onEvent MedicationCreatedAndAdded WHEN repositoryThrows THEN errorMessageIsEmitted`() = runTest {
        // GIVEN — repository throws when creating a medication
        coEvery { mockRepository.createOrGetMedicationInLibrary(any()) } throws RuntimeException("DB error")
        val vm = createViewModel()
        advanceUntilIdle()

        // WHEN — user creates a new medication
        vm.onEvent(DailyLogEvent.MedicationCreatedAndAdded("Ibuprofen"))
        advanceUntilIdle()

        // THEN — errorMessage is set
        assertNotNull(vm.uiState.value.errorMessage, "errorMessage should be non-null when medication save fails")
        assertTrue(
            vm.uiState.value.errorMessage!!.contains("medication", ignoreCase = true),
            "errorMessage should mention medication"
        )
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

    // ── Symptom Library Edit/Delete tests ─────────────────────────────

    @Test
    fun `onEvent SymptomLongPressed THEN setsSymptomForContextMenu`() = runTest {
        val symptom = buildSymptom(id = "s1", name = "Headache")
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(DailyLogEvent.SymptomLongPressed(symptom))
        advanceUntilIdle()

        assertEquals(symptom, vm.uiState.value.symptomForContextMenu)
    }

    @Test
    fun `onEvent SymptomEditDismissed THEN clearsAllSymptomEditState`() = runTest {
        val symptom = buildSymptom(id = "s1", name = "Headache")
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(DailyLogEvent.SymptomLongPressed(symptom))
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.symptomForContextMenu)

        vm.onEvent(DailyLogEvent.SymptomEditDismissed)
        advanceUntilIdle()

        assertNull(vm.uiState.value.symptomForContextMenu)
        assertNull(vm.uiState.value.symptomRenaming)
        assertNull(vm.uiState.value.symptomToDelete)
        assertNull(vm.uiState.value.renameError)
    }

    @Test
    fun `onEvent RenameSymptomConfirmed WHEN success THEN clearsSymptomRenaming`() = runTest {
        coEvery { mockRenameSymptomUseCase(any(), any(), any()) } returns RenameResult.Success
        val vm = createViewModel()
        advanceUntilIdle()

        val symptom = buildSymptom(id = "s1", name = "Headache")
        vm.onEvent(DailyLogEvent.RenameSymptomClicked(symptom))
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.symptomRenaming)

        vm.onEvent(DailyLogEvent.RenameSymptomConfirmed("s1", "Migraine"))
        advanceUntilIdle()

        assertNull(vm.uiState.value.symptomRenaming)
        assertNull(vm.uiState.value.renameError)
    }

    @Test
    fun `onEvent RenameSymptomConfirmed WHEN nameExists THEN setsRenameError`() = runTest {
        coEvery { mockRenameSymptomUseCase(any(), any(), any()) } returns RenameResult.NameAlreadyExists
        val vm = createViewModel()
        advanceUntilIdle()

        val symptom = buildSymptom(id = "s1", name = "Headache")
        vm.onEvent(DailyLogEvent.RenameSymptomClicked(symptom))
        advanceUntilIdle()

        vm.onEvent(DailyLogEvent.RenameSymptomConfirmed("s1", "Cramps"))
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.renameError)
        assertNotNull(vm.uiState.value.symptomRenaming, "dialog should remain open on error")
    }

    @Test
    fun `onEvent DeleteSymptomClicked THEN fetchesCountAndSetsSymptomToDelete`() = runTest {
        coEvery { mockDeleteSymptomUseCase.getLogCount("s1") } returns 5
        val symptom = buildSymptom(id = "s1", name = "Headache")
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(DailyLogEvent.DeleteSymptomClicked(symptom))
        advanceUntilIdle()

        assertEquals(symptom, vm.uiState.value.symptomToDelete)
        assertEquals(5, vm.uiState.value.symptomDeleteLogCount)
        assertNull(vm.uiState.value.symptomForContextMenu, "context menu should be cleared")
    }

    @Test
    fun `onEvent DeleteSymptomConfirmed THEN callsDeleteAndClearsStateAndFiltersLogs`() = runTest {
        val symptom = buildSymptom(id = "s1", name = "Headache")
        val symptomLog = buildSymptomLog(symptomId = "s1")
        val logWithSymptom = testLog.copy(symptomLogs = listOf(symptomLog))
        coEvery { mockGetOrCreateDailyLog(any()) } returns logWithSymptom

        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.log!!.symptomLogs.size)

        vm.onEvent(DailyLogEvent.DeleteSymptomConfirmed("s1"))
        advanceUntilIdle()

        coVerify(exactly = 1) { mockDeleteSymptomUseCase("s1") }
        assertNull(vm.uiState.value.symptomToDelete)
        assertEquals(0, vm.uiState.value.log!!.symptomLogs.size, "deleted symptom logs should be filtered out")
    }

    // ── Medication Library Edit/Delete tests ──────────────────────────

    @Test
    fun `onEvent MedicationLongPressed THEN setsMedicationForContextMenu`() = runTest {
        val medication = buildMedication(id = "m1", name = "Ibuprofen")
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(DailyLogEvent.MedicationLongPressed(medication))
        advanceUntilIdle()

        assertEquals(medication, vm.uiState.value.medicationForContextMenu)
    }

    @Test
    fun `onEvent MedicationEditDismissed THEN clearsAllMedicationEditState`() = runTest {
        val medication = buildMedication(id = "m1", name = "Ibuprofen")
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(DailyLogEvent.MedicationLongPressed(medication))
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.medicationForContextMenu)

        vm.onEvent(DailyLogEvent.MedicationEditDismissed)
        advanceUntilIdle()

        assertNull(vm.uiState.value.medicationForContextMenu)
        assertNull(vm.uiState.value.medicationRenaming)
        assertNull(vm.uiState.value.medicationToDelete)
        assertNull(vm.uiState.value.renameError)
    }

    @Test
    fun `onEvent RenameMedicationConfirmed WHEN success THEN clearsMedicationRenaming`() = runTest {
        coEvery { mockRenameMedicationUseCase(any(), any(), any()) } returns RenameResult.Success
        val vm = createViewModel()
        advanceUntilIdle()

        val medication = buildMedication(id = "m1", name = "Ibuprofen")
        vm.onEvent(DailyLogEvent.RenameMedicationClicked(medication))
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.medicationRenaming)

        vm.onEvent(DailyLogEvent.RenameMedicationConfirmed("m1", "Naproxen"))
        advanceUntilIdle()

        assertNull(vm.uiState.value.medicationRenaming)
        assertNull(vm.uiState.value.renameError)
    }

    @Test
    fun `onEvent RenameMedicationConfirmed WHEN nameExists THEN setsRenameError`() = runTest {
        coEvery { mockRenameMedicationUseCase(any(), any(), any()) } returns RenameResult.NameAlreadyExists
        val vm = createViewModel()
        advanceUntilIdle()

        val medication = buildMedication(id = "m1", name = "Ibuprofen")
        vm.onEvent(DailyLogEvent.RenameMedicationClicked(medication))
        advanceUntilIdle()

        vm.onEvent(DailyLogEvent.RenameMedicationConfirmed("m1", "Aspirin"))
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.renameError)
        assertNotNull(vm.uiState.value.medicationRenaming, "dialog should remain open on error")
    }

    @Test
    fun `onEvent DeleteMedicationClicked THEN fetchesCountAndSetsMedicationToDelete`() = runTest {
        coEvery { mockDeleteMedicationUseCase.getLogCount("m1") } returns 3
        val medication = buildMedication(id = "m1", name = "Ibuprofen")
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(DailyLogEvent.DeleteMedicationClicked(medication))
        advanceUntilIdle()

        assertEquals(medication, vm.uiState.value.medicationToDelete)
        assertEquals(3, vm.uiState.value.medicationDeleteLogCount)
        assertNull(vm.uiState.value.medicationForContextMenu, "context menu should be cleared")
    }

    @Test
    fun `onEvent DeleteMedicationConfirmed THEN callsDeleteAndClearsStateAndFiltersLogs`() = runTest {
        val medication = buildMedication(id = "m1", name = "Ibuprofen")
        val medicationLog = buildMedicationLog(medicationId = "m1")
        val logWithMedication = testLog.copy(medicationLogs = listOf(medicationLog))
        coEvery { mockGetOrCreateDailyLog(any()) } returns logWithMedication

        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.log!!.medicationLogs.size)

        vm.onEvent(DailyLogEvent.DeleteMedicationConfirmed("m1"))
        advanceUntilIdle()

        coVerify(exactly = 1) { mockDeleteMedicationUseCase("m1") }
        assertNull(vm.uiState.value.medicationToDelete)
        assertEquals(0, vm.uiState.value.log!!.medicationLogs.size, "deleted medication logs should be filtered out")
    }
}
