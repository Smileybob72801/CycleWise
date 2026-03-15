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
import com.veleda.cyclewise.ui.coachmark.HintKey
import com.veleda.cyclewise.ui.coachmark.HintPreferences
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
 * The prompt is controlled by a one-time [HintKey.WELLNESS_EMPTY_PROMPT] flag
 * persisted via [HintPreferences]. It shows only if the user has never logged
 * wellness data, and is permanently dismissed on the first wellness interaction.
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
    private lateinit var mockHintPreferences: HintPreferences

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
        mockHintPreferences = mockk(relaxed = true)
        every { mockHintPreferences.isHintSeen(any()) } returns flowOf(false)

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
            hintPreferences = mockHintPreferences,
        )
    }

    @Test
    fun `showWellnessPrompt true WHEN hint not seen`() = runTest {
        // GIVEN — hint has NOT been seen
        every { mockHintPreferences.isHintSeen(HintKey.WELLNESS_EMPTY_PROMPT) } returns flowOf(false)

        // WHEN
        val vm = createViewModel()
        advanceUntilIdle()

        // THEN
        assertTrue(vm.uiState.value.showWellnessPrompt, "prompt should show when hint not seen")
    }

    @Test
    fun `showWellnessPrompt false WHEN hint already seen`() = runTest {
        // GIVEN — hint HAS been seen
        every { mockHintPreferences.isHintSeen(HintKey.WELLNESS_EMPTY_PROMPT) } returns flowOf(true)

        // WHEN
        val vm = createViewModel()
        advanceUntilIdle()

        // THEN
        assertFalse(vm.uiState.value.showWellnessPrompt, "prompt should hide when hint already seen")
    }

    @Test
    fun `showWellnessPrompt dismissed WHEN mood changed via event`() = runTest {
        // GIVEN — prompt is showing
        every { mockHintPreferences.isHintSeen(HintKey.WELLNESS_EMPTY_PROMPT) } returns flowOf(false)
        val vm = createViewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showWellnessPrompt)

        // WHEN — user changes mood score
        vm.onEvent(DailyLogEvent.MoodScoreChanged(4))
        advanceUntilIdle()

        // THEN — prompt is dismissed
        assertFalse(vm.uiState.value.showWellnessPrompt, "prompt should be dismissed after mood change")
    }

    @Test
    fun `showWellnessPrompt dismissed WHEN water incremented`() = runTest {
        // GIVEN — prompt is showing
        every { mockHintPreferences.isHintSeen(HintKey.WELLNESS_EMPTY_PROMPT) } returns flowOf(false)
        val vm = createViewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showWellnessPrompt)

        // WHEN — user increments water
        vm.onEvent(DailyLogEvent.WaterIncrement)
        advanceUntilIdle()

        // THEN — prompt is dismissed
        assertFalse(vm.uiState.value.showWellnessPrompt, "prompt should be dismissed after water increment")
    }
}
