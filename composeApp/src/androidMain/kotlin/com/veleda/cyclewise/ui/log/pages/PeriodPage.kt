package com.veleda.cyclewise.ui.log.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodConsistency
import com.veleda.cyclewise.ui.coachmark.CoachMarkState
import com.veleda.cyclewise.ui.coachmark.HintKey
import com.veleda.cyclewise.ui.coachmark.coachMarkTarget
import com.veleda.cyclewise.ui.log.components.SectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Daily log page for period tracking: toggle, flow intensity, color, and consistency.
 *
 * Displays a period on/off toggle; when enabled, reveals [SectionCard]s for
 * flow intensity, period color, and period consistency selectors. Each section
 * includes an info button for educational content. Integrates with the
 * coach-mark walkthrough to highlight the period toggle.
 *
 * @param isPeriodDay Whether the current day is marked as a period day.
 * @param flowIntensity Currently selected flow intensity, or `null` if unset.
 * @param periodColor Currently selected period color, or `null` if unset.
 * @param periodConsistency Currently selected period consistency, or `null` if unset.
 * @param onPeriodToggled Callback when the user toggles the period switch.
 * @param onFlowChanged Callback when the user selects a flow intensity (or `null` to deselect).
 * @param onColorChanged Callback when the user selects a period color (or `null` to deselect).
 * @param onConsistencyChanged Callback when the user selects a consistency (or `null` to deselect).
 * @param onDone Callback when the user taps the "Done" button to return to the Tracker.
 * @param onShowEducationalSheet Callback to display educational content for the given tag.
 * @param coachMarkState Optional coach-mark state for walkthrough integration.
 * @param activeHintKey The currently active walkthrough hint key, or `null` when no
 *        walkthrough is running. Used to disable the toggle during non-target steps.
 */
@Composable
internal fun PeriodPage(
    isPeriodDay: Boolean,
    flowIntensity: FlowIntensity?,
    periodColor: PeriodColor?,
    periodConsistency: PeriodConsistency?,
    onPeriodToggled: (Boolean) -> Unit,
    onFlowChanged: (FlowIntensity?) -> Unit,
    onColorChanged: (PeriodColor?) -> Unit,
    onConsistencyChanged: (PeriodConsistency?) -> Unit,
    onDone: () -> Unit = {},
    onShowEducationalSheet: (String) -> Unit,
    coachMarkState: CoachMarkState? = null,
    activeHintKey: HintKey? = null,
) {
    val dims = LocalDimensions.current
    val toggleEnabled = activeHintKey == null || activeHintKey == HintKey.DAILY_LOG_PERIOD_TOGGLE
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.md),
        verticalArrangement = Arrangement.spacedBy(dims.md),
    ) {
        Spacer(Modifier.height(dims.sm))

        // Period toggle
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = if (coachMarkState != null) {
                Modifier.coachMarkTarget(HintKey.DAILY_LOG_PERIOD_TOGGLE, coachMarkState)
            } else {
                Modifier
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dims.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.daily_log_period_toggle),
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(
                    checked = isPeriodDay,
                    onCheckedChange = onPeriodToggled,
                    enabled = toggleEnabled,
                    modifier = Modifier.testTag("period_toggle"),
                )
            }
        }

        AnimatedVisibility(
            visible = isPeriodDay,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(dims.md)) {
                SectionCard(
                    title = stringResource(R.string.daily_log_flow_title),
                    icon = Icons.Outlined.WaterDrop,
                    onInfoClick = { onShowEducationalSheet("FlowIntensity") },
                ) {
                    FlowIntensitySelector(
                        selectedIntensity = flowIntensity,
                        onSelectionChanged = onFlowChanged,
                    )
                }

                SectionCard(
                    title = stringResource(R.string.period_color_section_title),
                    icon = Icons.Outlined.WaterDrop,
                    onInfoClick = { onShowEducationalSheet("PeriodColor") },
                ) {
                    PeriodColorSelector(
                        selectedColor = periodColor,
                        onSelectionChanged = onColorChanged,
                    )
                }

                SectionCard(
                    title = stringResource(R.string.period_consistency_section_title),
                    icon = Icons.Outlined.WaterDrop,
                    onInfoClick = { onShowEducationalSheet("PeriodConsistency") },
                ) {
                    PeriodConsistencySelector(
                        selectedConsistency = periodConsistency,
                        onSelectionChanged = onConsistencyChanged,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isPeriodDay,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dims.xl),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.daily_log_period_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        FilledTonalButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.daily_log_done_button))
        }

        Spacer(Modifier.height(dims.xl))
    }
}

