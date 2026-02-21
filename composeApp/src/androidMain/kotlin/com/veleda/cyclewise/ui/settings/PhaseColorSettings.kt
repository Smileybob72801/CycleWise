package com.veleda.cyclewise.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.tracker.CyclePhaseColors
import com.veleda.cyclewise.ui.tracker.parseHexColor

/** Material 200-shade presets for quick color selection. */
private val PRESET_COLORS = listOf(
    "EF9A9A", // Red 200 (default menstruation)
    "F48FB1", // Pink 200
    "CE93D8", // Purple 200
    "B39DDB", // Deep Purple 200 (default luteal)
    "90CAF9", // Blue 200
    "80CBC4", // Teal 200 (default follicular)
    "A5D6A7", // Green 200
    "E6EE9C", // Lime 200
    "FFCC80", // Orange 200 (default ovulation)
    "BCAAA4", // Brown 200
)

/**
 * Composable that renders four color-editing rows (one per cycle phase),
 * each with a preset color grid, plus a "Reset to Defaults" button.
 *
 * Each row shows a colored preview swatch, the phase label, an [OutlinedTextField]
 * for entering a 6-character hex color code (no `#` prefix), and a horizontally
 * scrollable row of preset color swatches. Invalid input displays an inline error.
 *
 * Accepts state values and change callbacks instead of [AppSettings] directly,
 * wiring through the [SettingsViewModel].
 *
 * @param menstruationHex            Current 6-char hex color for the Menstruation phase.
 * @param follicularHex              Current 6-char hex color for the Follicular phase.
 * @param ovulationHex               Current 6-char hex color for the Ovulation phase.
 * @param lutealHex                  Current 6-char hex color for the Luteal phase.
 * @param onMenstruationColorChanged Callback invoked with the sanitised hex when the user edits the Menstruation color.
 * @param onFollicularColorChanged   Callback invoked with the sanitised hex when the user edits the Follicular color.
 * @param onOvulationColorChanged    Callback invoked with the sanitised hex when the user edits the Ovulation color.
 * @param onLutealColorChanged       Callback invoked with the sanitised hex when the user edits the Luteal color.
 * @param onResetDefaults            Callback invoked when the user taps "Reset to Defaults".
 * @param showTitle                  When `true` (default), renders a [titleMedium] header above the rows.
 *                                   Set to `false` when embedded inside a parent card that already provides a title.
 */
@Composable
fun PhaseColorSettings(
    menstruationHex: String,
    follicularHex: String,
    ovulationHex: String,
    lutealHex: String,
    onMenstruationColorChanged: (String) -> Unit,
    onFollicularColorChanged: (String) -> Unit,
    onOvulationColorChanged: (String) -> Unit,
    onLutealColorChanged: (String) -> Unit,
    onResetDefaults: () -> Unit,
    showTitle: Boolean = true,
) {
    val dims = LocalDimensions.current

    Column {
        if (showTitle) {
            Text(
                stringResource(R.string.phase_colors_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(dims.sm))
        }

        PhaseColorRow(
            label = stringResource(R.string.phase_color_period_label),
            hexValue = menstruationHex,
            defaultColor = CyclePhaseColors.Menstruation,
            onValueChange = onMenstruationColorChanged
        )
        PresetColorGrid(
            selectedHex = menstruationHex,
            onSelect = onMenstruationColorChanged
        )

        Spacer(Modifier.height(dims.sm))

        PhaseColorRow(
            label = stringResource(R.string.phase_color_follicular_label),
            hexValue = follicularHex,
            defaultColor = CyclePhaseColors.Follicular,
            onValueChange = onFollicularColorChanged
        )
        PresetColorGrid(
            selectedHex = follicularHex,
            onSelect = onFollicularColorChanged
        )

        Spacer(Modifier.height(dims.sm))

        PhaseColorRow(
            label = stringResource(R.string.phase_color_ovulation_label),
            hexValue = ovulationHex,
            defaultColor = CyclePhaseColors.Ovulation,
            onValueChange = onOvulationColorChanged
        )
        PresetColorGrid(
            selectedHex = ovulationHex,
            onSelect = onOvulationColorChanged
        )

        Spacer(Modifier.height(dims.sm))

        PhaseColorRow(
            label = stringResource(R.string.phase_color_luteal_label),
            hexValue = lutealHex,
            defaultColor = CyclePhaseColors.Luteal,
            onValueChange = onLutealColorChanged
        )
        PresetColorGrid(
            selectedHex = lutealHex,
            onSelect = onLutealColorChanged
        )

        Spacer(Modifier.height(dims.sm))
        TextButton(
            onClick = onResetDefaults,
            modifier = Modifier.padding(horizontal = dims.md)
        ) {
            Text(stringResource(R.string.phase_color_reset_defaults))
        }
    }
}

private val HEX_FILTER = Regex("[^0-9A-Fa-f]")

/**
 * A single row for editing one phase's hex color.
 *
 * Shows a 24 dp colored preview swatch, the phase label, and a hex text field.
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
    val dims = LocalDimensions.current
    val parsedColor = parseHexColor(hexValue)
    val isError = hexValue.isNotEmpty() && parsedColor == null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.sm),
        modifier = Modifier.padding(horizontal = dims.md)
    ) {
        Box(
            modifier = Modifier
                .size(dims.lg)
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

/**
 * Horizontally scrollable row of 32 dp colored circle swatches from [PRESET_COLORS].
 *
 * Tapping a swatch invokes [onSelect] with its hex value. The currently selected
 * preset receives a 2 dp `primary`-colored border ring.
 *
 * @param selectedHex The currently active hex value (used to highlight the matching swatch).
 * @param onSelect    Callback invoked with the hex string of the tapped preset.
 */
@Composable
private fun PresetColorGrid(
    selectedHex: String,
    onSelect: (String) -> Unit
) {
    val dims = LocalDimensions.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .padding(horizontal = dims.md)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dims.sm)
    ) {
        PRESET_COLORS.forEach { hex ->
            val color = parseHexColor(hex) ?: return@forEach
            val isSelected = hex.equals(selectedHex, ignoreCase = true)
            val desc = stringResource(R.string.phase_color_preset_content_description, hex)

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) Modifier.border(2.dp, primaryColor, CircleShape)
                        else Modifier
                    )
                    .clickable { onSelect(hex) }
                    .semantics { contentDescription = desc }
            )
        }
    }
}
