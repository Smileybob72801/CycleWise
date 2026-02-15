package com.veleda.cyclewise.ui.tracker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
 *
 * Users may override these colors via [AppSettings] phase-color preferences. The constants here
 * serve as defaults and fallbacks.
 */
object CyclePhaseColors {
    /** Default 6-char hex string for the Menstruation phase (Material Red 200). */
    const val DEFAULT_MENSTRUATION_HEX = "EF9A9A"

    /** Default 6-char hex string for the Follicular phase (Material Teal 200). */
    const val DEFAULT_FOLLICULAR_HEX = "80CBC4"

    /** Default 6-char hex string for the Ovulation phase (Material Orange 200). */
    const val DEFAULT_OVULATION_HEX = "FFCC80"

    /** Default 6-char hex string for the Luteal phase (Material Deep Purple 200). */
    const val DEFAULT_LUTEAL_HEX = "B39DDB"

    /** Light pink-red tint for the Menstruation phase (Material Red 200). */
    val Menstruation = Color(0xFFEF9A9A)

    /** Soft blue-green tint for the Follicular phase (Material Teal 200). */
    val Follicular = Color(0xFF80CBC4)

    /** Warm accent for the Ovulation window (Material Orange 200). */
    val Ovulation = Color(0xFFFFCC80)

    /** Muted lavender for the Luteal phase (Material Deep Purple 200). */
    val Luteal = Color(0xFFB39DDB)
}

private val HEX_REGEX = Regex("^[0-9A-Fa-f]{6}$")

/**
 * Parses a 6-character hex string (e.g. `"EF9A9A"`) into a [Color].
 *
 * The input must be exactly 6 hexadecimal characters with no `#` prefix.
 * Returns `null` if the input is blank, the wrong length, or contains non-hex characters.
 */
fun parseHexColor(hex: String): Color? {
    if (!HEX_REGEX.matches(hex)) return null
    val colorLong = hex.toLongOrNull(16) ?: return null
    return Color(0xFF000000 or colorLong)
}

/**
 * Converts a [Color] to an uppercase 6-character hex string (e.g. `"EF9A9A"`).
 *
 * Only the RGB channels are encoded; the alpha channel is discarded.
 */
fun hexFromColor(color: Color): String {
    val argb = color.toArgb()
    return String.format("%06X", argb and 0x00FFFFFF)
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
