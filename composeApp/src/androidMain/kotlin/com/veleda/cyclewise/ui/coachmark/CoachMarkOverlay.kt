package com.veleda.cyclewise.ui.coachmark

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Padding around the cutout highlight in dp. */
private val CUTOUT_PADDING_DP = 4.dp

/** Corner radius for the cutout rounded rect. */
private val CUTOUT_CORNER_DP = 8.dp

/** Vertical gap between the cutout and the tooltip card. */
private val TOOLTIP_GAP_DP = 8.dp

/** Maximum width for the tooltip card. */
private val TOOLTIP_MAX_WIDTH_DP = 320.dp

/** Duration in milliseconds for the long-press skip gesture. */
private const val SKIP_HOLD_DURATION_MS = 2_000

/** Height of the skip progress indicator bar. */
private val SKIP_PROGRESS_HEIGHT = 4.dp

/**
 * Modifier that reports a composable's root-coordinate bounds to [CoachMarkState]
 * so the overlay can draw a cutout around it.
 *
 * Attaches an [onGloballyPositioned] callback that calls [CoachMarkState.registerTarget]
 * with the composable's [boundsInRoot].
 *
 * @param key   The [HintKey] this composable is the target for.
 * @param state The [CoachMarkState] managing the current walkthrough.
 */
fun Modifier.coachMarkTarget(key: HintKey, state: CoachMarkState): Modifier =
    this.onGloballyPositioned { coordinates ->
        state.registerTarget(key, coordinates.boundsInRoot())
    }

/**
 * Full-screen overlay that dims the screen except for a highlighted cutout around
 * the active coach mark's target, with a tooltip card positioned nearby.
 *
 * Only renders when [CoachMarkState.active] is non-null.
 *
 * The overlay tracks its own position within the window root so that target bounds
 * (which are reported in root coordinates) are correctly translated to overlay-local
 * coordinates. This avoids misalignment caused by system bars and scaffold padding.
 *
 * Touch events pass through the scrim so users can interact with the highlighted target
 * (e.g., tapping the Period tab at step 3). Underlying controls are guarded by per-step
 * logic in the host screen (tab locks, pager scroll disable) rather than by the overlay.
 *
 * @param state     The per-screen [CoachMarkState] driving this overlay.
 * @param allDefs   The full walkthrough map, used by [CoachMarkState.advanceOrDismiss].
 * @param onSkipAll Optional callback invoked after the user completes the "Hold to Skip"
 *                  gesture and the overlay has already called [CoachMarkState.skipAll].
 *                  Host screens use this to run additional cleanup (e.g., marking hints
 *                  on other screens as seen, wiping seed data, unlocking the navbar).
 */
