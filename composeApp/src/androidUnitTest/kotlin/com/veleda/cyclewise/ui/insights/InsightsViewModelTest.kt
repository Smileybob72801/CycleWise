package com.veleda.cyclewise.ui.insights

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.veleda.cyclewise.domain.insights.CycleLengthAverage
import com.veleda.cyclewise.domain.insights.InsightEngine
import com.veleda.cyclewise.domain.insights.InsightScorer
import com.veleda.cyclewise.domain.insights.ScoredInsight
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

    private fun stubRepositoryDefaults() {
        coEvery { mockAppSettings.topSymptomsCount } returns flowOf(3)
        coEvery { mockRepository.getAllPeriods() } returns flowOf(emptyList())
        coEvery { mockRepository.getAllLogs() } returns flowOf(emptyList())
        coEvery { mockRepository.getSymptomLibrary() } returns flowOf(emptyList())
        coEvery { mockRepository.getAllWaterIntakes() } returns flowOf(emptyList())
        coEvery { mockRepository.getMedicationLibrary() } returns flowOf(emptyList())
    }

    @Test
    fun init_WHEN_dataLoaded_THEN_uiStateHasInsights() = runTest {
        // ARRANGE
        val fakeInsight = CycleLengthAverage(28.5)
        val fakeScored = listOf(ScoredInsight(fakeInsight, 0.8))
        stubRepositoryDefaults()
        coEvery { mockInsightEngine.generateInsights(any(), any(), any(), any(), any(), any()) } returns fakeScored

        // ACT
        viewModel = InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings, mockEducationalContentProvider)

        // ASSERT
        viewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading, "Final state should not be loading")
            assertEquals(listOf(fakeInsight), finalState.insights)
            expectNoEvents()
        }
    }

    @Test
    fun init_WHEN_noInsights_THEN_uiStateHasEmptyList() = runTest {
        // ARRANGE
        stubRepositoryDefaults()
        coEvery { mockInsightEngine.generateInsights(any(), any(), any(), any(), any(), any()) } returns emptyList()

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
        // ARRANGE
        val refreshedInsight = CycleLengthAverage(30.0)
        val refreshedScored = listOf(ScoredInsight(refreshedInsight, 0.8))
        stubRepositoryDefaults()
        coEvery {
            mockInsightEngine.generateInsights(any(), any(), any(), any(), any(), any())
        } returns emptyList() andThen refreshedScored

        viewModel = InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings, mockEducationalContentProvider)

        // ACT
        viewModel.refresh()

        // ASSERT
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isRefreshing, "isRefreshing should be false after refresh completes")
            assertEquals(listOf(refreshedInsight), state.insights)
            expectNoEvents()
        }
    }

    @Test
    fun refresh_WHEN_called_THEN_doesNotSetIsLoading() = runTest {
        // ARRANGE
        val refreshedInsight = CycleLengthAverage(25.0)
        val refreshedScored = listOf(ScoredInsight(refreshedInsight, 0.8))
        stubRepositoryDefaults()
        coEvery {
            mockInsightEngine.generateInsights(any(), any(), any(), any(), any(), any())
        } returns emptyList() andThen refreshedScored

        viewModel = InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings, mockEducationalContentProvider)

        // ASSERT
        viewModel.uiState.test {
            val postInitState = awaitItem()
            assertFalse(postInitState.isLoading, "isLoading should be false after init")

            // ACT
            viewModel.refresh()
            val postRefreshState = awaitItem()

            assertFalse(postRefreshState.isLoading, "isLoading should stay false during refresh")
            assertFalse(postRefreshState.isRefreshing, "isRefreshing should be false after refresh completes")
            assertEquals(listOf(refreshedInsight), postRefreshState.insights)
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
        stubRepositoryDefaults()
        coEvery { mockInsightEngine.generateInsights(any(), any(), any(), any(), any(), any()) } returns emptyList()

        return InsightsViewModel(mockRepository, mockInsightEngine, mockAppSettings, mockEducationalContentProvider)
    }

    @Test
    fun `init WHEN articlesExist THEN allArticlesPopulated`() = runTest {
        viewModel = createViewModelWithArticles()
        advanceUntilIdle()

        assertEquals(testArticles, viewModel.uiState.value.allArticles)
        assertEquals(testArticles, viewModel.uiState.value.filteredArticles)
    }

    @Test
    fun `filterArticles WHEN null THEN showsAll`() = runTest {
        viewModel = createViewModelWithArticles()
        advanceUntilIdle()

        every { mockEducationalContentProvider.getByCategory(ArticleCategory.SYMPTOMS) } returns
                listOf(testArticles[1])
        viewModel.onEvent(InsightsEvent.FilterArticles(ArticleCategory.SYMPTOMS))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.filteredArticles.size)

        viewModel.onEvent(InsightsEvent.FilterArticles(null))
        advanceUntilIdle()

        assertEquals(testArticles, viewModel.uiState.value.filteredArticles)
        assertEquals(null, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `filterArticles WHEN category THEN filtersCorrectly`() = runTest {
        viewModel = createViewModelWithArticles()
        advanceUntilIdle()

        every { mockEducationalContentProvider.getByCategory(ArticleCategory.SYMPTOMS) } returns
                listOf(testArticles[1])

        viewModel.onEvent(InsightsEvent.FilterArticles(ArticleCategory.SYMPTOMS))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.filteredArticles.size)
        assertEquals(ArticleCategory.SYMPTOMS, viewModel.uiState.value.filteredArticles[0].category)
        assertEquals(ArticleCategory.SYMPTOMS, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `toggleArticleExpanded WHEN notExpanded THEN addsToSet`() = runTest {
        viewModel = createViewModelWithArticles()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.expandedArticleIds.isEmpty())

        viewModel.onEvent(InsightsEvent.ToggleArticleExpanded("a1"))
        advanceUntilIdle()

        assertTrue("a1" in viewModel.uiState.value.expandedArticleIds)
    }

    @Test
    fun `toggleArticleExpanded WHEN expanded THEN removesFromSet`() = runTest {
        viewModel = createViewModelWithArticles()
        advanceUntilIdle()
        viewModel.onEvent(InsightsEvent.ToggleArticleExpanded("a1"))
        advanceUntilIdle()
        assertTrue("a1" in viewModel.uiState.value.expandedArticleIds)

        viewModel.onEvent(InsightsEvent.ToggleArticleExpanded("a1"))
        advanceUntilIdle()

        assertFalse("a1" in viewModel.uiState.value.expandedArticleIds)
    }

    @Test
    fun `reduce FilterArticles WHEN category THEN selectedCategoryUpdatedImmediately`() = runTest {
        viewModel = createViewModelWithArticles()
        advanceUntilIdle()

        every { mockEducationalContentProvider.getByCategory(ArticleCategory.WELLNESS) } returns
                listOf(testArticles[2])

        viewModel.onEvent(InsightsEvent.FilterArticles(ArticleCategory.WELLNESS))

        assertEquals(ArticleCategory.WELLNESS, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `reduce ToggleArticleExpanded WHEN multipleToggled THEN setTracksAll`() = runTest {
        viewModel = createViewModelWithArticles()
        advanceUntilIdle()

        viewModel.onEvent(InsightsEvent.ToggleArticleExpanded("a1"))
        viewModel.onEvent(InsightsEvent.ToggleArticleExpanded("a2"))

        val expanded = viewModel.uiState.value.expandedArticleIds
        assertTrue("a1" in expanded)
        assertTrue("a2" in expanded)
        assertEquals(2, expanded.size)
    }
}
