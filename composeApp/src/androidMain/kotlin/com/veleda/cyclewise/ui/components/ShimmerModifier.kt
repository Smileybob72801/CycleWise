package com.veleda.cyclewise.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.veleda.cyclewise.ui.utils.isReducedMotionEnabled

/** Duration of one full shimmer sweep in milliseconds. */
private const val SHIMMER_DURATION_MS = 1200

/**
 * Applies a shimmer loading effect to this composable.
 *
 * Draws an animated linear gradient sweep over the content. When the system
 * has reduced motion enabled, shows a static surface-variant background instead.
 */
fun Modifier.shimmer(): Modifier = composed {
    val reducedMotion = isReducedMotionEnabled()
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface

    if (reducedMotion) {
        drawWithContent {
            drawRect(baseColor)
        }
    } else {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(SHIMMER_DURATION_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmer_offset",
        )

        drawWithContent {
            val width = size.width
            val offsetX = width * (progress * 3f - 1f)
            drawRect(baseColor)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(baseColor, highlightColor, baseColor),
                    start = Offset(offsetX, 0f),
                    end = Offset(offsetX + width, size.height),
                ),
            )
        }
    }
}