@Composable
fun CoachMarkOverlay(
    state: CoachMarkState,
    allDefs: Map<HintKey, CoachMarkDef>,
    onSkipAll: () -> Unit = {},
) {
    val activeCoachMark by state.active.collectAsState()
    val active = activeCoachMark ?: return

    val dims = LocalDimensions.current
    val density = LocalDensity.current

    // Track the overlay's own position and size so we can translate root-coordinate
    // target bounds into overlay-local coordinates.
    var overlayOffset by remember { mutableStateOf(Offset.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }

    val cutoutPadding = with(density) { CUTOUT_PADDING_DP.toPx() }
    val cutoutCorner = with(density) { CUTOUT_CORNER_DP.toPx() }

    // Translate target bounds from root coordinates to overlay-local coordinates.
    val localBounds = Rect(
        left = active.targetBounds.left - overlayOffset.x,
        top = active.targetBounds.top - overlayOffset.y,
        right = active.targetBounds.right - overlayOffset.x,
        bottom = active.targetBounds.bottom - overlayOffset.y,
    )

    // Expand target bounds by the cutout padding.
    val highlightRect = Rect(
        left = localBounds.left - cutoutPadding,
        top = localBounds.top - cutoutPadding,
        right = localBounds.right + cutoutPadding,
        bottom = localBounds.bottom + cutoutPadding,
    )

    // Use overlay's own size for positioning calculations.
    val overlayHeightPx = overlaySize.height.toFloat()
    val overlayWidthPx = overlaySize.width.toFloat()

    // Decide whether the tooltip goes below or above the cutout.
    val tooltipGapPx = with(density) { TOOLTIP_GAP_DP.toPx() }
    val spaceBelow = overlayHeightPx - highlightRect.bottom - tooltipGapPx
    val spaceAbove = highlightRect.top - tooltipGapPx
    val placeBelow = spaceBelow >= spaceAbove

    // Approximate tooltip height for "above" placement (card grows downward from offset).
    val estimatedTooltipHeightPx = with(density) { 160.dp.toPx() }
    val tooltipY = if (placeBelow) {
        highlightRect.bottom + tooltipGapPx
    } else {
        // Place above the cutout, accounting for the card's height.
        (highlightRect.top - tooltipGapPx - estimatedTooltipHeightPx).coerceAtLeast(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                overlayOffset = coordinates.positionInRoot()
                overlaySize = coordinates.size
            },
    ) {
        // Scrim with cutout — purely visual; touch events pass through to the host screen.
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            drawScrimWithCutout(highlightRect, cutoutCorner)
        }

        // Tooltip card — pre-compute positions to avoid referencing unavailable scope properties.
        val tooltipMaxWidthPx = with(density) { TOOLTIP_MAX_WIDTH_DP.toPx() }
        val marginPx = with(density) { dims.md.toPx() }
        val tooltipX = highlightRect.left
            .coerceIn(marginPx, (overlayWidthPx - tooltipMaxWidthPx - marginPx).coerceAtLeast(marginPx))

        ElevatedCard(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = tooltipX.roundToInt(),
                        y = tooltipY.roundToInt(),
                    )
                }
                .widthIn(max = TOOLTIP_MAX_WIDTH_DP)
                .padding(horizontal = dims.md),
        ) {
            Column(modifier = Modifier.padding(dims.md)) {
                Text(
                    text = stringResource(active.def.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(active.def.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = dims.sm),
                )

                // Button row: "Hold to skip" on the start, optional skip + primary on the end.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dims.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HoldToSkipButton(
                        onSkip = {
                            state.skipAll(allDefs)
                            onSkipAll()
                        },
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (active.def.skipButtonRes != null) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            TextButton(
                                onClick = {
                                    state.skipToKey(active.def.skipTargetKey!!, allDefs)
                                    Toast.makeText(
                                        context,
                                        context.getString(active.def.skipToastRes!!),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                },
                            ) {
                                Text(
                                    text = stringResource(active.def.skipButtonRes),
                                    maxLines = 1,
                                )
                            }
                        }

                        TextButton(
                            onClick = { state.advanceOrDismiss(allDefs) },
                        ) {
                            Text(
                                text = stringResource(active.def.dismissLabelRes),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A button that requires a sustained ~2-second press to activate.
 *
 * Shows a linear progress indicator that fills while the user holds down
 * the button. Releasing early resets progress. When the progress reaches
 * 100%, [onSkip] is called to terminate the entire walkthrough.
 *
 * @param onSkip   Callback invoked when the hold completes successfully.
 * @param modifier [Modifier] applied to the root [Column].
 */
@Composable
private fun HoldToSkipButton(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.coach_mark_hold_to_skip),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            // Wait for a finger to press down.
                            awaitFirstDown(requireUnconsumed = false)

                            // Animate progress from current value to 1f.
                            val animationJob = scope.launch {
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = SKIP_HOLD_DURATION_MS,
                                        easing = LinearEasing,
                                    ),
                                )
                            }

                            // Wait until the user lifts or cancels.
                            val up = waitForUpOrCancellation()

                            animationJob.cancel()

                            if (up != null && progress.value >= 1f) {
                                // Hold completed — skip the walkthrough.
                                onSkip()
                            } else {
                                // Released early — reset progress.
                                scope.launch { progress.snapTo(0f) }
                            }
                        }
                    }
                },
        )

        LinearProgressIndicator(
            progress = { progress.value },
            modifier = Modifier
                .padding(top = 2.dp)
                .height(SKIP_PROGRESS_HEIGHT)
                .clip(RoundedCornerShape(SKIP_PROGRESS_HEIGHT / 2)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/**
 * Draws a semi-transparent scrim over the entire canvas with a transparent rounded-rect
 * cutout at [highlightRect].
 *
 * Uses [PathFillType.EvenOdd] to draw a single donut-shaped path (full screen minus
 * cutout) instead of drawing a full scrim and then clearing a hole. This avoids the
 * need for [androidx.compose.ui.graphics.CompositingStrategy.Offscreen] on the parent,
 * which caused [androidx.compose.ui.graphics.BlendMode.Clear] to erase underlying
 * screen content along with the scrim.
 */
private fun DrawScope.drawScrimWithCutout(highlightRect: Rect, cornerRadius: Float) {
    val scrimPath = Path().apply {
        addRect(Rect(0f, 0f, size.width, size.height))
        addRoundRect(
            RoundRect(
                rect = highlightRect,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            )
        )
        fillType = PathFillType.EvenOdd
    }
    drawPath(path = scrimPath, color = Color.Black.copy(alpha = 0.6f))
}
