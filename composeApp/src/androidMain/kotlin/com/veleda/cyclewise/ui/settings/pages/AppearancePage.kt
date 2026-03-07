package com.veleda.cyclewise.ui.settings.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.components.EducationalBottomSheet
import com.veleda.cyclewise.ui.settings.PhaseColorSettings
import com.veleda.cyclewise.ui.settings.PhaseVisibilitySettings
import com.veleda.cyclewise.ui.settings.AppearanceSettingsState
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.settings.components.SettingsSectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.theme.ThemeMode

/**
 * Page 1 — Appearance: Display toggles, phase visibility, and phase colors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearancePage(
    state: AppearanceSettingsState,
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

        // ── Theme Card ───────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_theme)) {
            val modes = ThemeMode.entries
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.md)
            ) {
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = modes.size
                        ),
                        onClick = { onEvent(SettingsEvent.ThemeModeChanged(mode)) },
                        selected = state.themeMode == mode,
                        label = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
                                    ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
                                    ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
                                }
                            )
                        }
                    )
                }
            }
        }

        // ── Display Card ─────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_display)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_mood_label)) },
                supportingContent = { Text(stringResource(R.string.show_mood_description)) },
                trailingContent = {
                    Switch(
                        checked = state.showMood,
                        onCheckedChange = { onEvent(SettingsEvent.ShowMoodToggled(it)) }
                    )
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_energy_label)) },
                supportingContent = { Text(stringResource(R.string.show_energy_description)) },
                trailingContent = {
                    Switch(
                        checked = state.showEnergy,
                        onCheckedChange = { onEvent(SettingsEvent.ShowEnergyToggled(it)) }
                    )
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_libido_label)) },
                supportingContent = { Text(stringResource(R.string.show_libido_description)) },
                trailingContent = {
                    Switch(
                        checked = state.showLibido,
                        onCheckedChange = { onEvent(SettingsEvent.ShowLibidoToggled(it)) }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = dims.md))

            PhaseVisibilitySettings(
                showFollicular = state.showFollicular,
                showOvulation = state.showOvulation,
                showLuteal = state.showLuteal,
                onFollicularToggled = { onEvent(SettingsEvent.ShowFollicularToggled(it)) },
                onOvulationToggled = { onEvent(SettingsEvent.ShowOvulationToggled(it)) },
                onLutealToggled = { onEvent(SettingsEvent.ShowLutealToggled(it)) },
                showTitle = false,
            )
        }

        // ── Customization Card ───────────────────────────────────────
        SettingsSectionCard(
            title = stringResource(R.string.settings_section_customization),
            onInfoClick = { onEvent(SettingsEvent.ShowEducationalSheet("CyclePhase.Colors")) },
        ) {
            PhaseColorSettings(
                menstruationHex = state.menstruationColorHex,
                follicularHex = state.follicularColorHex,
                ovulationHex = state.ovulationColorHex,
                lutealHex = state.lutealColorHex,
                onMenstruationColorChanged = { onEvent(SettingsEvent.MenstruationColorChanged(it)) },
                onFollicularColorChanged = { onEvent(SettingsEvent.FollicularColorChanged(it)) },
                onOvulationColorChanged = { onEvent(SettingsEvent.OvulationColorChanged(it)) },
                onLutealColorChanged = { onEvent(SettingsEvent.LutealColorChanged(it)) },
                onResetDefaults = { onEvent(SettingsEvent.ResetPhaseColorsToDefaults) },
                showTitle = false,
            )
        }

        Spacer(Modifier.height(dims.xl))
    }

    state.educationalArticles?.let { articles ->
        EducationalBottomSheet(
            articles = articles,
            onDismiss = { onEvent(SettingsEvent.DismissEducationalSheet) },
        )
    }
}
