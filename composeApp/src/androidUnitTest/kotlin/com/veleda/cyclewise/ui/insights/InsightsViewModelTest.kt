package com.veleda.cyclewise.ui.insights

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.veleda.cyclewise.domain.insights.CycleLengthAverage
import com.veleda.cyclewise.domain.insights.InsightEngine
import com.veleda.cyclewise.domain.repository.PeriodRepository
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

    private lateinit var mockRepository: PeriodRepository
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
    fun init_WHEN_dataLoaded_THEN_uiStateHasInsights() = runTest {
        // ARRANGE
        val fakeInsights = listOf(CycleLengthAverage(28.5))
        coEvery { mockAppSettings.topSymptomsCount } returns flowOf(3)
        coEvery { mockRepository.getAllPeriods() } returns flowOf(emptyList())
        coEvery { mockRepository.getAllLogs() } returns flowOf(emptyList())
        coEvery { mockRepository.getSymptomLibrary() } returns flowOf(emptyList())
        coEvery { mockInsightEngine.generateInsights(any(), any(), any(), any()) } returns fakeInsights

        // ACT
        viewModel = InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings)

        // ASSERT
        viewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading, "Final state should not be loading")
            assertEquals(fakeInsights, finalState.insights)
            expectNoEvents()
        }
    }

    @Test
    fun init_WHEN_noInsights_THEN_uiStateHasEmptyList() = runTest {
        // ARRANGE
        coEvery { mockAppSettings.topSymptomsCount } returns flowOf(3)
        coEvery { mockRepository.getAllPeriods() } returns flowOf(emptyList())
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

    @Test
    fun refresh_WHEN_called_THEN_setsIsRefreshingAndReloads() = runTest {
        // ARRANGE — init with empty, then refresh returns data
        val refreshedInsights = listOf(CycleLengthAverage(30.0))
        coEvery { mockAppSettings.topSymptomsCount } returns flowOf(3)
        coEvery { mockRepository.getAllPeriods() } returns flowOf(emptyList())
        coEvery { mockRepository.getAllLogs() } returns flowOf(emptyList())
        coEvery { mockRepository.getSymptomLibrary() } returns flowOf(emptyList())
        coEvery {
            mockInsightEngine.generateInsights(any(), any(), any(), any())
        } returns emptyList() andThen refreshedInsights

        viewModel = InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings)

        // ACT
        viewModel.refresh()

        // ASSERT
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRefreshing, "isRefreshing should be false after refresh completes")
            assertEquals(refreshedInsights, state.insights)
            expectNoEvents()
        }
    }

    @Test
    fun refresh_WHEN_called_THEN_doesNotSetIsLoading() = runTest {
        // ARRANGE — init returns empty, refresh returns new data so StateFlow emits a change
        val refreshedInsights = listOf(CycleLengthAverage(25.0))
        coEvery { mockAppSettings.topSymptomsCount } returns flowOf(3)
        coEvery { mockRepository.getAllPeriods() } returns flowOf(emptyList())
        coEvery { mockRepository.getAllLogs() } returns flowOf(emptyList())
        coEvery { mockRepository.getSymptomLibrary() } returns flowOf(emptyList())
        coEvery {
            mockInsightEngine.generateInsights(any(), any(), any(), any())
        } returns emptyList() andThen refreshedInsights

        viewModel = InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings)

        // ASSERT — after init, isLoading should already be false
        viewModel.uiState.test {
            val postInitState = awaitItem()
            assertFalse(postInitState.isLoading, "isLoading should be false after init")

            // ACT — refresh produces new data, forcing a new emission
            viewModel.refresh()
            val postRefreshState = awaitItem()

            // ASSERT — isLoading must stay false during and after refresh
            assertFalse(postRefreshState.isLoading, "isLoading should stay false during refresh")
            assertFalse(postRefreshState.isRefreshing, "isRefreshing should be false after refresh completes")
            assertEquals(refreshedInsights, postRefreshState.insights)
            expectNoEvents()
        }
    }
}
