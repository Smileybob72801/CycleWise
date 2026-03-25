package com.veleda.cyclewise.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.reminders.ReminderNotifier
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlin.math.roundToInt

/**
 * Composable that renders the three reminder configuration sections:
 * Period Prediction, Daily Medication, and Hydration.
 *
 * Each section has an enable/disable [Switch] plus type-specific settings that
 * are only shown when the reminder is enabled. All state changes are dispatched
 * via [onEvent] to the [SettingsViewModel].
 *
 * On enabling any reminder on API 33+, the composable requests the POST_NOTIFICATIONS
 * runtime permission. If denied, the toggle reverts and a rationale message is shown.
 * Permission handling stays in the composable since it requires `@Composable` context.
 *
 * @param periodEnabled            Whether the period prediction reminder is enabled.
 * @param periodDaysBefore         Days before predicted period to notify (1-3).
 * @param periodPrivacyAccepted    Whether the user has accepted the period privacy notice.
 * @param medicationEnabled        Whether the daily medication reminder is enabled.
 * @param medicationHour           Hour for the medication reminder (0-23).
 * @param medicationMinute         Minute for the medication reminder (0-59).
 * @param hydrationEnabled         Whether the hydration reminder is enabled.
 * @param hydrationGoalCups        Daily water goal in cups.
 * @param hydrationFrequencyHours  Interval between hydration reminders in hours.
 * @param hydrationStartHour       Active window start hour.
 * @param hydrationEndHour         Active window end hour.
 * @param showPermissionRationale  Whether to show the permission denied rationale text.
 * @param showPrivacyDialog        Whether the period privacy dialog should be shown.
 * @param onEvent                  Event dispatcher for [SettingsEvent] variants.
 * @param showTitle                When `true` (default), renders a [titleMedium] header above the sections.
 *                                 Set to `false` when embedded inside a parent card that already provides a title.
 */
