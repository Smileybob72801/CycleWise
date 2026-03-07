package com.veleda.cyclewise.ui.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.ui.theme.CyclePhasePalette
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Horizontal legend showing cycle-phase colours as compact chip-style entries.
 *
 * Placed between the day-of-week header and the calendar grid so users
 * can identify what each background tint represents. Only visible phases
 * (as configured in settings) are shown; Period is always displayed.
 *
 * @param palette      The current [CyclePhasePalette] providing per-phase dot colors.
 * @param phaseVisible Map of each [CyclePhase] to its visibility flag.
 */
@Composable
internal fun PhaseLegend(
    palette: CyclePhasePalette,
    phaseVisible: Map<CyclePhase, Boolean> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dims.md, vertical = dims.xs),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendChip(
            color = palette.menstruation.dot,
            label = stringResource(R.string.phase_color_period_label)
        )
        if (phaseVisible[CyclePhase.FOLLICULAR] != false) {
            LegendChip(
                color = palette.follicular.dot,
                label = stringResource(R.string.phase_color_follicular_label)
            )
        }
        if (phaseVisible[CyclePhase.OVULATION] != false) {
            LegendChip(
                color = palette.ovulation.dot,
                label = stringResource(R.string.phase_color_ovulation_label)
            )
        }
        if (phaseVisible[CyclePhase.LUTEAL] != false) {
            LegendChip(
                color = palette.luteal.dot,
                label = stringResource(R.string.phase_color_luteal_label)
            )
        }
    }
}

/**
 * Single legend entry rendered as a compact chip: a small coloured swatch
 * followed by a label, wrapped in a [Surface] with `surfaceVariant` background.
 *
 * @param color The fill colour for the swatch.
 * @param label The text displayed next to the swatch.
 */
@Composable
internal fun LegendChip(color: Color, label: String) {
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
            Box(
                modifier = Modifier
                    .size(dims.sm)
                    .background(color, RoundedCornerShape(dims.xs))
            )
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
