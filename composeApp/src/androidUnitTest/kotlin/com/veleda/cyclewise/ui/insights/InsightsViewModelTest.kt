package com.veleda.cyclewise.ui.insights

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.veleda.cyclewise.domain.insights.CycleLengthAverage
import com.veleda.cyclewise.domain.insights.InsightEngine
import com.veleda.cyclewise.domain.models.ArticleCategory
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.domain.providers.EducationalContentProvider
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.settings.AppSettings
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
    private lateinit var mockEducationalContentProvider: EducationalContentProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk()
        mockInsightEngine = mockk()
        mockAppSettings = mockk()
        mockEducationalContentProvider = mockk(relaxed = true)
        every { mockEducationalContentProvider.articles } returns emptyList()
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
        viewModel = InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings, mockEducationalContentProvider)

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
        viewModel = InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings, mockEducationalContentProvider)

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

        viewModel = InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings, mockEducationalContentProvider)

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

        viewModel = InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings, mockEducationalContentProvider)

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

    // ── Educational article tests ──────────────────────────────────

    private val testArticles = listOf(
        EducationalArticle(
            id = "a1",
            title = "Cycle Basics",
            body = "Body 1",
            category = ArticleCategory.CYCLE_BASICS,
            contentTags = listOf("CyclePhase"),
            sourceName = "OWH",
            sourceUrl = "https://example.com",
            sortOrder = 1,
        ),
        EducationalArticle(
            id = "a2",
            title = "Pain",
            body = "Body 2",
            category = ArticleCategory.SYMPTOMS,
            contentTags = listOf("Symptoms"),
            sourceName = "NICHD",
            sourceUrl = "https://example.com",
            sortOrder = 2,
        ),
        EducationalArticle(
            id = "a3",
            title = "Hydration",
            body = "Body 3",
            category = ArticleCategory.WELLNESS,
            contentTags = listOf("Mood"),
            sourceName = "OWH",
            sourceUrl = "https://example.com",
            sortOrder = 3,
        ),
    )

    private fun createViewModelWithArticles(articles: List<EducationalArticle> = testArticles): InsightsViewModel {
        every { mockEducationalContentProvider.articles } returns articles
        coEvery { mockAppSettings.topSymptomsCount } returns flowOf(3)
        coEvery { mockRepository.getAllPeriods() } returns flowOf(emptyList())
        coEvery { mockRepository.getAllLogs() } returns flowOf(emptyList())
        coEvery { mockRepository.getSymptomLibrary() } returns flowOf(emptyList())
        coEvery { mockInsightEngine.generateInsights(any(), any(), any(), any()) } returns emptyList()

        return InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings, mockEducationalContentProvider)
    }

    @Test
    fun `init WHEN articlesExist THEN allArticlesPopulated`() = runTest {
        // GIVEN — provider returns test articles
        // WHEN — ViewModel is created
        viewModel = createViewModelWithArticles()
        advanceUntilIdle()

        // THEN — allArticles and filteredArticles are populated
        assertEquals(testArticles, viewModel.uiState.value.allArticles)
        assertEquals(testArticles, viewModel.uiState.value.filteredArticles)
    }

    @Test
    fun `filterArticles WHEN null THEN showsAll`() = runTest {
        // GIVEN — ViewModel with articles, initially filtered by category
        viewModel = createViewModelWithArticles()
        advanceUntilIdle()

        every { mockEducationalContentProvider.getByCategory(ArticleCategory.SYMPTOMS) } returns
                listOf(testArticles[1])
        viewModel.onEvent(InsightsEvent.FilterArticles(ArticleCategory.SYMPTOMS))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.filteredArticles.size)

        // WHEN — filter reset to null (All)
        viewModel.onEvent(InsightsEvent.FilterArticles(null))
        advanceUntilIdle()

        // THEN — all articles shown again
        assertEquals(testArticles, viewModel.uiState.value.filteredArticles)
        assertEquals(null, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `filterArticles WHEN category THEN filtersCorrectly`() = runTest {
        // GIVEN — ViewModel with articles
        viewModel = createViewModelWithArticles()
        advanceUntilIdle()

        every { mockEducationalContentProvider.getByCategory(ArticleCategory.SYMPTOMS) } returns
                listOf(testArticles[1])

        // WHEN — filter by SYMPTOMS
        viewModel.onEvent(InsightsEvent.FilterArticles(ArticleCategory.SYMPTOMS))
        advanceUntilIdle()

        // THEN — only SYMPTOMS articles shown
        assertEquals(1, viewModel.uiState.value.filteredArticles.size)
        assertEquals(ArticleCategory.SYMPTOMS, viewModel.uiState.value.filteredArticles[0].category)
        assertEquals(ArticleCategory.SYMPTOMS, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `toggleArticleExpanded WHEN notExpanded THEN addsToSet`() = runTest {
        // GIVEN — ViewModel with articles, none expanded
        viewModel = createViewModelWithArticles()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.expandedArticleIds.isEmpty())

        // WHEN — toggle article "a1"
        viewModel.onEvent(InsightsEvent.ToggleArticleExpanded("a1"))
        advanceUntilIdle()

        // THEN — "a1" is in expanded set
        assertTrue("a1" in viewModel.uiState.value.expandedArticleIds)
    }

    @Test
    fun `toggleArticleExpanded WHEN expanded THEN removesFromSet`() = runTest {
        // GIVEN — ViewModel with "a1" already expanded
        viewModel = createViewModelWithArticles()
        advanceUntilIdle()
        viewModel.onEvent(InsightsEvent.ToggleArticleExpanded("a1"))
        advanceUntilIdle()
        assertTrue("a1" in viewModel.uiState.value.expandedArticleIds)

        // WHEN — toggle "a1" again
        viewModel.onEvent(InsightsEvent.ToggleArticleExpanded("a1"))
        advanceUntilIdle()

        // THEN — "a1" is removed from expanded set
        assertFalse("a1" in viewModel.uiState.value.expandedArticleIds)
    }
}
