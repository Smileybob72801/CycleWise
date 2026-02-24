package com.veleda.cyclewise.domain.providers

import com.veleda.cyclewise.domain.models.ArticleCategory
import com.veleda.cyclewise.domain.models.EducationalArticle
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [EducationalContentProvider].
 *
 * Covers sorting, category filtering, tag filtering, and ID lookup.
 */
class EducationalContentProviderTest {

    private fun article(
        id: String = "test-id",
        category: ArticleCategory = ArticleCategory.CYCLE_BASICS,
        tags: List<String> = emptyList(),
        sortOrder: Int = 0,
    ) = EducationalArticle(
        id = id,
        title = "Title $id",
        body = "Body $id",
        category = category,
        contentTags = tags,
        sourceName = "Test Source",
        sourceUrl = "https://example.com",
        sortOrder = sortOrder,
    )

    // ── articles sorting ─────────────────────────────────────────────

    @Test
    fun `articles WHEN multipleArticles THEN sortedBySortOrder`() {
        // GIVEN — articles in reverse sort order
        val articles = listOf(
            article(id = "c", sortOrder = 30),
            article(id = "a", sortOrder = 10),
            article(id = "b", sortOrder = 20),
        )

        // WHEN
        val provider = EducationalContentProvider(articles)

        // THEN
        assertEquals(
            listOf("a", "b", "c"),
            provider.articles.map { it.id },
            "Articles should be sorted by sortOrder ascending"
        )
    }

    // ── getByCategory ────────────────────────────────────────────────

    @Test
    fun `getByCategory WHEN matchExists THEN returnsOnlyThatCategory`() {
        // GIVEN
        val articles = listOf(
            article(id = "basics", category = ArticleCategory.CYCLE_BASICS, sortOrder = 1),
            article(id = "symptoms", category = ArticleCategory.SYMPTOMS, sortOrder = 2),
            article(id = "basics2", category = ArticleCategory.CYCLE_BASICS, sortOrder = 3),
        )
        val provider = EducationalContentProvider(articles)

        // WHEN
        val result = provider.getByCategory(ArticleCategory.CYCLE_BASICS)

        // THEN
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == ArticleCategory.CYCLE_BASICS })
    }

    @Test
    fun `getByCategory WHEN noMatch THEN returnsEmptyList`() {
        // GIVEN
        val articles = listOf(
            article(id = "basics", category = ArticleCategory.CYCLE_BASICS, sortOrder = 1),
        )
        val provider = EducationalContentProvider(articles)

        // WHEN
        val result = provider.getByCategory(ArticleCategory.WHEN_TO_SEE_A_DOCTOR)

        // THEN
        assertTrue(result.isEmpty())
    }

    // ── getByTag ─────────────────────────────────────────────────────

    @Test
    fun `getByTag WHEN tagExists THEN returnsMatchingArticles`() {
        // GIVEN
        val articles = listOf(
            article(id = "a", tags = listOf("FlowIntensity", "Mood"), sortOrder = 1),
            article(id = "b", tags = listOf("Energy"), sortOrder = 2),
            article(id = "c", tags = listOf("Mood"), sortOrder = 3),
        )
        val provider = EducationalContentProvider(articles)

        // WHEN
        val result = provider.getByTag("Mood")

        // THEN
        assertEquals(listOf("a", "c"), result.map { it.id })
    }

    @Test
    fun `getByTag WHEN tagAbsent THEN returnsEmptyList`() {
        // GIVEN
        val articles = listOf(
            article(id = "a", tags = listOf("FlowIntensity"), sortOrder = 1),
        )
        val provider = EducationalContentProvider(articles)

        // WHEN
        val result = provider.getByTag("NonExistentTag")

        // THEN
        assertTrue(result.isEmpty())
    }

    // ── getById ──────────────────────────────────────────────────────

    @Test
    fun `getById WHEN idExists THEN returnsArticle`() {
        // GIVEN
        val articles = listOf(
            article(id = "find-me", sortOrder = 1),
            article(id = "not-me", sortOrder = 2),
        )
        val provider = EducationalContentProvider(articles)

        // WHEN
        val result = provider.getById("find-me")

        // THEN
        assertEquals("find-me", result?.id)
    }

    @Test
    fun `getById WHEN idAbsent THEN returnsNull`() {
        // GIVEN
        val provider = EducationalContentProvider(
            listOf(article(id = "exists", sortOrder = 1))
        )

        // WHEN
        val result = provider.getById("does-not-exist")

        // THEN
        assertNull(result)
    }
}
