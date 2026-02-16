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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import com.veleda.cyclewise.reminders.ReminderNotifier
import com.veleda.cyclewise.reminders.ReminderScheduler
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Standalone composable that renders the three reminder configuration sections:
 * Period Prediction, Daily Medication, and Hydration.
 *
 * Each section has an enable/disable [Switch] plus type-specific settings that
 * are only shown when the reminder is enabled. Toggling or changing any setting
 * immediately schedules/cancels the corresponding WorkManager job via [reminderScheduler].
 *
 * On enabling any reminder on API 33+, the composable requests the POST_NOTIFICATIONS
 * runtime permission. If denied, the toggle reverts and a rationale message is shown.
 *
 * @param appSettings       the [AppSettings] instance for reading/writing reminder preferences.
 * @param reminderScheduler the [ReminderScheduler] instance for enqueuing/cancelling work.
 */
@Composable
fun ReminderSettings(appSettings: AppSettings, reminderScheduler: ReminderScheduler) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- Notification permission handling ---
    var pendingEnableAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingEnableAction?.invoke()
        } else {
            showPermissionRationale = true
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

    // --- State ---
    val periodEnabled by appSettings.reminderPeriodEnabled.collectAsState(initial = false)
    val periodDaysBefore by appSettings.reminderPeriodDaysBefore.collectAsState(initial = 2)
    val periodPrivacyAccepted by appSettings.reminderPeriodPrivacyAccepted.collectAsState(initial = false)
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val medicationEnabled by appSettings.reminderMedicationEnabled.collectAsState(initial = false)
    val medicationHour by appSettings.reminderMedicationHour.collectAsState(initial = 9)
    val medicationMinute by appSettings.reminderMedicationMinute.collectAsState(initial = 0)

    val hydrationEnabled by appSettings.reminderHydrationEnabled.collectAsState(initial = false)
    val hydrationGoalCups by appSettings.reminderHydrationGoalCups.collectAsState(initial = 8)
    val hydrationFrequencyHours by appSettings.reminderHydrationFrequencyHours.collectAsState(initial = 3)
    val hydrationStartHour by appSettings.reminderHydrationStartHour.collectAsState(initial = 8)
    val hydrationEndHour by appSettings.reminderHydrationEndHour.collectAsState(initial = 20)

    Column {
        Text(
            stringResource(R.string.reminder_settings_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        // ── Period Prediction ───────────────────────────────────────

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.reminder_period_label),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    stringResource(R.string.reminder_period_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = periodEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        ensurePermissionThen {
                            if (periodPrivacyAccepted) {
                                scope.launch {
                                    appSettings.setReminderPeriodEnabled(true)
                                    reminderScheduler.schedulePeriodPrediction(true)
                                }
                            } else {
                                showPrivacyDialog = true
                            }
                        }
                    } else {
                        scope.launch {
                            appSettings.setReminderPeriodEnabled(false)
                            reminderScheduler.schedulePeriodPrediction(false)
                        }
                    }
                }
            )
        }

        if (periodEnabled) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.reminder_period_days_before_label, periodDaysBefore))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3).forEach { days ->
                    FilterChip(
                        selected = periodDaysBefore == days,
                        onClick = {
                            scope.launch {
                                appSettings.setReminderPeriodDaysBefore(days)
                            }
                        },
                        label = { Text("$days") }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Daily Medication ────────────────────────────────────────

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.reminder_medication_label),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    stringResource(R.string.reminder_medication_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = medicationEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        ensurePermissionThen {
                            scope.launch {
                                appSettings.setReminderMedicationEnabled(true)
                                reminderScheduler.scheduleMedication(true, medicationHour, medicationMinute)
                            }
                        }
                    } else {
                        scope.launch {
                            appSettings.setReminderMedicationEnabled(false)
                            reminderScheduler.scheduleMedication(false)
                        }
                    }
                }
            )
        }

        if (medicationEnabled) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.reminder_medication_time_label))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "%02d:%02d".format(medicationHour, medicationMinute),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Hour", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = medicationHour.toFloat(),
                        onValueChange = { newHour ->
                            val hour = newHour.roundToInt()
                            scope.launch {
                                appSettings.setReminderMedicationHour(hour)
                                reminderScheduler.scheduleMedication(true, hour, medicationMinute)
                            }
                        },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    Text("Minute", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = medicationMinute.toFloat(),
                        onValueChange = { newMinute ->
                            val minute = newMinute.roundToInt()
                            scope.launch {
                                appSettings.setReminderMedicationMinute(minute)
                                reminderScheduler.scheduleMedication(true, medicationHour, minute)
                            }
                        },
                        valueRange = 0f..59f,
                        steps = 58,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Hydration ───────────────────────────────────────────────

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.reminder_hydration_label),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    stringResource(R.string.reminder_hydration_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = hydrationEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        ensurePermissionThen {
                            scope.launch {
                                appSettings.setReminderHydrationEnabled(true)
                                reminderScheduler.scheduleHydration(true, hydrationFrequencyHours)
                            }
                        }
                    } else {
                        scope.launch {
                            appSettings.setReminderHydrationEnabled(false)
                            reminderScheduler.scheduleHydration(false)
                        }
                    }
                }
            )
        }

        if (hydrationEnabled) {
            Spacer(Modifier.height(8.dp))

            // Goal cups
            Text(stringResource(R.string.reminder_hydration_goal_label, hydrationGoalCups))
            Slider(
                value = hydrationGoalCups.toFloat(),
                onValueChange = { newValue ->
                    scope.launch { appSettings.setReminderHydrationGoalCups(newValue.roundToInt()) }
                },
                valueRange = 1f..20f,
                steps = 18,
                modifier = Modifier.fillMaxWidth()
            )

            // Frequency
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.reminder_hydration_frequency_label))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 4).forEach { hours ->
                    FilterChip(
                        selected = hydrationFrequencyHours == hours,
                        onClick = {
                            scope.launch {
                                appSettings.setReminderHydrationFrequencyHours(hours)
                                reminderScheduler.scheduleHydration(true, hours)
                            }
                        },
                        label = { Text("${hours}h") }
                    )
                }
            }

            // Active window
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.reminder_hydration_start_label))
                    Slider(
                        value = hydrationStartHour.toFloat(),
                        onValueChange = { newValue ->
                            scope.launch {
                                appSettings.setReminderHydrationStartHour(newValue.roundToInt())
                            }
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
                            scope.launch {
                                appSettings.setReminderHydrationEndHour(newValue.roundToInt())
                            }
                        },
                        valueRange = 0f..23f,
                        steps = 22
                    )
                    Text("%02d:00".format(hydrationEndHour), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // --- Permission rationale ---
        if (showPermissionRationale) {
            Spacer(Modifier.height(8.dp))
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
            onAccept = {
                showPrivacyDialog = false
                scope.launch {
                    appSettings.setReminderPeriodPrivacyAccepted(true)
                    appSettings.setReminderPeriodEnabled(true)
                    reminderScheduler.schedulePeriodPrediction(true)
                }
            },
            onDismiss = {
                showPrivacyDialog = false
            }
        )
    }
}
