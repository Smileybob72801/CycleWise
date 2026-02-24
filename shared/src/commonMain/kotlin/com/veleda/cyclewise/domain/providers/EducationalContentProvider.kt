package com.veleda.cyclewise.domain.providers

import com.veleda.cyclewise.domain.models.ArticleCategory
import com.veleda.cyclewise.domain.models.EducationalArticle

/**
 * Singleton provider for educational content loaded from the bundled JSON asset.
 *
 * Unlike [SymptomLibraryProvider] and [MedicationLibraryProvider], this provider
 * is **not** Flow-based because the content is static and immutable — it never
 * changes after the app starts. It is registered in Koin's singleton scope so it
 * can be injected into both singleton-scoped and session-scoped ViewModels.
 *
 * @param articles The full list of articles loaded at app start.
 */
class EducationalContentProvider(articles: List<EducationalArticle>) {

    /** All articles sorted by [EducationalArticle.sortOrder] (ascending). */
    val articles: List<EducationalArticle> = articles.sortedBy { it.sortOrder }

    /**
     * Returns articles belonging to the given [category], preserving sort order.
     *
     * @param category The category to filter by.
     * @return Articles matching the category, or an empty list if none match.
     */
    fun getByCategory(category: ArticleCategory): List<EducationalArticle> =
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
    fun getByTag(tag: String): List<EducationalArticle> =
        articles.filter { tag in it.contentTags }

    /**
     * Returns the article with the given [id], or `null` if not found.
     *
     * @param id The unique article identifier.
     */
    fun getById(id: String): EducationalArticle? =
        articles.find { it.id == id }
}
