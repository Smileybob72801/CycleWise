package com.veleda.cyclewise.ui.tracker

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
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
 * Each chip's background colour matches the corresponding calendar phase fill:
 * [CyclePhasePalette.menstruation]`.fill` for Period (opaque, matching calendar
 * period-day cells) and `.fillSubtle` for Follicular, Ovulation, and Luteal
 * (matching the subtle calendar phase tint). When [heatmapActive] is true, chips
 * switch to a neutral background with a coloured border, mirroring how calendar
 * day cells swap phase fills for outline borders in heatmap mode.
 *
 * @param palette        The current [CyclePhasePalette] providing per-phase colours.
 * @param phaseVisible   Map of each [CyclePhase] to its visibility flag.
 * @param heatmapActive  True when a heatmap metric is active — chips render with borders
 *                       instead of filled backgrounds.
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
            fillColor = palette.menstruation.fill,
            borderColor = palette.menstruation.border,
            label = stringResource(R.string.phase_color_period_label),
            outlined = heatmapActive,
        )
        if (phaseVisible[CyclePhase.FOLLICULAR] != false) {
            LegendChip(
                fillColor = palette.follicular.fillSubtle,
                borderColor = palette.follicular.border,
                label = stringResource(R.string.phase_color_follicular_label),
                outlined = heatmapActive,
            )
        }
        if (phaseVisible[CyclePhase.OVULATION] != false) {
            LegendChip(
                fillColor = palette.ovulation.fillSubtle,
                borderColor = palette.ovulation.border,
                label = stringResource(R.string.phase_color_ovulation_label),
                outlined = heatmapActive,
            )
        }
        if (phaseVisible[CyclePhase.LUTEAL] != false) {
            LegendChip(
                fillColor = palette.luteal.fillSubtle,
                borderColor = palette.luteal.border,
                label = stringResource(R.string.phase_color_luteal_label),
                outlined = heatmapActive,
            )
        }
    }
}

/**
 * Single legend entry rendered as a compact chip whose background colour matches
 * the corresponding calendar phase fill.
 *
 * In **normal mode** ([outlined] = false), the chip's [Surface] background is
 * [fillColor] — the same colour the calendar uses for that phase's day cells.
 * Text colour is computed at runtime via [Color.luminance] on the composited
 * background to guarantee contrast regardless of user-customised colours or
 * light/dark mode.
 *
 * In **heatmap mode** ([outlined] = true), the chip reverts to a neutral
 * `surfaceVariant` background with a coloured [borderColor] outline, mirroring
 * how calendar day cells swap phase fills for outline borders when a heatmap
 * metric is active.
 *
 * The label is constrained to one line and ellipsized if it would overflow,
 * preventing vertical character-by-character wrapping on narrow screens.
 *
 * @param fillColor   Background colour for the chip in normal mode.
 * @param borderColor Border colour for the chip in heatmap (outlined) mode.
 * @param label       The phase name displayed inside the chip.
 * @param outlined    When true, renders a bordered outline instead of a filled background.
 */
@Composable
internal fun LegendChip(
    fillColor: Color,
    borderColor: Color,
    label: String,
    outlined: Boolean = false,
) {
    val dims = LocalDimensions.current
    val chipShape = MaterialTheme.shapes.small

    if (outlined) {
        Surface(
            shape = chipShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .semantics(mergeDescendants = true) { }
                .border(dims.xxs, borderColor, chipShape),
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = dims.sm, vertical = dims.xs),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        val surface = MaterialTheme.colorScheme.surface
        val effectiveColor = fillColor.compositeOver(surface)
        val textColor = if (effectiveColor.luminance() < 0.5f) Color.White
            else Color(0xFF1A1113)

        Surface(
            shape = chipShape,
            color = fillColor,
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = dims.sm, vertical = dims.xs),
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
