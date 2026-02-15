package com.veleda.cyclewise.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.launch

/**
 * Standalone composable that renders three checkbox rows for toggling
 * the visibility of calculated cycle phases (Follicular, Ovulation, Luteal)
 * on the tracker calendar.
 *
 * Period/Menstruation days are always visible and therefore have no toggle.
 *
 * Designed as a portable component so it can be relocated to different
 * settings menus in the future without modification.
 *
 * @param appSettings The [AppSettings] instance used to read and persist phase-visibility preferences.
 */
@Composable
fun PhaseVisibilitySettings(appSettings: AppSettings) {
    val scope = rememberCoroutineScope()

    val showFollicular by appSettings.showFollicularPhase.collectAsState(initial = true)
    val showOvulation by appSettings.showOvulationPhase.collectAsState(initial = true)
    val showLuteal by appSettings.showLutealPhase.collectAsState(initial = true)

    Column {
        Text(
            stringResource(R.string.phase_visibility_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = showFollicular,
                onCheckedChange = { scope.launch { appSettings.setShowFollicularPhase(it) } }
            )
            Text(stringResource(R.string.show_follicular_label))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = showOvulation,
                onCheckedChange = { scope.launch { appSettings.setShowOvulationPhase(it) } }
            )
            Text(stringResource(R.string.show_ovulation_label))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = showLuteal,
                onCheckedChange = { scope.launch { appSettings.setShowLutealPhase(it) } }
            )
            Text(stringResource(R.string.show_luteal_label))
        }
    }
}
