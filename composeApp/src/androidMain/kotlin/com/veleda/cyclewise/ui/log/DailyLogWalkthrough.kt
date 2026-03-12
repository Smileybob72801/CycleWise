package com.veleda.cyclewise.ui.log

import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.coachmark.CoachMarkDef
import com.veleda.cyclewise.ui.coachmark.HintKey
import com.veleda.cyclewise.ui.coachmark.walkthroughStepList

/**
 * Ten-step guided walkthrough shown on the Daily Log screen the first time a user
 * opens it after initial passphrase setup.
 *
 * The chain progresses:
 * 1. **Welcome** — highlights the date header and introduces the daily log concept.
 * 2. **Mood** — highlights the Mood section card on the Wellness page.
 * 3. **Energy** — highlights the Energy section card on the Wellness page.
 * 4. **Water** — highlights the Water section card on the Wellness page.
 * 5. **Explore Tabs** — highlights the tab row and encourages exploring different tabs.
 * 6. **Period Tab** — highlights the Period tab; includes a skip button for non-menstruators.
 * 7. **Period Toggle** — highlights the period toggle switch.
 * 8. **Symptoms Tab** — highlights the Symptoms tab.
 * 9. **Medications Tab** — highlights the Medications tab.
 * 10. **Notes Tab** — highlights the Notes & Tags tab (final step).
 *
 * Each step links to the next via [CoachMarkDef.nextKey]. The last step has `nextKey = null`.
 */
/**
 * Ordered list of all [HintKey]s in the Daily Log walkthrough, derived by
 * following the [CoachMarkDef.nextKey] chain from [HintKey.DAILY_LOG_WELCOME].
 *
 * Used by [com.veleda.cyclewise.ui.coachmark.CoachMarkOverlay] to show a
 * progress indicator across the full walkthrough.
 */
val DAILY_LOG_STEP_LIST: List<HintKey> by lazy {
    walkthroughStepList(HintKey.DAILY_LOG_WELCOME, DAILY_LOG_HINTS)
}

val DAILY_LOG_HINTS: Map<HintKey, CoachMarkDef> = mapOf(
    HintKey.DAILY_LOG_WELCOME to CoachMarkDef(
        key = HintKey.DAILY_LOG_WELCOME,
        titleRes = R.string.hint_daily_log_welcome_title,
        bodyRes = R.string.hint_daily_log_welcome_body,
        nextKey = HintKey.DAILY_LOG_MOOD,
        dismissLabelRes = R.string.coach_mark_next,
    ),
    HintKey.DAILY_LOG_MOOD to CoachMarkDef(
        key = HintKey.DAILY_LOG_MOOD,
        titleRes = R.string.hint_daily_log_mood_title,
        bodyRes = R.string.hint_daily_log_mood_body,
        nextKey = HintKey.DAILY_LOG_ENERGY,
        dismissLabelRes = R.string.coach_mark_next,
        requiresAction = true,
    ),
    HintKey.DAILY_LOG_ENERGY to CoachMarkDef(
        key = HintKey.DAILY_LOG_ENERGY,
        titleRes = R.string.hint_daily_log_energy_title,
        bodyRes = R.string.hint_daily_log_energy_body,
        nextKey = HintKey.DAILY_LOG_WATER,
        dismissLabelRes = R.string.coach_mark_next,
        requiresAction = true,
    ),
    HintKey.DAILY_LOG_WATER to CoachMarkDef(
        key = HintKey.DAILY_LOG_WATER,
        titleRes = R.string.hint_daily_log_water_title,
        bodyRes = R.string.hint_daily_log_water_body,
        nextKey = HintKey.DAILY_LOG_EXPLORE_TABS,
        dismissLabelRes = R.string.coach_mark_next,
        requiresAction = true,
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
        skipButtonRes = R.string.coach_mark_no_periods,
        skipTargetKey = HintKey.DAILY_LOG_SYMPTOMS_TAB,
        skipToastRes = R.string.coach_mark_no_periods_toast,
        requiresAction = true,
    ),
    HintKey.DAILY_LOG_PERIOD_TOGGLE to CoachMarkDef(
        key = HintKey.DAILY_LOG_PERIOD_TOGGLE,
        titleRes = R.string.hint_daily_log_period_toggle_title,
        bodyRes = R.string.hint_daily_log_period_toggle_body,
        nextKey = HintKey.DAILY_LOG_SYMPTOMS_TAB,
        dismissLabelRes = R.string.coach_mark_next,
        requiresAction = true,
    ),
    HintKey.DAILY_LOG_SYMPTOMS_TAB to CoachMarkDef(
        key = HintKey.DAILY_LOG_SYMPTOMS_TAB,
        titleRes = R.string.hint_daily_log_symptoms_tab_title,
        bodyRes = R.string.hint_daily_log_symptoms_tab_body,
        nextKey = HintKey.DAILY_LOG_MEDICATIONS_TAB,
        dismissLabelRes = R.string.coach_mark_next,
        requiresAction = true,
    ),
    HintKey.DAILY_LOG_MEDICATIONS_TAB to CoachMarkDef(
        key = HintKey.DAILY_LOG_MEDICATIONS_TAB,
        titleRes = R.string.hint_daily_log_medications_tab_title,
        bodyRes = R.string.hint_daily_log_medications_tab_body,
        nextKey = HintKey.DAILY_LOG_NOTES_TAB,
        dismissLabelRes = R.string.coach_mark_next,
        requiresAction = true,
    ),
    HintKey.DAILY_LOG_NOTES_TAB to CoachMarkDef(
        key = HintKey.DAILY_LOG_NOTES_TAB,
        titleRes = R.string.hint_daily_log_notes_tab_title,
        bodyRes = R.string.hint_daily_log_notes_tab_body,
        nextKey = null,
        dismissLabelRes = R.string.coach_mark_got_it,
    ),
)
