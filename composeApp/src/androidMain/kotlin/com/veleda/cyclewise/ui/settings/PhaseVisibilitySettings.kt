package com.veleda.cyclewise.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.coroutines.launch

/**
 * Composable that renders three toggle rows for controlling the visibility of
 * calculated cycle phases (Follicular, Ovulation, Luteal) on the tracker calendar.
 *
 * Period/Menstruation days are always visible and therefore have no toggle.
 *
 * Each phase is displayed as a Material 3 [ListItem] with a trailing [Switch],
 * a headline label, and a supporting description.
 *
 * @param appSettings The [AppSettings] instance used to read and persist phase-visibility preferences.
 * @param showTitle   When `true` (default), renders a [titleMedium] header above the toggles.
 *                    Set to `false` when embedded inside a parent card that already provides a title.
 */
@Composable
fun PhaseVisibilitySettings(appSettings: AppSettings, showTitle: Boolean = true) {
    val scope = rememberCoroutineScope()
    val dims = LocalDimensions.current

    val showFollicular by appSettings.showFollicularPhase.collectAsState(initial = true)
    val showOvulation by appSettings.showOvulationPhase.collectAsState(initial = true)
    val showLuteal by appSettings.showLutealPhase.collectAsState(initial = true)

    Column {
        if (showTitle) {
            Text(
                stringResource(R.string.phase_visibility_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(dims.sm))
        }

        ListItem(
            headlineContent = { Text(stringResource(R.string.show_follicular_label)) },
            supportingContent = { Text(stringResource(R.string.show_follicular_description)) },
            trailingContent = {
                Switch(
                    checked = showFollicular,
                    onCheckedChange = { scope.launch { appSettings.setShowFollicularPhase(it) } }
                )
            }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.show_ovulation_label)) },
            supportingContent = { Text(stringResource(R.string.show_ovulation_description)) },
            trailingContent = {
                Switch(
                    checked = showOvulation,
                    onCheckedChange = { scope.launch { appSettings.setShowOvulationPhase(it) } }
                )
            }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.show_luteal_label)) },
            supportingContent = { Text(stringResource(R.string.show_luteal_description)) },
            trailingContent = {
                Switch(
                    checked = showLuteal,
                    onCheckedChange = { scope.launch { appSettings.setShowLutealPhase(it) } }
                )
            }
        )
    }
}
