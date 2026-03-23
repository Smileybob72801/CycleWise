package com.veleda.cyclewise.ui.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.CustomTag
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodConsistency
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.utils.toLocalizedDateString
import kotlinx.datetime.LocalDate

/**
 * Bottom-sheet content summarising a single day's log.
 *
 * Displays logged data in [InfoCard] rows with meaningful icons
 * (phase, flow, color, consistency, mood, energy, libido, water, notes),
 * symptom/medication chips, and a "View Full Log" button at the bottom.
 *
 * @param log               The full daily log to display.
 * @param periodId          Associated period ID, or null if the day is not a period day.
 * @param cyclePhase        Computed cycle phase for this date, or null if not determinable.
 * @param symptomLibrary    Library of all symptoms for name resolution.
 * @param medicationLibrary Library of all medications for name resolution.
 * @param customTagLibrary  Library of all custom tags for name resolution.
 * @param waterCups         Number of water cups logged, or null.
 * @param showMood          Whether to display the mood score row (controlled by user setting).
 * @param showEnergy        Whether to display the energy level row (controlled by user setting).
 * @param showLibido        Whether to display the libido score row (controlled by user setting).
 * @param onEditClick       Callback when the user taps the edit button.
 * @param onDeleteClick     Callback when the user taps the delete button.
 * @param onViewFullLogClick Callback when the user taps the "View Full Log" button.
 */
