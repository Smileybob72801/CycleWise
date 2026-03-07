package com.veleda.cyclewise.ui.coachmark

/**
 * Unique identifier for each coach mark hint in the app.
 *
 * Each value maps to a single [CoachMarkDef] in a walkthrough definition.
 * Designed to grow as future screens add their own hints.
 */
enum class HintKey {
    // Daily Log walkthrough (10 steps)
    DAILY_LOG_WELCOME,
    DAILY_LOG_MOOD,
    DAILY_LOG_ENERGY,
    DAILY_LOG_WATER,
    DAILY_LOG_EXPLORE_TABS,
    DAILY_LOG_PERIOD_TAB,
    DAILY_LOG_PERIOD_TOGGLE,
    DAILY_LOG_SYMPTOMS_TAB,
    DAILY_LOG_MEDICATIONS_TAB,
    DAILY_LOG_NOTES_TAB,

    // Tracker walkthrough (7 steps)
    TRACKER_WELCOME,
    TRACKER_NAV,
    TRACKER_PHASE_LEGEND,
    TRACKER_LONG_PRESS,
    TRACKER_DRAG,
    TRACKER_ADJUST,
    TRACKER_TAP_DAY,
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
 * @property skipButtonRes   Optional string resource for a secondary "skip" button label.
 *                           When non-null, the overlay renders a skip button alongside the
 *                           primary advance button. Used to let users bypass period-specific
 *                           steps (e.g., "I don't have periods").
 * @property skipTargetKey   The [HintKey] to jump to when the skip button is pressed.
 *                           Only meaningful when [skipButtonRes] is non-null.
 * @property skipToastRes    Optional string resource for a toast shown after skipping.
 *                           Only meaningful when [skipButtonRes] is non-null.
 * @property requiresAction  When `true`, the step is a task step: the "Next" / "Got it"
 *                           button is hidden, and the overlay allows touches through to
 *                           the target so the user can perform the required action. The
 *                           host screen is responsible for calling [CoachMarkState.advanceOrDismiss]
 *                           when the action completes. Defaults to `false` (informational step).
 */
data class CoachMarkDef(
    val key: HintKey,
    val titleRes: Int,
    val bodyRes: Int,
    val nextKey: HintKey?,
    val dismissLabelRes: Int,
    val skipButtonRes: Int? = null,
    val skipTargetKey: HintKey? = null,
    val skipToastRes: Int? = null,
    val requiresAction: Boolean = false,
)
