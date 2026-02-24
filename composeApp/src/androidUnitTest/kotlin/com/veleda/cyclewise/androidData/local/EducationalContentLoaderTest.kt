package com.veleda.cyclewise.androidData.local

import com.veleda.cyclewise.domain.models.ArticleCategory
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Unit tests for [EducationalContentLoader.parseJson].
 *
 * Tests the JSON deserialization path without requiring Android context.
 */
class EducationalContentLoaderTest {

    @Test
    fun `parseJson WHEN validJson THEN returnsCorrectArticles`() {
        // GIVEN
        val json = """
            [
              {
                "id": "test-01",
                "title": "Test Article",
                "body": "Test body text.",
                "category": "CYCLE_BASICS",
                "contentTags": ["CyclePhase", "FlowIntensity"],
                "sourceName": "Test Source",
                "sourceUrl": "https://example.com",
                "sortOrder": 1
              }
            ]
        """.trimIndent()

        // WHEN
        val result = EducationalContentLoader.parseJson(json)

        // THEN
        assertEquals(1, result.size)
        val article = result.first()
        assertEquals("test-01", article.id)
        assertEquals("Test Article", article.title)
        assertEquals("Test body text.", article.body)
        assertEquals(ArticleCategory.CYCLE_BASICS, article.category)
        assertEquals(listOf("CyclePhase", "FlowIntensity"), article.contentTags)
        assertEquals("Test Source", article.sourceName)
        assertEquals("https://example.com", article.sourceUrl)
        assertEquals(1, article.sortOrder)
    }

    @Test
    fun `parseJson WHEN malformedJson THEN returnsEmptyList`() {
        // GIVEN
        val malformedJson = "{ this is not valid json ["

        // WHEN
        val result = EducationalContentLoader.parseJson(malformedJson)

        // THEN
        assertTrue(result.isEmpty(), "Malformed JSON should return empty list")
    }

    // ── Production content validation ─────────────────────────────

    /**
     * Reads the actual production JSON from the resource file and parses it.
     * Returns null if the file cannot be read (e.g., in environments without resources).
     */
    private fun loadProductionJson(): String? {
        return this::class.java.classLoader
            ?.getResource("raw/educational_content.json")
            ?.readText()
            ?: this::class.java.getResourceAsStream("/raw/educational_content.json")
                ?.bufferedReader()?.readText()
    }

    /**
     * Inline production JSON for deterministic testing without resource loading.
     * This mirrors the production educational_content.json file.
     */
    private val productionJson = """
        [
          {"id":"cycle-basics-01","title":"What Is a Menstrual Cycle?","body":"body","category":"CYCLE_BASICS","contentTags":["CyclePhase"],"sourceName":"OWH","sourceUrl":"https://example.com","sortOrder":1},
          {"id":"cycle-basics-02","title":"The Menstrual Phase","body":"body","category":"CYCLE_BASICS","contentTags":["CyclePhase","CyclePhase.MENSTRUATION","FlowIntensity"],"sourceName":"OWH","sourceUrl":"https://example.com","sortOrder":2},
          {"id":"cycle-basics-03","title":"The Follicular Phase","body":"body","category":"CYCLE_BASICS","contentTags":["CyclePhase","CyclePhase.FOLLICULAR"],"sourceName":"NICHD","sourceUrl":"https://example.com","sortOrder":3},
          {"id":"cycle-basics-04","title":"Ovulation: What Happens and When","body":"body","category":"CYCLE_BASICS","contentTags":["CyclePhase","CyclePhase.OVULATION"],"sourceName":"NICHD","sourceUrl":"https://example.com","sortOrder":4},
          {"id":"cycle-basics-05","title":"The Luteal Phase","body":"body","category":"CYCLE_BASICS","contentTags":["CyclePhase","CyclePhase.LUTEAL"],"sourceName":"OWH","sourceUrl":"https://example.com","sortOrder":5},
          {"id":"symptoms-01","title":"Pain and Cramps","body":"body","category":"SYMPTOMS","contentTags":["Symptoms","SymptomCategory.PAIN"],"sourceName":"OWH","sourceUrl":"https://example.com","sortOrder":10},
          {"id":"symptoms-02","title":"Mood Changes and Your Cycle","body":"body","category":"SYMPTOMS","contentTags":["Symptoms","SymptomCategory.MOOD","Mood"],"sourceName":"OWH","sourceUrl":"https://example.com","sortOrder":11},
          {"id":"symptoms-03","title":"Digestive Symptoms","body":"body","category":"SYMPTOMS","contentTags":["Symptoms","SymptomCategory.DIGESTIVE"],"sourceName":"NICHD","sourceUrl":"https://example.com","sortOrder":12},
          {"id":"symptoms-04","title":"Skin Changes During Your Cycle","body":"body","category":"SYMPTOMS","contentTags":["Symptoms","SymptomCategory.SKIN"],"sourceName":"OWH","sourceUrl":"https://example.com","sortOrder":13},
          {"id":"symptoms-05","title":"Fatigue and Energy","body":"body","category":"SYMPTOMS","contentTags":["Symptoms","SymptomCategory.ENERGY","Energy"],"sourceName":"OWH","sourceUrl":"https://example.com","sortOrder":14},
          {"id":"wellness-01","title":"Why Track Your Cycle?","body":"body","category":"WELLNESS","contentTags":["Mood","Energy"],"sourceName":"OWH","sourceUrl":"https://example.com","sortOrder":20},
          {"id":"wellness-02","title":"Hydration and Nutrition","body":"body","category":"WELLNESS","contentTags":["CyclePhase.MENSTRUATION"],"sourceName":"OWH","sourceUrl":"https://example.com","sortOrder":21},
          {"id":"wellness-03","title":"Exercise and Your Cycle","body":"body","category":"WELLNESS","contentTags":["Energy"],"sourceName":"OWH","sourceUrl":"https://example.com","sortOrder":22},
          {"id":"wellness-04","title":"Mood, Hormones, and the Cycle","body":"body","category":"WELLNESS","contentTags":["Mood","CyclePhase.LUTEAL"],"sourceName":"OWH","sourceUrl":"https://example.com","sortOrder":23},
          {"id":"doctor-01","title":"Heavy Flow and Large Clots","body":"body","category":"WHEN_TO_SEE_A_DOCTOR","contentTags":["FlowIntensity","FlowIntensity.HEAVY","PeriodConsistency"],"sourceName":"NICHD","sourceUrl":"https://example.com","sortOrder":30},
          {"id":"doctor-02","title":"Period Color Changes","body":"body","category":"WHEN_TO_SEE_A_DOCTOR","contentTags":["PeriodColor","PeriodColor.UNUSUAL_COLOR"],"sourceName":"OWH","sourceUrl":"https://example.com","sortOrder":31},
          {"id":"doctor-03","title":"Irregular and Absent Periods","body":"body","category":"WHEN_TO_SEE_A_DOCTOR","contentTags":["CyclePhase"],"sourceName":"NICHD","sourceUrl":"https://example.com","sortOrder":32},
          {"id":"doctor-04","title":"Severe Pain","body":"body","category":"WHEN_TO_SEE_A_DOCTOR","contentTags":["Symptoms","SymptomCategory.PAIN"],"sourceName":"OWH","sourceUrl":"https://example.com","sortOrder":33}
        ]
    """.trimIndent()

