package com.veleda.cyclewise.ui.log

import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.coachmark.CoachMarkDef
import com.veleda.cyclewise.ui.coachmark.HintKey

/**
 * Four-step guided walkthrough shown on the Daily Log screen the first time a user
 * opens it after initial passphrase setup.
 *
 * The chain progresses:
 * 1. **Welcome** — highlights the date header and introduces the daily log concept.
 * 2. **Explore Tabs** — highlights the tab row and encourages exploring different tabs.
 * 3. **Period Tab** — highlights the Period tab specifically and explains its purpose.
 * 4. **Period Toggle** — highlights the period toggle card and explains how to start a cycle.
 *
 * Each step links to the next via [CoachMarkDef.nextKey]. The last step has `nextKey = null`.
 */
val DAILY_LOG_HINTS: Map<HintKey, CoachMarkDef> = mapOf(
    HintKey.DAILY_LOG_WELCOME to CoachMarkDef(
        key = HintKey.DAILY_LOG_WELCOME,
        titleRes = R.string.hint_daily_log_welcome_title,
        bodyRes = R.string.hint_daily_log_welcome_body,
        nextKey = HintKey.DAILY_LOG_EXPLORE_TABS,
        dismissLabelRes = R.string.coach_mark_next,
    ),
    HintKey.DAILY_LOG_EXPLORE_TABS to CoachMarkDef(
        key = HintKey.DAILY_LOG_EXPLORE_TABS,
        titleRes = R.string.hint_daily_log_explore_tabs_title,
        bodyRes = R.string.hint_daily_log_explore_tabs_body,
        nextKey = HintKey.DAILY_LOG_PERIOD_TAB,
        dismissLabelRes = R.string.coach_mark_next,
    ),
    HintKey.DAILY_LOG_PERIOD_TAB to CoachMarkDef(
        key = HintKey.DAILY_LOG_PERIOD_TAB,
        titleRes = R.string.hint_daily_log_period_tab_title,
        bodyRes = R.string.hint_daily_log_period_tab_body,
        nextKey = HintKey.DAILY_LOG_PERIOD_TOGGLE,
        dismissLabelRes = R.string.coach_mark_next,
    ),
    HintKey.DAILY_LOG_PERIOD_TOGGLE to CoachMarkDef(
        key = HintKey.DAILY_LOG_PERIOD_TOGGLE,
        titleRes = R.string.hint_daily_log_period_toggle_title,
        bodyRes = R.string.hint_daily_log_period_toggle_body,
        nextKey = null,
        dismissLabelRes = R.string.coach_mark_got_it,
    ),
)
