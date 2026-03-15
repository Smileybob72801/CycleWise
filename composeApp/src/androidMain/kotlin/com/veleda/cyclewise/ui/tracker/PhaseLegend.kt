package com.veleda.cyclewise.ui.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.ui.theme.CyclePhasePalette
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Flow-wrapping legend showing cycle-phase colours as compact chip-style entries.
 *
 * Uses [FlowRow] so that on narrow screens (< 360 dp) chips wrap to a second
 * row instead of compressing labels into vertical single-character columns.
 * Placed between the day-of-week header and the calendar grid so users
 * can identify what each background tint represents. Only visible phases
 * (as configured in settings) are shown; Period is always displayed.
 *
 * When [heatmapActive] is true, legend swatches switch from filled dots to outlined
 * rings to visually reflect the fill → border rendering swap on the calendar.
 *
 * @param palette        The current [CyclePhasePalette] providing per-phase dot colors.
 * @param phaseVisible   Map of each [CyclePhase] to its visibility flag.
 * @param heatmapActive  True when a heatmap metric is active — swatches render as outlined rings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PhaseLegend(
    palette: CyclePhasePalette,
    phaseVisible: Map<CyclePhase, Boolean> = emptyMap(),
    heatmapActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dims.md, vertical = dims.xs),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(dims.xs),
    ) {
        LegendChip(
            color = palette.menstruation.dot,
            label = stringResource(R.string.phase_color_period_label),
            outlined = heatmapActive,
        )
        if (phaseVisible[CyclePhase.FOLLICULAR] != false) {
            LegendChip(
                color = palette.follicular.dot,
                label = stringResource(R.string.phase_color_follicular_label),
                outlined = heatmapActive,
            )
        }
        if (phaseVisible[CyclePhase.OVULATION] != false) {
            LegendChip(
                color = palette.ovulation.dot,
                label = stringResource(R.string.phase_color_ovulation_label),
                outlined = heatmapActive,
            )
        }
        if (phaseVisible[CyclePhase.LUTEAL] != false) {
            LegendChip(
                color = palette.luteal.dot,
                label = stringResource(R.string.phase_color_luteal_label),
                outlined = heatmapActive,
            )
        }
    }
}

/**
 * Single legend entry rendered as a compact chip: a small coloured swatch
 * followed by a single-line label, wrapped in a [Surface] with `surfaceVariant`
 * background. The label is constrained to one line and ellipsized if it would
 * overflow, preventing vertical character-by-character wrapping on narrow screens.
 *
 * When [outlined] is true, the swatch renders as a hollow ring with [color] border
 * and transparent fill, mirroring the phase-border rendering on the calendar when
 * a heatmap metric is active.
 *
 * @param color    The fill (or border, when [outlined]) colour for the swatch.
 * @param label    The text displayed next to the swatch.
 * @param outlined When true, the swatch is drawn as an outlined ring instead of a filled dot.
 */
@Composable
internal fun LegendChip(color: Color, label: String, outlined: Boolean = false) {
    val dims = LocalDimensions.current

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.semantics(mergeDescendants = true) { }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dims.sm, vertical = dims.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.xs)
        ) {
            if (outlined) {
                Box(
                    modifier = Modifier
                        .size(dims.sm)
                        .border(dims.xxs, color, RoundedCornerShape(dims.xs))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(dims.sm)
                        .background(color, RoundedCornerShape(dims.xs))
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
