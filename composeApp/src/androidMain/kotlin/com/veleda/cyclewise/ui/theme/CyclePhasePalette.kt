package com.veleda.cyclewise.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.ui.tracker.CyclePhaseColors

/**
 * Color roles for a single cycle phase, derived from one base hue.
 *
 * Each role maps to a specific UI surface so that every phase is represented
 * consistently throughout the app without ad-hoc alpha or manual darkening.
 *
 * @property fill       Opaque background (calendar period days, chips).
 * @property fillSubtle Muted tint (calendar non-period phase cells).
 * @property onFill     Text/icon on top of [fill] — guaranteed contrast.
 * @property dot        Legend dots, small indicators (always opaque).
 * @property border     Outlines, card left-accent borders.
 * @property chartLine  Chart/graph lines, data-visualization strokes.
 */
@Immutable
data class PhaseColors(
    val fill: Color,
    val fillSubtle: Color,
    val onFill: Color,
    val dot: Color,
    val border: Color,
    val chartLine: Color,
)

/**
 * Full palette for all four cycle phases, themed for either light or dark mode.
 *
 * Provided via [LocalCyclePhasePalette] inside [RhythmWiseTheme] so any composable
 * can access phase-specific colors without prop-drilling.
 *
 * @property menstruation Colors for the menstruation (active bleeding) phase.
 * @property follicular   Colors for the follicular phase.
 * @property ovulation    Colors for the ovulation window.
 * @property luteal       Colors for the luteal phase.
 */
@Immutable
data class CyclePhasePalette(
    val menstruation: PhaseColors,
    val follicular: PhaseColors,
    val ovulation: PhaseColors,
    val luteal: PhaseColors,
) {
    /**
     * Returns the [PhaseColors] for the given [CyclePhase] enum value.
     *
     * @param phase The cycle phase to look up.
     * @return Matching [PhaseColors] for [phase].
     */
    fun forPhase(phase: CyclePhase): PhaseColors = when (phase) {
        CyclePhase.MENSTRUATION -> menstruation
        CyclePhase.FOLLICULAR -> follicular
        CyclePhase.OVULATION -> ovulation
        CyclePhase.LUTEAL -> luteal
    }
}

/**
 * Composition-local providing the current [CyclePhasePalette].
 *
 * Set inside [RhythmWiseTheme]; read via `LocalCyclePhasePalette.current`.
 */
val LocalCyclePhasePalette = staticCompositionLocalOf<CyclePhasePalette> {
    error("No CyclePhasePalette provided. Wrap your content in RhythmWiseTheme.")
}

// ── Default palettes ─────────────────────────────────────────────────

// Light-mode "on" colors (dark enough for WCAG contrast on the 200-variant fills)
private val OnMenstruationLight = Color(0xFF4E1515)
private val OnFollicularLight = Color(0xFF004D40)
private val OnOvulationLight = Color(0xFF4E3B00)
private val OnLutealLight = Color(0xFF311B72)

// Light-mode border / chart (300-variant)
private val MenstruationBorderLight = Color(0xFFE57373)
private val FollicularBorderLight = Color(0xFF4DB6AC)
private val OvulationBorderLight = Color(0xFFFFB74D)
private val LutealBorderLight = Color(0xFF9575CD)

/**
 * Default light-mode [CyclePhasePalette].
 *
 * Uses Material 200-variant fills with dark "on" colors for WCAG contrast,
 * and 300-variant borders/chart lines for visual definition against light backgrounds.
 */
internal val LightCyclePhasePalette = CyclePhasePalette(
    menstruation = PhaseColors(
        fill = CyclePhaseColors.Menstruation,
        fillSubtle = CyclePhaseColors.Menstruation.copy(alpha = 0.3f),
        onFill = OnMenstruationLight,
        dot = CyclePhaseColors.Menstruation,
        border = MenstruationBorderLight,
        chartLine = MenstruationBorderLight,
    ),
    follicular = PhaseColors(
        fill = CyclePhaseColors.Follicular,
        fillSubtle = CyclePhaseColors.Follicular.copy(alpha = 0.3f),
        onFill = OnFollicularLight,
        dot = CyclePhaseColors.Follicular,
        border = FollicularBorderLight,
        chartLine = FollicularBorderLight,
    ),
    ovulation = PhaseColors(
        fill = CyclePhaseColors.Ovulation,
        fillSubtle = CyclePhaseColors.Ovulation.copy(alpha = 0.3f),
        onFill = OnOvulationLight,
        dot = CyclePhaseColors.Ovulation,
        border = OvulationBorderLight,
        chartLine = OvulationBorderLight,
    ),
    luteal = PhaseColors(
        fill = CyclePhaseColors.Luteal,
        fillSubtle = CyclePhaseColors.Luteal.copy(alpha = 0.3f),
        onFill = OnLutealLight,
        dot = CyclePhaseColors.Luteal,
        border = LutealBorderLight,
        chartLine = LutealBorderLight,
    ),
)

