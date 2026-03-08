package com.veleda.cyclewise.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Centralized "Velvet Garden" color palette for the RhythmWise brand.
 *
 * Every color used by the Material 3 [lightColorScheme]/[darkColorScheme] is sourced
 * from [Light] or [Dark] respectively. **To swap the app palette, update only the
 * hex values in this object** — no other file references raw color literals.
 *
 * Cycle-phase colors ([CyclePhaseColors]) are intentionally independent of this
 * palette because they carry domain-specific meaning that must remain stable.
 */
object AppPalette {

    /** Light-mode Velvet Garden colors. */
    object Light {
        val Primary = Color(0xFF8E6C88)
        val OnPrimary = Color(0xFFFFFFFF)
        val PrimaryContainer = Color(0xFFD8B8D2)
        val OnPrimaryContainer = Color(0xFF3A1F35)
        val Secondary = Color(0xFF6A8E72)
        val OnSecondary = Color(0xFFFFFFFF)
        val SecondaryContainer = Color(0xFFB4D9BC)
        val OnSecondaryContainer = Color(0xFF1E3824)
        val Tertiary = Color(0xFFB5808E)
        val OnTertiary = Color(0xFFFFFFFF)
        val TertiaryContainer = Color(0xFFE8C4CE)
        val OnTertiaryContainer = Color(0xFF4A1F2C)
        val Surface = Color(0xFFFFF7F3)
        val OnSurface = Color(0xFF1D1B1B)
        val SurfaceVariant = Color(0xFFF2E4E8)
        val OnSurfaceVariant = Color(0xFF504348)
        val Background = Color(0xFFFFF7F3)
        val OnBackground = Color(0xFF1D1B1B)
        val Error = Color(0xFFBA1A1A)
        val OnError = Color(0xFFFFFFFF)
        val Outline = Color(0xFF847378)
        val Accent = Color(0xFFD4A66A)
    }

    /** Dark-mode Velvet Garden colors. */
    object Dark {
        val Primary = Color(0xFFBFA0B8)
        val OnPrimary = Color(0xFF3A1F35)
        val PrimaryContainer = Color(0xFF8E6C88)
        val OnPrimaryContainer = Color(0xFFF3DAED)
        val Secondary = Color(0xFF96BF9E)
        val OnSecondary = Color(0xFF1E3824)
        val SecondaryContainer = Color(0xFF6A8E72)
        val OnSecondaryContainer = Color(0xFFD8E8D9)
        val Tertiary = Color(0xFFD4A5B0)
        val OnTertiary = Color(0xFF4A1F2C)
        val TertiaryContainer = Color(0xFFB5808E)
        val OnTertiaryContainer = Color(0xFFFFD9E1)
        val Surface = Color(0xFF1A1016)
        val OnSurface = Color(0xFFEAE0E1)
        val SurfaceVariant = Color(0xFF3A2C32)
        val OnSurfaceVariant = Color(0xFFD6C2C8)
        val Background = Color(0xFF1A1016)
        val OnBackground = Color(0xFFEAE0E1)
        val Error = Color(0xFFFFB4AB)
        val OnError = Color(0xFF690005)
        val Outline = Color(0xFF9E8C91)
        val Accent = Color(0xFFE8C088)
    }
}

// ── Color schemes ────────────────────────────────────────────────────

/** Light Material 3 [androidx.compose.material3.ColorScheme] for the RhythmWise brand. */
internal val LightColorScheme = lightColorScheme(
    primary = AppPalette.Light.Primary,
    onPrimary = AppPalette.Light.OnPrimary,
    primaryContainer = AppPalette.Light.PrimaryContainer,
    onPrimaryContainer = AppPalette.Light.OnPrimaryContainer,
    secondary = AppPalette.Light.Secondary,
    onSecondary = AppPalette.Light.OnSecondary,
    secondaryContainer = AppPalette.Light.SecondaryContainer,
    onSecondaryContainer = AppPalette.Light.OnSecondaryContainer,
    tertiary = AppPalette.Light.Tertiary,
    onTertiary = AppPalette.Light.OnTertiary,
    tertiaryContainer = AppPalette.Light.TertiaryContainer,
    onTertiaryContainer = AppPalette.Light.OnTertiaryContainer,
    background = AppPalette.Light.Background,
    onBackground = AppPalette.Light.OnBackground,
    surface = AppPalette.Light.Surface,
    onSurface = AppPalette.Light.OnSurface,
    surfaceVariant = AppPalette.Light.SurfaceVariant,
    onSurfaceVariant = AppPalette.Light.OnSurfaceVariant,
    error = AppPalette.Light.Error,
    onError = AppPalette.Light.OnError,
    outline = AppPalette.Light.Outline,
)

/** Dark Material 3 [androidx.compose.material3.ColorScheme] for the RhythmWise brand. */
internal val DarkColorScheme = darkColorScheme(
    primary = AppPalette.Dark.Primary,
    onPrimary = AppPalette.Dark.OnPrimary,
    primaryContainer = AppPalette.Dark.PrimaryContainer,
    onPrimaryContainer = AppPalette.Dark.OnPrimaryContainer,
    secondary = AppPalette.Dark.Secondary,
    onSecondary = AppPalette.Dark.OnSecondary,
    secondaryContainer = AppPalette.Dark.SecondaryContainer,
    onSecondaryContainer = AppPalette.Dark.OnSecondaryContainer,
    tertiary = AppPalette.Dark.Tertiary,
    onTertiary = AppPalette.Dark.OnTertiary,
    tertiaryContainer = AppPalette.Dark.TertiaryContainer,
    onTertiaryContainer = AppPalette.Dark.OnTertiaryContainer,
    background = AppPalette.Dark.Background,
    onBackground = AppPalette.Dark.OnBackground,
    surface = AppPalette.Dark.Surface,
    onSurface = AppPalette.Dark.OnSurface,
    surfaceVariant = AppPalette.Dark.SurfaceVariant,
    onSurfaceVariant = AppPalette.Dark.OnSurfaceVariant,
    error = AppPalette.Dark.Error,
    onError = AppPalette.Dark.OnError,
    outline = AppPalette.Dark.Outline,
)

// ── Semantic app-level colors ────────────────────────────────────────

/**
 * Extra semantic colors used across the app that sit outside the Material 3 [ColorScheme].
 *
 * Access these directly; they are theme-independent constants.
 */
object RhythmWiseColors {
    /**
     * Accent tint used for filled star rating icons (mood, energy, libido).
     *
     * References [AppPalette.Light.Accent] for a warm gold that harmonizes with
     * the Velvet Garden palette. The same value works acceptably in both light
     * and dark modes because star icons sit on surface backgrounds.
     */
    val StarGold: Color = AppPalette.Light.Accent
}
