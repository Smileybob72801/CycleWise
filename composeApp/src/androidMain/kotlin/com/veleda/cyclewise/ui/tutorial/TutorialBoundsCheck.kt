package com.veleda.cyclewise.ui.tutorial

import com.veleda.cyclewise.ui.nav.NavRoute

/**
 * Determines whether the tutorial is "out of bounds" and cleanup should run.
 *
 * The tutorial walkthrough only runs on [NavRoute.DailyLogHome] and [NavRoute.Tracker].
 * If seed data is still present (tutorial active) but the user is on any other route,
 * or the walkthrough for the current screen has already completed, the tutorial state
 * is inconsistent and cleanup should fire.
 *
 * @param currentRoute The current Navigation-Compose route string, or `null` if unavailable.
 * @param dailyLogDone `true` when [com.veleda.cyclewise.ui.coachmark.HintKey.DAILY_LOG_NOTES_TAB]
 *                     has been marked seen (DailyLog walkthrough finished).
 *                     On [NavRoute.DailyLogHome] this alone is **not** sufficient — the
 *                     DailyLog→Tracker transition is a normal part of the flow, so cleanup
 *                     only fires when [trackerDone] is also `true`.
 * @param trackerDone  `true` when [com.veleda.cyclewise.ui.coachmark.HintKey.TRACKER_TAP_DAY]
 *                     has been marked seen (Tracker walkthrough finished).
 * @return `true` if tutorial cleanup should run immediately.
 */
internal fun shouldRunTutorialBoundsCleanup(
    currentRoute: String?,
    dailyLogDone: Boolean,
    trackerDone: Boolean,
): Boolean = when (currentRoute) {
    null, NavRoute.Passphrase.route -> false
    NavRoute.DailyLogHome.route -> dailyLogDone && trackerDone
    NavRoute.Tracker.route -> trackerDone
    else -> true
}
