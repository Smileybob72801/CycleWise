package com.veleda.cyclewise.domain.insights

/**
 * Wraps an [Insight] with a computed relevance score for sorting.
 *
 * @property insight        The original insight.
 * @property relevanceScore A 0.0-1.0 score indicating how relevant/important this insight is.
 */
data class ScoredInsight(
    val insight: Insight,
    val relevanceScore: Double,
)

/**
 * Assigns dynamic relevance scores to a list of insights.
 *
 * Scoring factors (weights sum to 1.0):
 * - **Statistical significance** (0.35): Based on the insight's priority as a proxy.
 * - **Category diversity** (0.30): Penalizes the 4th+ insight in the same category.
 * - **Novelty** (0.20): First occurrence of a category gets a novelty bonus.
 * - **Base priority** (0.15): Normalizes the insight's priority to 0-1 range.
 *
 * [CycleSummary] always receives a score of 1.0 (pinned to top).
 */
class InsightScorer {

    companion object {
        private const val WEIGHT_SIGNIFICANCE = 0.35
        private const val WEIGHT_DIVERSITY = 0.30
        private const val WEIGHT_NOVELTY = 0.20
        private const val WEIGHT_BASE = 0.15
        private const val MIN_PRIORITY = 90.0
        private const val MAX_PRIORITY = 115.0
        private const val DIVERSITY_PENALTY_THRESHOLD = 3
    }

    /**
     * Scores and sorts insights by relevance descending.
     *
     * @param insights The raw insights to score.
     * @return Scored insights sorted by [ScoredInsight.relevanceScore] descending.
     */
    fun score(insights: List<Insight>): List<ScoredInsight> {
        val categoryCounts = mutableMapOf<InsightCategory, Int>()
        val seenCategories = mutableSetOf<InsightCategory>()

        return insights.map { insight ->
            if (insight is CycleSummary) {
                return@map ScoredInsight(insight, 1.0)
            }

            val categoryCount = categoryCounts.getOrDefault(insight.category, 0) + 1
            categoryCounts[insight.category] = categoryCount

            val isNovel = seenCategories.add(insight.category)

            val significanceScore = normalizedPriority(insight.priority)
            val diversityScore = if (categoryCount > DIVERSITY_PENALTY_THRESHOLD) {
                1.0 / categoryCount
            } else {
                1.0
            }
            val noveltyScore = if (isNovel) 1.0 else 0.5
            val baseScore = normalizedPriority(insight.priority)

            val totalScore = (significanceScore * WEIGHT_SIGNIFICANCE) +
                (diversityScore * WEIGHT_DIVERSITY) +
                (noveltyScore * WEIGHT_NOVELTY) +
                (baseScore * WEIGHT_BASE)

            ScoredInsight(insight, totalScore.coerceIn(0.0, 1.0))
        }.sortedByDescending { it.relevanceScore }
    }

    private fun normalizedPriority(priority: Int): Double =
        ((priority - MIN_PRIORITY) / (MAX_PRIORITY - MIN_PRIORITY)).coerceIn(0.0, 1.0)
}