/**
 * Horizontal row of [FilterChip]s for selecting a [FlowIntensity] value.
 *
 * Tapping an already-selected chip deselects it (sends `null`).
 *
 * @param selectedIntensity Currently selected intensity, or `null` if none.
 * @param onSelectionChanged Callback with the new selection (or `null` on deselect).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FlowIntensitySelector(
    selectedIntensity: FlowIntensity?,
    onSelectionChanged: (FlowIntensity?) -> Unit
) {
    val options = FlowIntensity.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm)
    ) {
        for (intensity in options) {
            FilterChip(
                selected = selectedIntensity == intensity,
                onClick = {
                    val newSelection = if (selectedIntensity == intensity) null else intensity
                    onSelectionChanged(newSelection)
                },
                label = { Text(intensity.name.replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

/**
 * Selector for [PeriodColor] using wrapping filter chips.
 * Tapping an already-selected chip deselects it (sends null).
 *
 * @param selectedColor Currently selected color, or null.
 * @param onSelectionChanged Callback with the new selection (or null on deselect).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PeriodColorSelector(
    selectedColor: PeriodColor?,
    onSelectionChanged: (PeriodColor?) -> Unit
) {
    val labels = mapOf(
        PeriodColor.PINK to stringResource(R.string.period_color_pink),
        PeriodColor.BRIGHT_RED to stringResource(R.string.period_color_bright_red),
        PeriodColor.DARK_RED to stringResource(R.string.period_color_dark_red),
        PeriodColor.BROWN to stringResource(R.string.period_color_brown),
        PeriodColor.BLACK_OR_VERY_DARK to stringResource(R.string.period_color_black),
        PeriodColor.UNUSUAL_COLOR to stringResource(R.string.period_color_unusual)
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm)
    ) {
        for ((color, label) in labels) {
            FilterChip(
                selected = selectedColor == color,
                onClick = {
                    val newSelection = if (selectedColor == color) null else color
                    onSelectionChanged(newSelection)
                },
                label = { Text(label) }
            )
        }
    }
}

/**
 * Selector for [PeriodConsistency] using wrapping filter chips.
 * Tapping an already-selected chip deselects it (sends null).
 *
 * @param selectedConsistency Currently selected consistency, or null.
 * @param onSelectionChanged Callback with the new selection (or null on deselect).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PeriodConsistencySelector(
    selectedConsistency: PeriodConsistency?,
    onSelectionChanged: (PeriodConsistency?) -> Unit
) {
    val labels = mapOf(
        PeriodConsistency.THIN to stringResource(R.string.period_consistency_thin),
        PeriodConsistency.MODERATE to stringResource(R.string.period_consistency_moderate),
        PeriodConsistency.THICK to stringResource(R.string.period_consistency_thick),
        PeriodConsistency.STRINGY to stringResource(R.string.period_consistency_stringy),
        PeriodConsistency.CLOTS_SMALL to stringResource(R.string.period_consistency_clots_small),
        PeriodConsistency.CLOTS_LARGE to stringResource(R.string.period_consistency_clots_large)
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm)
    ) {
        for ((consistency, label) in labels) {
            FilterChip(
                selected = selectedConsistency == consistency,
                onClick = {
                    val newSelection = if (selectedConsistency == consistency) null else consistency
                    onSelectionChanged(newSelection)
                },
                label = { Text(label) }
            )
        }
    }
}
