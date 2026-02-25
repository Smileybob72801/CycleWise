package com.veleda.cyclewise.domain.models

import kotlinx.serialization.Serializable

/**
 * Categorises educational articles into browsable topic groups.
 *
 * Each value carries a [displayKey] used to look up the localised category
 * label from string resources at the UI layer.
 *
 * Annotated with [@Serializable] (unlike other domain enums) because
 * articles are loaded from a JSON asset via `kotlinx.serialization`.
 */
@Serializable
enum class ArticleCategory(val displayKey: String) {
    CYCLE_BASICS("cycle_basics"),
    SYMPTOMS("symptoms"),
    WELLNESS("wellness"),
    WHEN_TO_SEE_A_DOCTOR("when_to_see_doctor"),
}

/**
 * A single educational article displayed in contextual bottom sheets and the
 * browsable Learn section of the Insights tab.
 *
 * Articles are authored as a JSON asset (`res/raw/educational_content.json`)
 * and deserialised at app start. Body text uses lightweight Markdown formatting
 * (`**bold**`, `- ` bullet lists) rendered by the [MarkdownText] composable.
 *
 * Annotated with [@Serializable] (unlike other domain models) because the
 * loading mechanism is JSON deserialization via `kotlinx.serialization`.
 *
 * @property id          Stable unique identifier (e.g. `"cycle-basics-01"`).
 * @property title       Article headline shown in cards and bottom sheets.
 * @property body        Article body text, potentially with Markdown formatting.
 * @property category    The [ArticleCategory] this article belongs to.
 * @property contentTags Tags linking this article to UI elements (e.g. `"FlowIntensity"`, `"Mood"`).
 * @property sourceName  Human-readable attribution (e.g. `"Office on Women's Health"`).
 * @property sourceUrl   URL of the original government source page.
 * @property sortOrder   Controls display ordering within lists; lower values appear first.
 */
@Serializable
data class EducationalArticle(
    val id: String,
    val title: String,
    val body: String,
    val category: ArticleCategory,
    val contentTags: List<String>,
    val sourceName: String,
    val sourceUrl: String,
    val sortOrder: Int,
)
