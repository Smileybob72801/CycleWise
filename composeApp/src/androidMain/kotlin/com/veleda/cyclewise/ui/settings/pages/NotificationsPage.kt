package com.veleda.cyclewise.ui.settings.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.settings.ReminderSettings
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.settings.SettingsUiState
import com.veleda.cyclewise.ui.settings.components.SettingsSectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Page 2 — Notifications: Period prediction, medication, and hydration reminders.
 */
@Composable
internal fun NotificationsPage(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    val dims = LocalDimensions.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.md),
        verticalArrangement = Arrangement.spacedBy(dims.md)
    ) {
        Spacer(Modifier.height(dims.sm))

        // ── Notifications Card ───────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_notifications)) {
            ReminderSettings(
                periodEnabled = uiState.periodReminderEnabled,
                periodDaysBefore = uiState.periodDaysBefore,
                periodPrivacyAccepted = uiState.periodPrivacyAccepted,
                medicationEnabled = uiState.medicationReminderEnabled,
                medicationHour = uiState.medicationHour,
                medicationMinute = uiState.medicationMinute,
                hydrationEnabled = uiState.hydrationReminderEnabled,
                hydrationGoalCups = uiState.hydrationGoalCups,
                hydrationFrequencyHours = uiState.hydrationFrequencyHours,
                hydrationStartHour = uiState.hydrationStartHour,
                hydrationEndHour = uiState.hydrationEndHour,
                showPermissionRationale = uiState.showPermissionRationale,
                showPrivacyDialog = uiState.showPrivacyDialog,
                onEvent = onEvent,
                showTitle = false,
            )
        }

        Spacer(Modifier.height(dims.xl))
    }
}
