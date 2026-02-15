package com.veleda.cyclewise.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.tracker.CyclePhaseColors
import com.veleda.cyclewise.ui.tracker.parseHexColor
import kotlinx.coroutines.launch

/**
 * Standalone composable that renders four color-editing rows (one per cycle phase)
 * plus a "Reset to Defaults" button.
 *
 * Each row shows a colored preview dot, the phase label, and an [OutlinedTextField]
 * for entering a 6-character hex color code (no `#` prefix). Invalid input displays
 * an inline error message.
 *
 * Designed as a portable component so it can be relocated to different settings menus
 * in the future without modification.
 *
 * @param appSettings The [AppSettings] instance used to read and persist phase-color preferences.
 */
@Composable
fun PhaseColorSettings(appSettings: AppSettings) {
    val scope = rememberCoroutineScope()

    val menstruationHex by appSettings.menstruationColor.collectAsState(initial = CyclePhaseColors.DEFAULT_MENSTRUATION_HEX)
    val follicularHex by appSettings.follicularColor.collectAsState(initial = CyclePhaseColors.DEFAULT_FOLLICULAR_HEX)
    val ovulationHex by appSettings.ovulationColor.collectAsState(initial = CyclePhaseColors.DEFAULT_OVULATION_HEX)
    val lutealHex by appSettings.lutealColor.collectAsState(initial = CyclePhaseColors.DEFAULT_LUTEAL_HEX)

    Column {
        Text(
            stringResource(R.string.phase_colors_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        PhaseColorRow(
            label = stringResource(R.string.phase_color_period_label),
            hexValue = menstruationHex,
            defaultColor = CyclePhaseColors.Menstruation,
            onValueChange = { scope.launch { appSettings.setMenstruationColor(it) } }
        )
        PhaseColorRow(
            label = stringResource(R.string.phase_color_follicular_label),
            hexValue = follicularHex,
            defaultColor = CyclePhaseColors.Follicular,
            onValueChange = { scope.launch { appSettings.setFollicularColor(it) } }
        )
        PhaseColorRow(
            label = stringResource(R.string.phase_color_ovulation_label),
            hexValue = ovulationHex,
            defaultColor = CyclePhaseColors.Ovulation,
            onValueChange = { scope.launch { appSettings.setOvulationColor(it) } }
        )
        PhaseColorRow(
            label = stringResource(R.string.phase_color_luteal_label),
            hexValue = lutealHex,
            defaultColor = CyclePhaseColors.Luteal,
            onValueChange = { scope.launch { appSettings.setLutealColor(it) } }
        )

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            scope.launch {
                appSettings.setMenstruationColor(CyclePhaseColors.DEFAULT_MENSTRUATION_HEX)
                appSettings.setFollicularColor(CyclePhaseColors.DEFAULT_FOLLICULAR_HEX)
                appSettings.setOvulationColor(CyclePhaseColors.DEFAULT_OVULATION_HEX)
                appSettings.setLutealColor(CyclePhaseColors.DEFAULT_LUTEAL_HEX)
            }
        }) {
            Text(stringResource(R.string.phase_color_reset_defaults))
        }
    }
}

private val HEX_FILTER = Regex("[^0-9A-Fa-f]")

/**
 * A single row for editing one phase's hex color.
 *
 * @param label        Phase display name.
 * @param hexValue     Current persisted 6-char hex string.
 * @param defaultColor Fallback [Color] used when [hexValue] is invalid.
 * @param onValueChange Callback invoked with the sanitised hex string when the user edits the field.
 */
@Composable
private fun PhaseColorRow(
    label: String,
    hexValue: String,
    defaultColor: Color,
    onValueChange: (String) -> Unit
) {
    val parsedColor = parseHexColor(hexValue)
    val isError = hexValue.isNotEmpty() && parsedColor == null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(parsedColor ?: defaultColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp)
        )
        OutlinedTextField(
            value = hexValue,
            onValueChange = { raw ->
                val filtered = raw.replace(HEX_FILTER, "").take(6).uppercase()
                onValueChange(filtered)
            },
            placeholder = { Text(stringResource(R.string.phase_color_hex_hint)) },
            isError = isError,
            supportingText = if (isError) {
                { Text(stringResource(R.string.phase_color_invalid_hex)) }
            } else null,
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}