@Composable
internal fun LogSummarySheetContent(
    log: FullDailyLog,
    periodId: String?,
    cyclePhase: CyclePhase? = null,
    symptomLibrary: List<Symptom>,
    medicationLibrary: List<Medication>,
    customTagLibrary: List<CustomTag>,
    waterCups: Int?,
    showMood: Boolean,
    showEnergy: Boolean,
    showLibido: Boolean,
    onEditClick: (LocalDate) -> Unit,
    onDeleteClick: (String) -> Unit,
    onViewFullLogClick: (LocalDate) -> Unit
) {
    val dims = LocalDimensions.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dims.md),
        verticalArrangement = Arrangement.spacedBy(dims.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.tracker_log_for, log.entry.entryDate.toLocalizedDateString()),
                style = MaterialTheme.typography.titleLarge
            )
            Row {
                IconButton(
                    onClick = { onEditClick(log.entry.entryDate) },
                    modifier = Modifier.testTag("edit-log-button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.tracker_edit_log))
                }
                if (periodId != null) {
                    IconButton(
                        onClick = { onDeleteClick(periodId) },
                        modifier = Modifier.testTag("delete-period-button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.tracker_delete_period))
                    }
                }
            }
        }

        HorizontalDivider()

        cyclePhase?.let { phase ->
            InfoCard(
                icon = Icons.Default.CalendarMonth,
                title = stringResource(R.string.tracker_phase_label),
                value = when (phase) {
                    CyclePhase.MENSTRUATION -> stringResource(R.string.phase_color_period_label)
                    CyclePhase.FOLLICULAR -> stringResource(R.string.phase_color_follicular_label)
                    CyclePhase.OVULATION -> stringResource(R.string.phase_color_ovulation_label)
                    CyclePhase.LUTEAL -> stringResource(R.string.phase_color_luteal_label)
                }
            )
        }

        log.periodLog?.flowIntensity?.let {
            InfoCard(
                icon = Icons.Default.Opacity,
                title = stringResource(R.string.tracker_flow_label),
                value = it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
            )
        }

        log.periodLog?.periodColor?.let {
            InfoCard(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.tracker_color_label),
                value = when (it) {
                    PeriodColor.PINK -> stringResource(R.string.period_color_pink)
                    PeriodColor.BRIGHT_RED -> stringResource(R.string.period_color_bright_red)
                    PeriodColor.DARK_RED -> stringResource(R.string.period_color_dark_red)
                    PeriodColor.BROWN -> stringResource(R.string.period_color_brown)
                    PeriodColor.BLACK_OR_VERY_DARK -> stringResource(R.string.period_color_black)
                    PeriodColor.UNUSUAL_COLOR -> stringResource(R.string.period_color_unusual)
                }
            )
        }

        log.periodLog?.periodConsistency?.let {
            InfoCard(
                icon = Icons.Default.Grain,
                title = stringResource(R.string.tracker_consistency_label),
                value = when (it) {
                    PeriodConsistency.THIN -> stringResource(R.string.period_consistency_thin)
                    PeriodConsistency.MODERATE -> stringResource(R.string.period_consistency_moderate)
                    PeriodConsistency.THICK -> stringResource(R.string.period_consistency_thick)
                    PeriodConsistency.STRINGY -> stringResource(R.string.period_consistency_stringy)
                    PeriodConsistency.CLOTS_SMALL -> stringResource(R.string.period_consistency_clots_small)
                    PeriodConsistency.CLOTS_LARGE -> stringResource(R.string.period_consistency_clots_large)
                }
            )
        }

        if (showMood) {
            log.entry.moodScore?.let {
                InfoCard(
                    icon = Icons.Default.Mood,
                    title = stringResource(R.string.tracker_mood_label),
                    value = "$it / 5"
                )
            }
        }

        if (showEnergy) {
            log.entry.energyLevel?.let {
                InfoCard(
                    icon = Icons.Default.Bolt,
                    title = stringResource(R.string.tracker_energy_label),
                    value = "$it / 5"
                )
            }
        }

        if (showLibido) {
            log.entry.libidoScore?.let {
                InfoCard(
                    icon = Icons.Default.FavoriteBorder,
                    title = stringResource(R.string.tracker_libido_label),
                    value = "$it / 5"
                )
            }
        }

        waterCups?.let {
            if (it > 0) {
                InfoCard(
                    icon = Icons.Default.WaterDrop,
                    title = stringResource(R.string.tracker_water_label),
                    value = stringResource(R.string.tracker_water_cups, it)
                )
            }
        }

        if (!log.entry.note.isNullOrBlank()) {
            InfoCard(
                icon = Icons.AutoMirrored.Filled.Notes,
                title = stringResource(R.string.tracker_notes_label),
                value = log.entry.note!!
            )
        }

        if (log.symptomLogs.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Column(modifier = Modifier.padding(dims.sm)) {
                    Text(
                        stringResource(R.string.tracker_symptoms_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(dims.sm),
                        modifier = Modifier.padding(top = dims.xs)
                    ) {
                        items(log.symptomLogs, key = { it.symptomId }) { symptomLog ->
                            val symptomInfo = symptomLibrary.find { it.id == symptomLog.symptomId }
                            if (symptomInfo != null) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(symptomInfo.name) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (log.medicationLogs.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Column(modifier = Modifier.padding(dims.sm)) {
                    Text(
                        stringResource(R.string.tracker_medications_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(dims.sm),
                        modifier = Modifier.padding(top = dims.xs)
                    ) {
                        items(log.medicationLogs, key = { it.medicationId }) { medicationLog ->
                            val medicationInfo =
                                medicationLibrary.find { it.id == medicationLog.medicationId }
                            if (medicationInfo != null) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(medicationInfo.name) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (log.customTagLogs.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Column(modifier = Modifier.padding(dims.sm)) {
                    Text(
                        stringResource(R.string.tracker_custom_tags_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(dims.sm),
                        modifier = Modifier.padding(top = dims.xs)
                    ) {
                        items(log.customTagLogs, key = { it.tagId }) { tagLog ->
                            val tagInfo = customTagLibrary.find { it.id == tagLog.tagId }
                            if (tagInfo != null) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(tagInfo.name) }
                                )
                            }
                        }
                    }
                }
            }
        }

        FilledTonalButton(
            onClick = { onViewFullLogClick(log.entry.entryDate) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.tracker_view_full_log))
        }

        Spacer(Modifier.height(dims.md))
    }
}

/**
 * A card-styled info row with a leading icon, title, and trailing value.
 *
 * Used inside [LogSummarySheetContent] for each scalar data field
 * (phase, flow, color, consistency, mood, energy, libido, water, notes).
 *
 * @param icon  The leading [ImageVector] icon.
 * @param title The label text displayed after the icon.
 * @param value The data value displayed at the trailing edge.
 */
@Composable
private fun InfoCard(icon: ImageVector, title: String, value: String) {
    val dims = LocalDimensions.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.sm)
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Text(text = "$title:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
