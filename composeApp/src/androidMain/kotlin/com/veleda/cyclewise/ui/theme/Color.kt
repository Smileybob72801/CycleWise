package com.veleda.cyclewise.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Brand palette ────────────────────────────────────────────────────

/** Warm mauve primary. */
private val MauvePrimary = Color(0xFF8B5E83)
private val MauvePrimaryLight = Color(0xFFB08DA9)
private val MauvePrimaryDark = Color(0xFF5F3558)

/** Sage green secondary. */
private val SageSecondary = Color(0xFF5D6F5E)
private val SageSecondaryLight = Color(0xFF8A9D8B)
private val SageSecondaryDark = Color(0xFF334434)

/** Dusty rose tertiary. */
private val DustyRoseTertiary = Color(0xFF7D5260)
private val DustyRoseTertiaryLight = Color(0xFFAA808C)
private val DustyRoseTertiaryDark = Color(0xFF532937)

// ── Surface / background ─────────────────────────────────────────────

private val LightBackground = Color(0xFFFFF8F6)
private val LightSurface = Color(0xFFFFF8F6)
private val LightSurfaceVariant = Color(0xFFF0E0E6)
private val LightOnSurfaceVariant = Color(0xFF504348)

private val DarkBackground = Color(0xFF1A1113)
private val DarkSurface = Color(0xFF1A1113)
private val DarkSurfaceVariant = Color(0xFF3D2E34)
private val DarkOnSurfaceVariant = Color(0xFFD6C2C8)

// ── Error ────────────────────────────────────────────────────────────

private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)

// ── Color schemes ────────────────────────────────────────────────────

/** Light Material 3 [androidx.compose.material3.ColorScheme] for the RhythmWise brand. */
internal val LightColorScheme = lightColorScheme(
    primary = MauvePrimary,
    onPrimary = Color.White,
    primaryContainer = MauvePrimaryLight,
    onPrimaryContainer = MauvePrimaryDark,
    secondary = SageSecondary,
    onSecondary = Color.White,
    secondaryContainer = SageSecondaryLight,
    onSecondaryContainer = SageSecondaryDark,
    tertiary = DustyRoseTertiary,
    onTertiary = Color.White,
    tertiaryContainer = DustyRoseTertiaryLight,
    onTertiaryContainer = DustyRoseTertiaryDark,
    background = LightBackground,
    onBackground = Color(0xFF1D1B1B),
    surface = LightSurface,
    onSurface = Color(0xFF1D1B1B),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ErrorLight,
    onError = OnErrorLight,
    outline = Color(0xFF847378),
)

/** Dark Material 3 [androidx.compose.material3.ColorScheme] for the RhythmWise brand. */
internal val DarkColorScheme = darkColorScheme(
    primary = MauvePrimaryLight,
    onPrimary = MauvePrimaryDark,
    primaryContainer = MauvePrimary,
    onPrimaryContainer = Color(0xFFF3DAED),
    secondary = SageSecondaryLight,
    onSecondary = SageSecondaryDark,
    secondaryContainer = SageSecondary,
    onSecondaryContainer = Color(0xFFD8E8D9),
    tertiary = DustyRoseTertiaryLight,
    onTertiary = DustyRoseTertiaryDark,
    tertiaryContainer = DustyRoseTertiary,
    onTertiaryContainer = Color(0xFFFFD9E1),
    background = DarkBackground,
    onBackground = Color(0xFFEAE0E1),
    surface = DarkSurface,
    onSurface = Color(0xFFEAE0E1),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = ErrorDark,
    onError = OnErrorDark,
    outline = Color(0xFF9E8C91),
)

// ── Semantic app-level colors ────────────────────────────────────────

/**
 * Extra semantic colors used across the app that sit outside the Material 3 [ColorScheme].
 *
 * Access these directly; they are theme-independent constants.
 */
object RhythmWiseColors {
    /** Gold tint used for filled star rating icons (mood, energy, libido). */
    val StarGold: Color = Color(0xFFFFD700)
}
