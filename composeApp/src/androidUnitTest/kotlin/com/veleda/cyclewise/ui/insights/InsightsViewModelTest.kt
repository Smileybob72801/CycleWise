package com.veleda.cyclewise.ui.insights

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.veleda.cyclewise.domain.insights.CycleLengthAverage
import com.veleda.cyclewise.domain.insights.InsightEngine
import com.veleda.cyclewise.domain.repository.CycleRepository
import com.veleda.cyclewise.settings.AppSettings
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockRepository: CycleRepository
    private lateinit var mockInsightEngine: InsightEngine
    private lateinit var viewModel: InsightsViewModel
    private lateinit var mockAppSettings: AppSettings

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk()
        mockInsightEngine = mockk()
        mockAppSettings = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init WHEN data is loaded successfully THEN uiState is updated with insights`() = runTest {
        // ARRANGE
        val fakeInsights = listOf(CycleLengthAverage(28.5))
        coEvery { mockAppSettings.topSymptomsCount } returns flowOf(3)
        coEvery { mockRepository.getAllCycles() } returns flowOf(emptyList())
        coEvery { mockRepository.getAllLogs() } returns flowOf(emptyList())
        coEvery { mockRepository.getSymptomLibrary() } returns flowOf(emptyList())
        coEvery { mockInsightEngine.generateInsights(any(), any(), any(), any()) } returns fakeInsights

        // ACT - Create the ViewModel *after* mocks are set up.
        viewModel = InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings)

        // ASSERT
        viewModel.uiState.test {
            // Because of the UnconfinedTestDispatcher, the init block has already finished.
            // We are only observing the final state.
            val finalState = awaitItem()
            assertFalse(finalState.isLoading, "Final state should not be loading")
            assertEquals(fakeInsights, finalState.insights)

            // We expect no more emissions.
            expectNoEvents()
        }
    }

    @Test
    fun `init WHEN engine returns no insights THEN uiState is updated with empty list`() = runTest {
        // ARRANGE
        coEvery { mockAppSettings.topSymptomsCount } returns flowOf(3)
        coEvery { mockRepository.getAllCycles() } returns flowOf(emptyList())
        coEvery { mockRepository.getAllLogs() } returns flowOf(emptyList())
        coEvery { mockRepository.getSymptomLibrary() } returns flowOf(emptyList())
        coEvery { mockInsightEngine.generateInsights(any(), any(), any(), any()) } returns emptyList()

        // ACT
        viewModel = InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings)

        // ASSERT
        viewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertTrue(finalState.insights.isEmpty())
            expectNoEvents()
        }
    }
}