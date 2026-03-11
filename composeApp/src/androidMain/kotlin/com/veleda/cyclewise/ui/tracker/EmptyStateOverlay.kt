package com.veleda.cyclewise.ui.tracker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Minimum fling velocity (px/s) to trigger a snap-to-dismiss. */
private const val FLING_VELOCITY_THRESHOLD = 500f

/** Fraction of container width at which a drag commits to dismiss. */
private const val DISMISS_FRACTION = 0.5f

/** Base alpha for the overlay scrim, using surface color for a gentle tonal veil. */
private const val OVERLAY_SCRIM_ALPHA = 0.7f

/**
 * Full-screen swipeable overlay shown when the tracker has no data.
 *
 * Renders a semi-transparent scrim that blocks all touch events on the tracker
 * content beneath, plus a centered column with a calendar icon and instructional
 * text. The user can dismiss the overlay by:
 * - **Swiping** horizontally in either direction past [DISMISS_FRACTION] of the
 *   container width, or flinging with velocity above [FLING_VELOCITY_THRESHOLD].
 * - **Tapping** anywhere, which auto-animates the content off-screen in the
 *   layout-direction-aware "forward" direction (right for LTR, left for RTL).
 *
 * The scrim alpha decreases proportionally as the content slides off-screen.
 *
 * @param onDismissed Callback invoked after the dismiss animation completes.
 * @param modifier Optional [Modifier] applied to the outermost container.
 */
@Composable
fun EmptyStateOverlay(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current
    val scrimColor = MaterialTheme.colorScheme.surface
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidthPx = constraints.maxWidth.toFloat()

        val swipeProgress = if (containerWidthPx > 0f) {
            (abs(offsetX.value) / containerWidthPx).coerceIn(0f, 1f)
        } else {
            0f
        }
        val scrimAlpha = (1f - swipeProgress) * OVERLAY_SCRIM_ALPHA

        // Scrim layer — blocks all touches on content beneath.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor.copy(alpha = scrimAlpha))
                .testTag("emptyStateScrim")
                .pointerInput(Unit) {
                    // Consume all taps on the scrim so they don't reach the calendar.
                    detectTapGestures { }
                },
        )

        // Swipeable / tappable content card.
        ElevatedCard(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val progress = abs(offsetX.value) / containerWidthPx
                                if (progress >= DISMISS_FRACTION) {
                                    // Commit dismiss in the drag direction.
                                    val target = if (offsetX.value > 0) containerWidthPx else -containerWidthPx
                                    offsetX.animateTo(target, tween(durationMillis = 200))
                                    onDismissed()
                                } else {
                                    // Snap back to center.
                                    offsetX.animateTo(0f, spring())
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch { offsetX.animateTo(0f, spring()) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount)
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        coroutineScope.launch {
                            val target = if (isRtl) -containerWidthPx else containerWidthPx
                            offsetX.animateTo(target, tween(durationMillis = 300))
                            onDismissed()
                        }
                    }
                }
                .testTag("emptyStateContent"),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(dims.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dims.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = stringResource(R.string.tracker_empty_icon_cd),
                    modifier = Modifier.size(dims.iconLg),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.tracker_empty_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.tracker_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
