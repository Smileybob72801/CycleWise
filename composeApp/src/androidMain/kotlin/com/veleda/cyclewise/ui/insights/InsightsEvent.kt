package com.veleda.cyclewise.ui.insights

import com.veleda.cyclewise.domain.insights.InsightCategory
import com.veleda.cyclewise.domain.models.ArticleCategory

/**
 * Defines all user interactions that can occur on the Insights screen.
 */
sealed interface InsightsEvent {
    /**
     * The user selected a category filter chip.
     *
     * @property category The [ArticleCategory] to filter by, or `null` to show all articles.
     */
    data class FilterArticles(val category: ArticleCategory?) : InsightsEvent

    /**
     * The user tapped an article card to expand or collapse it.
     *
     * @property articleId The ID of the [EducationalArticle][com.veleda.cyclewise.domain.models.EducationalArticle]
     *                     to toggle.
     */
    data class ToggleArticleExpanded(val articleId: String) : InsightsEvent

    /**
     * The user tapped a category header to expand or collapse the categorized insights section.
     *
     * @property category The [InsightCategory] to toggle.
     */
    data class ToggleCategory(val category: InsightCategory) : InsightsEvent
}
