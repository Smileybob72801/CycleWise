package com.veleda.cyclewise.domain.providers

import com.veleda.cyclewise.domain.models.ArticleCategory
import com.veleda.cyclewise.domain.models.EducationalArticle

/**
 * Contract for accessing educational content loaded from bundled assets.
 *
 * Unlike [SymptomLibraryProvider] and [MedicationLibraryProvider], this provider
 * is **not** Flow-based because the content is static and immutable — it never
 * changes after the app starts. It is registered in Koin's singleton scope so it
 * can be injected into both singleton-scoped and session-scoped ViewModels.
 *
 * The default Android implementation is
 * `com.veleda.cyclewise.androidData.local.providers.StaticEducationalContentProvider`.
 *
 * @see com.veleda.cyclewise.domain.models.EducationalArticle
 */
interface EducationalContentProvider {

    /** All articles sorted by [EducationalArticle.sortOrder] (ascending). */
    val articles: List<EducationalArticle>

    /**
     * Returns articles belonging to the given [category], preserving sort order.
     *
     * @param category The category to filter by.
     * @return Articles matching the category, or an empty list if none match.
     */
    fun getByCategory(category: ArticleCategory): List<EducationalArticle>

    /**
     * Returns articles whose [EducationalArticle.contentTags] contain the given [tag].
     *
     * Used by info buttons to find articles relevant to a specific UI element
     * (e.g. `"FlowIntensity"`, `"Mood"`, `"CyclePhase"`).
     *
     * @param tag The content tag to search for (case-sensitive).
     * @return Articles containing the tag, or an empty list if none match.
     */
    fun getByTag(tag: String): List<EducationalArticle>

    /**
     * Returns the article with the given [id], or `null` if not found.
     *
     * @param id The unique article identifier.
     * @return The matching [EducationalArticle], or `null` if no article has the given [id].
     */
    fun getById(id: String): EducationalArticle?
}
