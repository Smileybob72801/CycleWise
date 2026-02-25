package com.veleda.cyclewise.ui.coachmark

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlin.math.roundToInt

/** Padding around the cutout highlight in dp. */
private val CUTOUT_PADDING_DP = 4.dp

/** Corner radius for the cutout rounded rect. */
private val CUTOUT_CORNER_DP = 8.dp

/** Vertical gap between the cutout and the tooltip card. */
private val TOOLTIP_GAP_DP = 8.dp

/** Maximum width for the tooltip card. */
private val TOOLTIP_MAX_WIDTH_DP = 320.dp

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
 * @param state   The per-screen [CoachMarkState] driving this overlay.
 * @param allDefs The full walkthrough map, used by [CoachMarkState.advanceOrDismiss].
 */
@Composable
fun CoachMarkOverlay(
    state: CoachMarkState,
    allDefs: Map<HintKey, CoachMarkDef>,
) {
    val activeCoachMark by state.active.collectAsState()
    val active = activeCoachMark ?: return

    val dims = LocalDimensions.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }

    val cutoutPadding = with(density) { CUTOUT_PADDING_DP.toPx() }
    val cutoutCorner = with(density) { CUTOUT_CORNER_DP.toPx() }

    // Expand target bounds by the cutout padding.
    val highlightRect = Rect(
        left = active.targetBounds.left - cutoutPadding,
        top = active.targetBounds.top - cutoutPadding,
        right = active.targetBounds.right + cutoutPadding,
        bottom = active.targetBounds.bottom + cutoutPadding,
    )

    // Decide whether the tooltip goes below or above the cutout.
    val tooltipGapPx = with(density) { TOOLTIP_GAP_DP.toPx() }
    val spaceBelow = screenHeight - highlightRect.bottom - tooltipGapPx
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim with cutout
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { state.dismiss() }
        ) {
            drawScrimWithCutout(highlightRect, cutoutCorner)
        }

        // Tooltip card — pre-compute positions to avoid referencing unavailable scope properties.
        val tooltipMaxWidthPx = with(density) { TOOLTIP_MAX_WIDTH_DP.toPx() }
        val marginPx = with(density) { dims.md.toPx() }
        val tooltipX = highlightRect.left
            .coerceIn(marginPx, (screenWidth - tooltipMaxWidthPx - marginPx).coerceAtLeast(marginPx))

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
                TextButton(
                    onClick = { state.advanceOrDismiss(allDefs) },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = dims.sm),
                ) {
                    Text(stringResource(active.def.dismissLabelRes))
                }
            }
        }
    }
}

/**
 * Draws a semi-transparent scrim over the entire canvas with a clear rounded-rect
 * cutout at [highlightRect].
 */
private fun DrawScope.drawScrimWithCutout(highlightRect: Rect, cornerRadius: Float) {
    // Draw the full-screen scrim.
    drawRect(color = Color.Black.copy(alpha = 0.6f))

    // Punch a clear hole for the target.
    val cutoutPath = Path().apply {
        addRoundRect(
            RoundRect(
                rect = highlightRect,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            )
        )
    }
    drawPath(
        path = cutoutPath,
        color = Color.Transparent,
        blendMode = BlendMode.Clear,
    )
}
