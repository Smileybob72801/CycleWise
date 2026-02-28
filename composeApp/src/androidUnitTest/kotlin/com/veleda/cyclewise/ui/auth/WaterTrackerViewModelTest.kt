package com.veleda.cyclewise.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class WaterTrackerViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockLockedWaterDraft: LockedWaterDraft
    private val draftsFlow = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockLockedWaterDraft = mockk(relaxed = true)
        every { mockLockedWaterDraft.drafts } returns draftsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): WaterTrackerViewModel {
        return WaterTrackerViewModel(mockLockedWaterDraft)
    }

    @Test
    fun init_WHEN_created_THEN_callsEnsureRolledOver() = runTest {
        // ACT
        createViewModel()
        advanceUntilIdle()

        // ASSERT
        coVerify(exactly = 1) { mockLockedWaterDraft.ensureRolledOver(any()) }
    }

    @Test
    fun init_WHEN_draftsEmitTodaysCups_THEN_stateReflectsCupCount() = runTest {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        draftsFlow.value = mapOf(today to 5)

        // ACT
        val viewModel = createViewModel()
        advanceUntilIdle()

        // ASSERT
        assertEquals(5, viewModel.uiState.value.todayCups)
    }

    @Test
    fun init_WHEN_draftsContainYesterdayCups_THEN_stateShowsYesterdayPrompt() = runTest {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        draftsFlow.value = mapOf(today to 3, yesterday to 7)

        // ACT
        val viewModel = createViewModel()
        advanceUntilIdle()

        // ASSERT
        assertEquals(3, viewModel.uiState.value.todayCups)
        assertEquals(7, viewModel.uiState.value.yesterdayCupsForPrompt)
    }

    @Test
    fun init_WHEN_noYesterdayDraft_THEN_yesterdayCupsForPromptIsNull() = runTest {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        draftsFlow.value = mapOf(today to 2)

        // ACT
        val viewModel = createViewModel()
        advanceUntilIdle()

        // ASSERT
        assertNull(viewModel.uiState.value.yesterdayCupsForPrompt)
    }

    @Test
    fun init_WHEN_yesterdayCupsAreZero_THEN_yesterdayCupsForPromptIsNull() = runTest {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        draftsFlow.value = mapOf(today to 1, yesterday to 0)

        // ACT
        val viewModel = createViewModel()
        advanceUntilIdle()

        // ASSERT — 0 cups should not trigger a prompt
        assertNull(viewModel.uiState.value.yesterdayCupsForPrompt)
    }

    @Test
    fun onIncrement_WHEN_called_THEN_callsSetCupsWithIncrementedValue() = runTest {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        draftsFlow.value = mapOf(today to 3)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // ACT
        viewModel.onIncrement()
        advanceUntilIdle()

        // ASSERT
        coVerify { mockLockedWaterDraft.setCups(today, 4) }
    }

    @Test
    fun onDecrement_WHEN_cupsAboveZero_THEN_callsSetCupsWithDecrementedValue() = runTest {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        draftsFlow.value = mapOf(today to 3)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // ACT
        viewModel.onDecrement()
        advanceUntilIdle()

        // ASSERT
        coVerify { mockLockedWaterDraft.setCups(today, 2) }
    }

    @Test
    fun onDecrement_WHEN_cupsAreZero_THEN_doesNotCallSetCups() = runTest {
        // ARRANGE
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        draftsFlow.value = mapOf(today to 0)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // ACT
        viewModel.onDecrement()
        advanceUntilIdle()

        // ASSERT — setCups should not be called for decrement from 0
        coVerify(exactly = 0) { mockLockedWaterDraft.setCups(any(), any()) }
    }

    @Test
    fun onIncrement_WHEN_noDraftForToday_THEN_incrementsFromZero() = runTest {
        // ARRANGE
        draftsFlow.value = emptyMap()
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.todayCups)

        // ACT
        viewModel.onIncrement()
        advanceUntilIdle()

        // ASSERT
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        coVerify { mockLockedWaterDraft.setCups(today, 1) }
    }
}
