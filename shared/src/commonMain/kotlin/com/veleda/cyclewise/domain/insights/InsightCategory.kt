package com.veleda.cyclewise.domain.insights

/**
 * Broad classification for insight types, used for grouping in the progressive
 * disclosure UI and for category-diversity scoring.
 */
enum class InsightCategory {
    /** Forecasts of future events (e.g., next period date, symptom recurrence). */
    PREDICTION,
    /** Detected recurring correlations between data and cycle phase. */
    PATTERN,
    /** Cross-variable statistical correlations (e.g., mood vs. energy). */
    CORRELATION,
    /** Directional changes over time (e.g., cycle length trend). */
    TREND,
    /** Aggregate summaries of recorded data (e.g., average cycle length, top symptoms). */
    SUMMARY,
}
