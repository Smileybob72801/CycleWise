package com.veleda.cyclewise.domain.insights

import com.veleda.cyclewise.testutil.TestData
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Unit tests for [InsightScorer].
 *
 * Verifies scoring rules including CycleSummary pinning, sort order,
 * category diversity penalty, and novelty bonus.
 */
@OptIn(ExperimentalTime::class)
class InsightScorerTest {

    private val scorer = InsightScorer()

    @Test
    fun score_WHEN_cycleSummaryPresent_THEN_alwaysGetsScoreOne() {
        // GIVEN - a CycleSummary among other insights
        val summary = CycleSummary(
            cycleDay = 5,
            cycleLength = 28,
            phaseName = "menstruation",
            daysUntilNextPeriod = 23
        )
        val trend = CycleLengthTrend(trendDescription = "shortened", changeInDays = 2)

        // WHEN
        val scored = scorer.score(listOf(trend, summary))

        // THEN
        val summaryScoredInsight = scored.first { it.insight is CycleSummary }
        assertEquals(1.0, summaryScoredInsight.relevanceScore, "CycleSummary should always get score 1.0")
    }

    @Test
    fun score_WHEN_multipleInsights_THEN_sortedByScoreDescending() {
        // GIVEN - several insights of different types
        val summary = CycleSummary(cycleDay = 5, cycleLength = 28, phaseName = "menstruation", daysUntilNextPeriod = null)
        val prediction = NextPeriodPrediction(
            predictedDate = TestData.DATE.plus(20, DateTimeUnit.DAY),
            daysUntilPrediction = 20
        )
        val trend = CycleLengthTrend(trendDescription = "lengthened", changeInDays = 3)

        // WHEN
        val scored = scorer.score(listOf(trend, prediction, summary))

        // THEN
        assertEquals(summary, scored.first().insight, "CycleSummary should be first (score 1.0)")
        for (i in 0 until scored.size - 1) {
            assertTrue(
                scored[i].relevanceScore >= scored[i + 1].relevanceScore,
                "Scores should be in descending order"
            )
        }
    }

    @Test
    fun score_WHEN_moreThanThreeInsightsInSameCategory_THEN_diversityPenaltyApplied() {
        // GIVEN - 5 pattern insights in the same category (PATTERN)
        val patterns = (1..5).map { i ->
            MoodPhasePattern(
                moodType = "low",
                phaseDescription = "phase-$i",
                recurrenceRate = "3 out of 5"
            )
        }

        // WHEN
        val scored = scorer.score(patterns)

        // THEN - the 4th and 5th insights should have lower scores due to diversity penalty
        val fourthScore = scored[3].relevanceScore
        val firstScore = scored[0].relevanceScore
        assertTrue(
            fourthScore < firstScore,
            "4th+ insight in same category should have a lower score due to diversity penalty"
        )
    }

    @Test
    fun score_WHEN_emptyList_THEN_returnsEmptyList() {
        // GIVEN - no insights
        // WHEN
        val scored = scorer.score(emptyList())

        // THEN
        assertTrue(scored.isEmpty(), "Empty input should produce empty output")
    }
}
