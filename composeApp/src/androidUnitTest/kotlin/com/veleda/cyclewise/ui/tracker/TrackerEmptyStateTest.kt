package com.veleda.cyclewise.ui.tracker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.providers.EducationalContentProvider
import com.veleda.cyclewise.domain.providers.MedicationLibraryProvider
import com.veleda.cyclewise.domain.providers.SymptomLibraryProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.domain.usecases.AutoCloseOngoingPeriodUseCase
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.testutil.TestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.*

/**
 * Tests the conditions that drive the Tracker empty-state and bottom-text-area
 * visibility.
 *
 * The empty state is visible when `uiState.periods.isEmpty()` and hidden when
 * the list contains at least one [Period]. The bottom text area shows one of
 * three states: empty state (icon + title + body), ongoing-period text, or
 * drag-instructions text — driven by `periods.isEmpty()` and `ongoingPeriod`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackerEmptyStateTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockRepository: PeriodRepository
    private lateinit var mockSymptomProvider: SymptomLibraryProvider
    private lateinit var mockMedicationProvider: MedicationLibraryProvider
    private lateinit var mockAutoCloseUseCase: AutoCloseOngoingPeriodUseCase
    private lateinit var mockAppSettings: AppSettings
    private lateinit var mockEducationalContentProvider: EducationalContentProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockRepository = mockk(relaxed = true)
        mockSymptomProvider = mockk(relaxed = true)
        mockMedicationProvider = mockk(relaxed = true)
        mockAutoCloseUseCase = mockk(relaxed = true)
        mockAppSettings = mockk(relaxed = true)
        mockEducationalContentProvider = mockk(relaxed = true)

        every { mockRepository.observeDayDetails() } returns flowOf(emptyMap())
        every { mockSymptomProvider.symptoms } returns flowOf(emptyList())
        every { mockMedicationProvider.medications } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(periods: List<Period> = emptyList()): TrackerViewModel {
        every { mockRepository.getAllPeriods() } returns flowOf(periods)
        return TrackerViewModel(
            mockRepository, mockSymptomProvider, mockMedicationProvider,
            mockAutoCloseUseCase, mockAppSettings, mockEducationalContentProvider,
        )
    }

    @Test
    fun `empty state visible WHEN periods list is empty`() = runTest {
        // GIVEN — no periods exist
        val vm = createViewModel(periods = emptyList())
        advanceUntilIdle()

        // THEN — periods list is empty, so the overlay should be visible
        assertTrue(vm.uiState.value.periods.isEmpty(), "periods should be empty")
    }

    @Test
    fun `empty state hidden WHEN periods list has entries`() = runTest {
        // GIVEN — one period exists
        val period = Period(
            id = "period-1",
            startDate = TestData.DATE,
            endDate = TestData.DATE.plus(4, DateTimeUnit.DAY),
            createdAt = TestData.INSTANT,
            updatedAt = TestData.INSTANT,
        )
        val vm = createViewModel(periods = listOf(period))
        advanceUntilIdle()

        // THEN — periods list is non-empty, so the overlay should not be visible
        assertTrue(vm.uiState.value.periods.isNotEmpty(), "periods should not be empty")
        assertEquals(1, vm.uiState.value.periods.size)
    }

    @Test
    fun `ongoingPeriod is null WHEN periods list is empty`() = runTest {
        // GIVEN — no periods exist
        val vm = createViewModel(periods = emptyList())
        advanceUntilIdle()

        // THEN — ongoingPeriod is null (drives empty-state bottom text visibility)
        assertNull(vm.uiState.value.ongoingPeriod, "ongoingPeriod should be null when no periods exist")
    }

    @Test
    fun `ongoingPeriod is non-null WHEN a period has no endDate`() = runTest {
        // GIVEN — one ongoing period (endDate = null)
        val ongoingPeriod = Period(
            id = "period-ongoing",
            startDate = TestData.DATE,
            endDate = null,
            createdAt = TestData.INSTANT,
            updatedAt = TestData.INSTANT,
        )
        val vm = createViewModel(periods = listOf(ongoingPeriod))
        advanceUntilIdle()

        // THEN — ongoingPeriod should be the period with no endDate
        assertNotNull(vm.uiState.value.ongoingPeriod, "ongoingPeriod should be non-null for a period with no endDate")
        assertEquals("period-ongoing", vm.uiState.value.ongoingPeriod!!.id)
    }

    @Test
    fun `ongoingPeriod is null WHEN all periods have endDate`() = runTest {
        // GIVEN — one completed period (endDate is set)
        val completedPeriod = Period(
            id = "period-done",
            startDate = TestData.DATE,
            endDate = TestData.DATE.plus(4, DateTimeUnit.DAY),
            createdAt = TestData.INSTANT,
            updatedAt = TestData.INSTANT,
        )
        val vm = createViewModel(periods = listOf(completedPeriod))
        advanceUntilIdle()

        // THEN — no ongoing period (all have endDates)
        assertNull(vm.uiState.value.ongoingPeriod, "ongoingPeriod should be null when all periods have an endDate")
        assertTrue(vm.uiState.value.periods.isNotEmpty(), "periods should not be empty")
    }
}
