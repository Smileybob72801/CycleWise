package com.veleda.cyclewise.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Root theme composable for RhythmWise.
 *
 * Applies the brand [MaterialTheme] (color scheme, typography, shapes), provides
 * [LocalDimensions] and [LocalCyclePhasePalette] via [CompositionLocalProvider],
 * and configures the system status-bar appearance to match the current mode.
 *
 * Every screen should be wrapped inside this theme; it is applied once in
 * [com.veleda.cyclewise.ui.CycleWiseAppUI].
 *
 * @param darkTheme Whether dark-mode colors should be used (defaults to system setting).
 * @param cyclePhasePalette Pre-built palette (allows callers to inject user-customized colors).
 *                          Falls back to the theme-appropriate default when `null`.
 * @param content  The composable tree to render inside the theme.
 */
@Composable
fun RhythmWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    cyclePhasePalette: CyclePhasePalette? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val palette = cyclePhasePalette
        ?: if (darkTheme) DarkCyclePhasePalette else LightCyclePhasePalette

    // Set status-bar icon color to match the current theme brightness.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalDimensions provides Dimensions(),
        LocalCyclePhasePalette provides palette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RhythmWiseTypography,
            shapes = RhythmWiseShapes,
            content = content,
        )
    }
}
