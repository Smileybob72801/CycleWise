package com.veleda.cyclewise.ui.utils

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Returns `true` when the system animator duration scale is 0 (animations off).
 *
 * Used by Lottie and shimmer components to show static content instead of
 * animations, respecting the user's reduced motion preference.
 */
@Composable
fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return scale == 0f
}