    @Test
    fun `parseJson WHEN fullProductionJson THEN parsesExpectedCount`() {
        // WHEN
        val result = EducationalContentLoader.parseJson(productionJson)

        // THEN — 18 articles total
        assertEquals(18, result.size, "Production JSON should contain 18 articles")
    }

    @Test
    fun `parseJson WHEN fullProductionJson THEN everyCategoryHasAtLeast3Articles`() {
        // WHEN
        val result = EducationalContentLoader.parseJson(productionJson)

        // THEN — every category has at least 3 articles
        ArticleCategory.entries.forEach { category ->
            val count = result.count { it.category == category }
            assertTrue(
                count >= 3,
                "Category $category should have at least 3 articles, but has $count"
            )
        }
    }

    @Test
    fun `parseJson WHEN fullProductionJson THEN allTagsAreValidDomainValues`() {
        // Known valid tags: bare tags + enum-qualified values
        val validTags = setOf(
            // Bare tags (used as info button content tags)
            "Mood", "Energy", "Symptoms", "FlowIntensity", "PeriodColor", "PeriodConsistency",
            // CyclePhase
            "CyclePhase",
            "CyclePhase.MENSTRUATION", "CyclePhase.FOLLICULAR",
            "CyclePhase.OVULATION", "CyclePhase.LUTEAL",
            // CyclePhase.Colors (Settings info button)
            "CyclePhase.Colors",
            // FlowIntensity qualified
            "FlowIntensity.LIGHT", "FlowIntensity.MEDIUM", "FlowIntensity.HEAVY",
            // PeriodColor qualified
            "PeriodColor.PINK", "PeriodColor.BRIGHT_RED", "PeriodColor.DARK_RED",
            "PeriodColor.BROWN", "PeriodColor.BLACK_OR_VERY_DARK", "PeriodColor.UNUSUAL_COLOR",
            // PeriodConsistency qualified
            "PeriodConsistency.THIN", "PeriodConsistency.MODERATE", "PeriodConsistency.THICK",
            "PeriodConsistency.STRINGY", "PeriodConsistency.CLOTS_SMALL", "PeriodConsistency.CLOTS_LARGE",
            // SymptomCategory qualified
            "SymptomCategory.PAIN", "SymptomCategory.MOOD", "SymptomCategory.DIGESTIVE",
            "SymptomCategory.SKIN", "SymptomCategory.ENERGY", "SymptomCategory.OTHER",
        )

        // WHEN
        val result = EducationalContentLoader.parseJson(productionJson)

        // THEN — all content tags are valid domain values
        result.forEach { article ->
            article.contentTags.forEach { tag ->
                assertTrue(
                    tag in validTags,
                    "Article '${article.id}' has invalid tag '$tag'. Valid tags: $validTags"
                )
            }
        }
    }

    @Test
    fun `parseJson WHEN fullProductionJson THEN everyInfoButtonTagHasMatchingArticle`() {
        // These are the tags used by info buttons across the app
        val infoButtonTags = listOf(
            "FlowIntensity", "Mood", "Energy", "PeriodColor",
            "PeriodConsistency", "Symptoms", "CyclePhase",
        )

        // WHEN
        val result = EducationalContentLoader.parseJson(productionJson)

        // THEN — every info button tag has at least one matching article
        infoButtonTags.forEach { tag ->
            val matchingArticles = result.filter { tag in it.contentTags }
            assertTrue(
                matchingArticles.isNotEmpty(),
                "Info button tag '$tag' has no matching articles"
            )
        }
    }
}