@Composable
fun ReminderSettings(
    periodEnabled: Boolean,
    periodDaysBefore: Int,
    periodPrivacyAccepted: Boolean,
    medicationEnabled: Boolean,
    medicationHour: Int,
    medicationMinute: Int,
    hydrationEnabled: Boolean,
    hydrationGoalCups: Int,
    hydrationFrequencyHours: Int,
    hydrationStartHour: Int,
    hydrationEndHour: Int,
    showPermissionRationale: Boolean,
    showPrivacyDialog: Boolean,
    onEvent: (SettingsEvent) -> Unit,
    showTitle: Boolean = true,
) {
    val context = LocalContext.current
    val dims = LocalDimensions.current

    // --- Notification permission handling (requires @Composable context) ---
    var pendingEnableAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingEnableAction?.invoke()
        } else {
            onEvent(SettingsEvent.ShowPermissionRationale)
        }
        pendingEnableAction = null
    }

    /**
     * Ensures POST_NOTIFICATIONS permission is granted before executing [action].
     * On API < 33, executes immediately. On API 33+, requests the permission first.
     */
    fun ensurePermissionThen(action: () -> Unit) {
        if (ReminderNotifier.hasPermission(context)) {
            action()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingEnableAction = action
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(modifier = Modifier.padding(horizontal = dims.md)) {
        if (showTitle) {
            Text(
                stringResource(R.string.reminder_settings_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(dims.sm))
        }

        // ── Period Prediction ───────────────────────────────────────

        ListItem(
            headlineContent = { Text(stringResource(R.string.reminder_period_label)) },
            supportingContent = { Text(stringResource(R.string.reminder_period_description)) },
            trailingContent = {
                Switch(
                    checked = periodEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            ensurePermissionThen {
                                if (periodPrivacyAccepted) {
                                    onEvent(SettingsEvent.PeriodReminderToggled(true))
                                } else {
                                    onEvent(SettingsEvent.ShowPrivacyDialog)
                                }
                            }
                        } else {
                            onEvent(SettingsEvent.PeriodReminderToggled(false))
                        }
                    }
                )
            }
        )

        if (periodEnabled) {
            Column(modifier = Modifier.padding(start = dims.lg)) {
                Spacer(Modifier.height(dims.sm))
                Text(stringResource(R.string.reminder_period_days_before_label, periodDaysBefore))
                Row(horizontalArrangement = Arrangement.spacedBy(dims.sm)) {
                    listOf(1, 2, 3).forEach { days ->
                        FilterChip(
                            selected = periodDaysBefore == days,
                            onClick = { onEvent(SettingsEvent.PeriodDaysBeforeChanged(days)) },
                            label = { Text("$days") }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(dims.md))

        // ── Daily Medication ────────────────────────────────────────

        ListItem(
            headlineContent = { Text(stringResource(R.string.reminder_medication_label)) },
            supportingContent = { Text(stringResource(R.string.reminder_medication_description)) },
            trailingContent = {
                Switch(
                    checked = medicationEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            ensurePermissionThen {
                                onEvent(SettingsEvent.MedicationReminderToggled(true))
                            }
                        } else {
                            onEvent(SettingsEvent.MedicationReminderToggled(false))
                        }
                    }
                )
            }
        )

        if (medicationEnabled) {
            Column(modifier = Modifier.padding(start = dims.lg)) {
                Spacer(Modifier.height(dims.sm))
                Text(stringResource(R.string.reminder_medication_time_label))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dims.sm)
                ) {
                    Text(
                        "%02d:%02d".format(medicationHour, medicationMinute),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.width(dims.sm))
                    Column {
                        Text(stringResource(R.string.reminder_hour_label), style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = medicationHour.toFloat(),
                            onValueChange = { newHour ->
                                onEvent(SettingsEvent.MedicationHourChanged(newHour.roundToInt()))
                            },
                            valueRange = 0f..23f,
                            steps = 22,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dims.sm)
                ) {
                    Column {
                        Text(stringResource(R.string.reminder_minute_label), style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = medicationMinute.toFloat(),
                            onValueChange = { newMinute ->
                                onEvent(SettingsEvent.MedicationMinuteChanged(newMinute.roundToInt()))
                            },
                            valueRange = 0f..59f,
                            steps = 58,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(dims.md))

        // ── Hydration ───────────────────────────────────────────────

        ListItem(
            headlineContent = { Text(stringResource(R.string.reminder_hydration_label)) },
            supportingContent = { Text(stringResource(R.string.reminder_hydration_description)) },
            trailingContent = {
                Switch(
                    checked = hydrationEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            ensurePermissionThen {
                                onEvent(SettingsEvent.HydrationReminderToggled(true))
                            }
                        } else {
                            onEvent(SettingsEvent.HydrationReminderToggled(false))
                        }
                    }
                )
            }
        )

        if (hydrationEnabled) {
            Column(modifier = Modifier.padding(start = dims.lg)) {
                Spacer(Modifier.height(dims.sm))

                // Goal cups
                Text(stringResource(R.string.reminder_hydration_goal_label, hydrationGoalCups))
                Slider(
                    value = hydrationGoalCups.toFloat(),
                    onValueChange = { newValue ->
                        onEvent(SettingsEvent.HydrationGoalCupsChanged(newValue.roundToInt()))
                    },
                    valueRange = 1f..20f,
                    steps = 18,
                    modifier = Modifier.fillMaxWidth()
                )

                // Frequency
                Spacer(Modifier.height(dims.sm))
                Text(stringResource(R.string.reminder_hydration_frequency_label))
                Row(horizontalArrangement = Arrangement.spacedBy(dims.sm)) {
                    listOf(2, 3, 4).forEach { hours ->
                        FilterChip(
                            selected = hydrationFrequencyHours == hours,
                            onClick = { onEvent(SettingsEvent.HydrationFrequencyChanged(hours)) },
                            label = { Text("${hours}h") }
                        )
                    }
                }

                // Active window
                Spacer(Modifier.height(dims.sm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dims.md),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.reminder_hydration_start_label))
                        Slider(
                            value = hydrationStartHour.toFloat(),
                            onValueChange = { newValue ->
                                onEvent(SettingsEvent.HydrationStartHourChanged(newValue.roundToInt()))
                            },
                            valueRange = 0f..23f,
                            steps = 22
                        )
                        Text("%02d:00".format(hydrationStartHour), style = MaterialTheme.typography.bodySmall)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.reminder_hydration_end_label))
                        Slider(
                            value = hydrationEndHour.toFloat(),
                            onValueChange = { newValue ->
                                onEvent(SettingsEvent.HydrationEndHourChanged(newValue.roundToInt()))
                            },
                            valueRange = 0f..23f,
                            steps = 22
                        )
                        Text("%02d:00".format(hydrationEndHour), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // --- Permission rationale ---
        if (showPermissionRationale) {
            Spacer(Modifier.height(dims.sm))
            Text(
                stringResource(R.string.reminder_permission_rationale),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    // --- Privacy dialog ---
    if (showPrivacyDialog) {
        PeriodPrivacyDialog(
            onAccept = { onEvent(SettingsEvent.PeriodPrivacyAccepted) },
            onDismiss = { onEvent(SettingsEvent.DismissPrivacyDialog) }
        )
    }
}
