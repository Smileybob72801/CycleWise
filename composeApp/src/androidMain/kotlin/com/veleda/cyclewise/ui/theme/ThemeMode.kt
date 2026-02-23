package com.veleda.cyclewise.ui.theme

/**
 * User-selectable theme mode, persisted to DataStore as a [key] string.
 *
 * - [SYSTEM] — follow the device's dark/light setting (default).
 * - [LIGHT]  — always use the light color scheme.
 * - [DARK]   — always use the dark color scheme.
 */
enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        /** Resolves a [key] back to a [ThemeMode], falling back to [SYSTEM] for unknown values. */
        fun fromKey(key: String): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
