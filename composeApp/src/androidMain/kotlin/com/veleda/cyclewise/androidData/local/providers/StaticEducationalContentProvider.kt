package com.veleda.cyclewise.androidData.local.providers

import com.veleda.cyclewise.domain.models.ArticleCategory
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.domain.providers.EducationalContentProvider

/**
 * Android implementation of [EducationalContentProvider] backed by an immutable,
 * pre-loaded list of articles from the bundled JSON asset.
 *
 * The name "Static" reflects that the content is fixed at build time and never
 * changes after the app starts. Articles are sorted once at construction time
 * by [EducationalArticle.sortOrder] (ascending).
 *
 * Registered as a Koin singleton so it can be injected into both singleton-scoped
 * and session-scoped ViewModels.
 *
 * @param articles The full list of articles loaded at app start.
 */
class StaticEducationalContentProvider(
    articles: List<EducationalArticle>,
) : EducationalContentProvider {

    /** All articles sorted by [EducationalArticle.sortOrder] (ascending). */
    override val articles: List<EducationalArticle> = articles.sortedBy { it.sortOrder }

    /**
     * Returns articles belonging to the given [category], preserving sort order.
     *
     * @param category The category to filter by.
     * @return Articles matching the category, or an empty list if none match.
     */
    override fun getByCategory(category: ArticleCategory): List<EducationalArticle> =
        articles.filter { it.category == category }

    /**
     * Returns articles whose [EducationalArticle.contentTags] contain the given [tag].
     *
     * Used by info buttons to find articles relevant to a specific UI element
     * (e.g. `"FlowIntensity"`, `"Mood"`, `"CyclePhase"`).
     *
     * @param tag The content tag to search for (case-sensitive).
     * @return Articles containing the tag, or an empty list if none match.
     */
    override fun getByTag(tag: String): List<EducationalArticle> =
        articles.filter { tag in it.contentTags }

    /**
     * Returns the article with the given [id], or `null` if not found.
     *
     * @param id The unique article identifier.
     */
    override fun getById(id: String): EducationalArticle? =
        articles.find { it.id == id }
}
