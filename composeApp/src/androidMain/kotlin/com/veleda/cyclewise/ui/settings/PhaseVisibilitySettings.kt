package com.veleda.cyclewise.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Composable that renders three toggle rows for controlling the visibility of
 * calculated cycle phases (Follicular, Ovulation, Luteal) on the tracker calendar.
 *
 * Period/Menstruation days are always visible and therefore have no toggle.
 *
 * Each phase is displayed as a Material 3 [ListItem] with a trailing [Switch],
 * a headline label, and a supporting description.
 *
 * Accepts boolean state values and toggle callbacks instead of [AppSettings] directly,
 * wiring through the [SettingsViewModel].
 *
 * @param showFollicular       Whether the Follicular phase tint is currently visible.
 * @param showOvulation        Whether the Ovulation phase tint is currently visible.
 * @param showLuteal           Whether the Luteal phase tint is currently visible.
 * @param onFollicularToggled  Callback invoked when the Follicular toggle changes.
 * @param onOvulationToggled   Callback invoked when the Ovulation toggle changes.
 * @param onLutealToggled      Callback invoked when the Luteal toggle changes.
 * @param showTitle            When `true` (default), renders a [titleMedium] header above the toggles.
 *                             Set to `false` when embedded inside a parent card that already provides a title.
 */
@Composable
fun PhaseVisibilitySettings(
    showFollicular: Boolean,
    showOvulation: Boolean,
    showLuteal: Boolean,
    onFollicularToggled: (Boolean) -> Unit,
    onOvulationToggled: (Boolean) -> Unit,
    onLutealToggled: (Boolean) -> Unit,
    showTitle: Boolean = true,
) {
    val dims = LocalDimensions.current

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
                    onCheckedChange = onFollicularToggled
                )
            }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.show_ovulation_label)) },
            supportingContent = { Text(stringResource(R.string.show_ovulation_description)) },
            trailingContent = {
                Switch(
                    checked = showOvulation,
                    onCheckedChange = onOvulationToggled
                )
            }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.show_luteal_label)) },
            supportingContent = { Text(stringResource(R.string.show_luteal_description)) },
            trailingContent = {
                Switch(
                    checked = showLuteal,
                    onCheckedChange = onLutealToggled
                )
            }
        )
    }
}
