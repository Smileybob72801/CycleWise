package com.veleda.cyclewise.ui.tracker

import androidx.compose.ui.graphics.Color
import com.veleda.cyclewise.domain.models.CyclePhase

/**
 * Fixed semantic colors for cycle-phase calendar tinting.
 *
 * These are **not** derived from [androidx.compose.material3.MaterialTheme] because phase colors
 * carry domain-specific meaning (e.g. "teal = follicular") that must remain stable regardless of
 * the device's dynamic color / wallpaper selection. Material 3's "fixed" color roles still shift
 * hue with the dynamic scheme, so custom constants are used instead.
 *
 * Alpha is applied at the call site (e.g. `.copy(alpha = 0.3f)`) so the same values work for
 * both subtle calendar cell tints and solid legend dots.
 */
object CyclePhaseColors {
    /** Light pink-red tint for the Menstruation phase (Material Red 200). */
    val Menstruation = Color(0xFFEF9A9A)

    /** Soft blue-green tint for the Follicular phase (Material Teal 200). */
    val Follicular = Color(0xFF80CBC4)

    /** Warm accent for the Ovulation window (Material Orange 200). */
    val Ovulation = Color(0xFFFFCC80)

    /** Muted lavender for the Luteal phase (Material Deep Purple 200). */
    val Luteal = Color(0xFFB39DDB)
}

/**
 * Returns the background tint color for this cycle phase on the calendar.
 *
 * Every phase maps to a fixed [CyclePhaseColors] constant so that calendar tints remain
 * stable regardless of the device's dynamic Material 3 color scheme.
 */
fun CyclePhase.phaseBackgroundColor(): Color = when (this) {
    CyclePhase.MENSTRUATION -> CyclePhaseColors.Menstruation
    CyclePhase.FOLLICULAR -> CyclePhaseColors.Follicular
    CyclePhase.OVULATION -> CyclePhaseColors.Ovulation
    CyclePhase.LUTEAL -> CyclePhaseColors.Luteal
}

/**
 * Returns a user-facing label for this cycle phase.
 */
fun CyclePhase.displayLabel(): String = when (this) {
    CyclePhase.MENSTRUATION -> "Period"
    CyclePhase.FOLLICULAR -> "Follicular"
    CyclePhase.OVULATION -> "Ovulation"
    CyclePhase.LUTEAL -> "Luteal"
}
