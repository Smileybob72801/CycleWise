package com.veleda.cyclewise.ui.coachmark

/**
 * Unique identifier for each coach mark hint in the app.
 *
 * Each value maps to a single [CoachMarkDef] in a walkthrough definition.
 * Designed to grow as future screens add their own hints.
 */
enum class HintKey {
    DAILY_LOG_WELCOME,
    DAILY_LOG_EXPLORE_TABS,
    DAILY_LOG_PERIOD_TAB,
    DAILY_LOG_PERIOD_TOGGLE,
}

/**
 * Definition of a single coach mark tooltip.
 *
 * Coach marks can be chained via [nextKey] to form multi-step walkthroughs.
 * Each step shows a tooltip anchored to a target composable, with a title,
 * body, and a dismiss/advance button.
 *
 * @property key             Unique hint identifier.
 * @property titleRes        String resource ID for the tooltip title.
 * @property bodyRes         String resource ID for the tooltip body.
 * @property nextKey         Next hint in a chain, or `null` if this is the last step.
 * @property dismissLabelRes String resource ID for the dismiss/advance button label.
 */
data class CoachMarkDef(
    val key: HintKey,
    val titleRes: Int,
    val bodyRes: Int,
    val nextKey: HintKey?,
    val dismissLabelRes: Int,
)