// Dark-mode "on" colors
private val OnOvulationDark = Color(0xFF1A1113) // dark bg for light amber fill

/**
 * Default dark-mode [CyclePhasePalette].
 *
 * Uses the same base fills as light mode but with reduced subtle-alpha (0.25),
 * white "on" colors for maximum contrast on dark backgrounds, and
 * border/chart-line colors matching the opaque fill.
 */
internal val DarkCyclePhasePalette = CyclePhasePalette(
    menstruation = PhaseColors(
        fill = CyclePhaseColors.Menstruation,
        fillSubtle = CyclePhaseColors.Menstruation.copy(alpha = 0.25f),
        onFill = Color.White,
        dot = CyclePhaseColors.Menstruation,
        border = CyclePhaseColors.Menstruation,
        chartLine = CyclePhaseColors.Menstruation,
    ),
    follicular = PhaseColors(
        fill = CyclePhaseColors.Follicular,
        fillSubtle = CyclePhaseColors.Follicular.copy(alpha = 0.25f),
        onFill = Color.White,
        dot = CyclePhaseColors.Follicular,
        border = CyclePhaseColors.Follicular,
        chartLine = CyclePhaseColors.Follicular,
    ),
    ovulation = PhaseColors(
        fill = CyclePhaseColors.Ovulation,
        fillSubtle = CyclePhaseColors.Ovulation.copy(alpha = 0.25f),
        onFill = OnOvulationDark,
        dot = CyclePhaseColors.Ovulation,
        border = CyclePhaseColors.Ovulation,
        chartLine = CyclePhaseColors.Ovulation,
    ),
    luteal = PhaseColors(
        fill = CyclePhaseColors.Luteal,
        fillSubtle = CyclePhaseColors.Luteal.copy(alpha = 0.25f),
        onFill = Color.White,
        dot = CyclePhaseColors.Luteal,
        border = CyclePhaseColors.Luteal,
        chartLine = CyclePhaseColors.Luteal,
    ),
)

// ── Builder for custom colors ────────────────────────────────────────

/**
 * Builds a [CyclePhasePalette] that respects user-customized base colors.
 *
 * For each phase, if a custom [Color] is present in [customColors] the six roles
 * are derived from that base; otherwise the theme-appropriate default is used.
 *
 * Derivation rules:
 * - **fill** = custom base (opaque)
 * - **fillSubtle** = base with reduced alpha (0.3 light / 0.25 dark)
 * - **onFill** = white when the base luminance is below 0.5, otherwise a dark contrast color
 * - **dot** = same as fill (always opaque)
 * - **border** / **chartLine** = base darkened by 15 % (light) or same as base (dark)
 *
 * @param darkTheme Whether the current configuration is dark mode.
 * @param customColors Optional per-phase custom base colors provided by the user.
 * @return A fully derived [CyclePhasePalette].
 */
fun buildCyclePhasePalette(
    darkTheme: Boolean,
    customColors: Map<CyclePhase, Color>? = null,
): CyclePhasePalette {
    val defaults = if (darkTheme) DarkCyclePhasePalette else LightCyclePhasePalette
    if (customColors.isNullOrEmpty()) return defaults

    fun derivePhase(phase: CyclePhase, default: PhaseColors): PhaseColors {
        val base = customColors[phase] ?: return default
        val subtleAlpha = if (darkTheme) 0.25f else 0.3f
        val onFill = if (base.luminance() < 0.5f) Color.White else Color(0xFF1A1113)
        val accent = if (darkTheme) base else darken(base, 0.15f)
        return PhaseColors(
            fill = base,
            fillSubtle = base.copy(alpha = subtleAlpha),
            onFill = onFill,
            dot = base,
            border = accent,
            chartLine = accent,
        )
    }

    return CyclePhasePalette(
        menstruation = derivePhase(CyclePhase.MENSTRUATION, defaults.menstruation),
        follicular = derivePhase(CyclePhase.FOLLICULAR, defaults.follicular),
        ovulation = derivePhase(CyclePhase.OVULATION, defaults.ovulation),
        luteal = derivePhase(CyclePhase.LUTEAL, defaults.luteal),
    )
}

/**
 * Darkens [color] by [fraction] (0.0–1.0) by scaling each RGB channel toward zero.
 *
 * @param color    The source color.
 * @param fraction How much to darken (e.g. 0.15 = 15 % darker).
 * @return A new [Color] with the same alpha but darkened RGB channels.
 */
internal fun darken(color: Color, fraction: Float): Color {
    val factor = 1f - fraction.coerceIn(0f, 1f)
    return Color(
        red = color.red * factor,
        green = color.green * factor,
        blue = color.blue * factor,
        alpha = color.alpha,
    )
}
