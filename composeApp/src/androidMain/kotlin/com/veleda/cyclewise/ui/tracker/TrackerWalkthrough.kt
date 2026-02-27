package com.veleda.cyclewise.ui.tracker

import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.coachmark.CoachMarkDef
import com.veleda.cyclewise.ui.coachmark.HintKey

/**
 * Seven-step guided walkthrough shown on the Tracker screen when the user first
 * navigates to it (typically after completing the Daily Log walkthrough).
 *
 * The chain progresses:
 * 1. **Welcome** — highlights the calendar grid and introduces the tracker.
 * 2. **Nav** — highlights the month navigation row (arrows + "Today" button).
 * 3. **Phase Legend** — highlights the cycle-phase colour legend.
 * 4. **Long Press** — explains long-press to mark/toggle a period day.
 * 5. **Drag** — explains drag-to-select a range of period days.
 * 6. **Adjust** — explains how to shorten or extend a period by dragging edges.
 * 7. **Tap Day** — explains tapping a day to view a log summary sheet.
 *
 * Each step links to the next via [CoachMarkDef.nextKey]. The last step has `nextKey = null`.
 */
val TRACKER_HINTS: Map<HintKey, CoachMarkDef> = mapOf(
    HintKey.TRACKER_WELCOME to CoachMarkDef(
        key = HintKey.TRACKER_WELCOME,
        titleRes = R.string.hint_tracker_welcome_title,
        bodyRes = R.string.hint_tracker_welcome_body,
        nextKey = HintKey.TRACKER_NAV,
        dismissLabelRes = R.string.coach_mark_next,
    ),
    HintKey.TRACKER_NAV to CoachMarkDef(
        key = HintKey.TRACKER_NAV,
        titleRes = R.string.hint_tracker_nav_title,
        bodyRes = R.string.hint_tracker_nav_body,
        nextKey = HintKey.TRACKER_PHASE_LEGEND,
        dismissLabelRes = R.string.coach_mark_next,
    ),
    HintKey.TRACKER_PHASE_LEGEND to CoachMarkDef(
        key = HintKey.TRACKER_PHASE_LEGEND,
        titleRes = R.string.hint_tracker_phase_legend_title,
        bodyRes = R.string.hint_tracker_phase_legend_body,
        nextKey = HintKey.TRACKER_LONG_PRESS,
        dismissLabelRes = R.string.coach_mark_next,
    ),
    HintKey.TRACKER_LONG_PRESS to CoachMarkDef(
        key = HintKey.TRACKER_LONG_PRESS,
        titleRes = R.string.hint_tracker_long_press_title,
        bodyRes = R.string.hint_tracker_long_press_body,
        nextKey = HintKey.TRACKER_DRAG,
        dismissLabelRes = R.string.coach_mark_next,
    ),
    HintKey.TRACKER_DRAG to CoachMarkDef(
        key = HintKey.TRACKER_DRAG,
        titleRes = R.string.hint_tracker_drag_title,
        bodyRes = R.string.hint_tracker_drag_body,
        nextKey = HintKey.TRACKER_ADJUST,
        dismissLabelRes = R.string.coach_mark_next,
    ),
    HintKey.TRACKER_ADJUST to CoachMarkDef(
        key = HintKey.TRACKER_ADJUST,
        titleRes = R.string.hint_tracker_adjust_title,
        bodyRes = R.string.hint_tracker_adjust_body,
        nextKey = HintKey.TRACKER_TAP_DAY,
        dismissLabelRes = R.string.coach_mark_next,
    ),
    HintKey.TRACKER_TAP_DAY to CoachMarkDef(
        key = HintKey.TRACKER_TAP_DAY,
        titleRes = R.string.hint_tracker_tap_day_title,
        bodyRes = R.string.hint_tracker_tap_day_body,
        nextKey = null,
        dismissLabelRes = R.string.coach_mark_got_it,
    ),
)
