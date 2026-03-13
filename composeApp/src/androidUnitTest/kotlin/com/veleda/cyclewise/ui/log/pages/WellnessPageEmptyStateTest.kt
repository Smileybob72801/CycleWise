package com.veleda.cyclewise.ui.log.pages

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.veleda.cyclewise.domain.models.DailyEntry
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.providers.EducationalContentProvider
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.domain.usecases.DeleteMedicationUseCase
import com.veleda.cyclewise.domain.usecases.DeleteSymptomUseCase
import com.veleda.cyclewise.domain.usecases.GetOrCreateDailyLogUseCase
import com.veleda.cyclewise.domain.usecases.RenameMedicationUseCase
import com.veleda.cyclewise.domain.usecases.RenameSymptomUseCase
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.ui.log.DailyLogEvent
import com.veleda.cyclewise.ui.log.DailyLogViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.*

/**
 * Tests the conditions that drive the WellnessPage empty-state prompt visibility.
 *
 * The prompt is visible when `moodScore == null && energyLevel == null &&
 * libidoScore == null && waterCups == 0` and hidden otherwise.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WellnessPageEmptyStateTest {

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

    private val emptyEntry = DailyEntry(
        id = "entry-1",
        entryDate = testDate,
        dayInCycle = 1,
        moodScore = null,
        energyLevel = null,
        libidoScore = null,
        createdAt = testInstant,
        updatedAt = testInstant,
    )
    private val emptyLog = FullDailyLog(entry = emptyEntry)

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

        every { mockSymptomProvider.symptoms } returns flowOf(emptyList())
        every { mockMedicationProvider.medications } returns flowOf(emptyList())
        every { mockRepository.getAllPeriods() } returns flowOf(emptyList())
        coEvery { mockRepository.getWaterIntakeForDates(any()) } returns emptyList()
        coEvery { mockGetOrCreateDailyLog(any()) } returns emptyLog
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    private fun createViewModel(): DailyLogViewModel {
        return DailyLogViewModel(
            entryDate = testDate,
            periodRepository = mockRepository,
            getOrCreateDailyLog = mockGetOrCreateDailyLog,
            symptomLibraryProvider = mockSymptomProvider,
            medicationLibraryProvider = mockMedicationProvider,
            educationalContentProvider = mockEducationalContentProvider,
            renameSymptomUseCase = mockk(relaxed = true),
            deleteSymptomUseCase = mockk(relaxed = true),
            renameMedicationUseCase = mockk(relaxed = true),
            deleteMedicationUseCase = mockk(relaxed = true),
        )
    }

    @Test
    fun `empty state visible WHEN all wellness fields are unset`() = runTest {
        // GIVEN — daily log has no mood, energy, libido, and zero water
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value

        // THEN — all fields that drive empty-state visibility are in their "empty" state
        assertNull(state.log?.entry?.moodScore, "moodScore should be null")
        assertNull(state.log?.entry?.energyLevel, "energyLevel should be null")
        assertNull(state.log?.entry?.libidoScore, "libidoScore should be null")
        assertEquals(0, state.waterCups, "waterCups should be 0")
    }

    @Test
    fun `empty state hidden WHEN mood is set`() = runTest {
        // GIVEN — daily log with mood already set
        val entryWithMood = emptyEntry.copy(moodScore = 3)
        val logWithMood = FullDailyLog(entry = entryWithMood)
        coEvery { mockGetOrCreateDailyLog(any()) } returns logWithMood

        // WHEN — ViewModel is created
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value

        // THEN — moodScore is non-null, so the empty state condition is false
        assertNotNull(state.log?.entry?.moodScore, "moodScore should be set")
        assertEquals(3, state.log?.entry?.moodScore)
    }

    @Test
    fun `empty state hidden WHEN mood changed via event`() = runTest {
        // GIVEN — ViewModel starts with empty wellness data
        val vm = createViewModel()
        advanceUntilIdle()

        // Pre-condition: all wellness fields empty
        assertNull(vm.uiState.value.log?.entry?.moodScore)

        // WHEN — user changes mood score
        vm.onEvent(DailyLogEvent.MoodScoreChanged(4))
        advanceUntilIdle()

        // THEN — moodScore is now set, breaking the empty-state condition
        val state = vm.uiState.value
        assertEquals(4, state.log?.entry?.moodScore, "moodScore should be 4 after change")
    }

    @Test
    fun `empty state hidden WHEN water incremented`() = runTest {
        // GIVEN — ViewModel starts with zero water
        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.waterCups)

        // WHEN — user increments water
        vm.onEvent(DailyLogEvent.WaterIncrement)
        advanceUntilIdle()

        // THEN — waterCups > 0, breaking the empty-state condition
        assertEquals(1, vm.uiState.value.waterCups, "waterCups should be 1 after increment")
    }
}
