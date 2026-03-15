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
import com.veleda.cyclewise.ui.components.EducationalBottomSheet
import com.veleda.cyclewise.ui.settings.ColorsSettingsState
import com.veleda.cyclewise.ui.settings.HeatmapColorSettings
import com.veleda.cyclewise.ui.settings.PhaseColorSettings
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.settings.components.SettingsSectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Page 2 — Colors: Phase color and heatmap color customization.
 */
@Composable
internal fun ColorsPage(
    state: ColorsSettingsState,
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

        // ── Phase Colors Card ─────────────────────────────────────────
        SettingsSectionCard(
            title = stringResource(R.string.settings_section_phase_colors),
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

        // ── Heatmap Colors Card ───────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_heatmap_colors)) {
            HeatmapColorSettings(
                moodHex = state.heatmapMoodColorHex,
                energyHex = state.heatmapEnergyColorHex,
                libidoHex = state.heatmapLibidoColorHex,
                waterIntakeHex = state.heatmapWaterIntakeColorHex,
                symptomSeverityHex = state.heatmapSymptomSeverityColorHex,
                flowIntensityHex = state.heatmapFlowIntensityColorHex,
                medicationCountHex = state.heatmapMedicationCountColorHex,
                onColorChanged = { key, hex ->
                    onEvent(SettingsEvent.HeatmapColorChanged(key, hex))
                },
                onResetDefaults = { onEvent(SettingsEvent.ResetHeatmapColorsToDefaults) },
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
