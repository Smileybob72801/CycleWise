package com.veleda.cyclewise.ui.log.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Star as StarOutlined
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.auth.WaterTrackerCounter
import com.veleda.cyclewise.ui.coachmark.CoachMarkState
import com.veleda.cyclewise.ui.coachmark.HintKey
import com.veleda.cyclewise.ui.coachmark.coachMarkTarget
import com.veleda.cyclewise.ui.log.components.SectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.theme.RhythmWiseColors

/**
 * Daily log page for mood, energy, libido, and water intake.
 *
 * Renders each wellness metric inside a [SectionCard] with an info button
 * that triggers the educational bottom sheet. Integrates with the coach-mark
 * walkthrough to highlight mood, energy, and water sections.
 *
 * @param moodScore Current mood score (1-5), or `null` if unset.
 * @param energyLevel Current energy level (1-5), or `null` if unset.
 * @param libidoScore Current libido score (1-5), or `null` if unset.
 * @param waterCups Current water intake in cups.
 * @param showWellnessPrompt Whether to display the one-time empty-state prompt.
 *        Controlled by the ViewModel via [HintPreferences]; once the user logs any
 *        wellness data, this becomes `false` permanently.
 * @param onMoodChanged Callback when the user selects or deselects a mood score.
 * @param onEnergyChanged Callback when the user selects or deselects an energy level.
 * @param onLibidoChanged Callback when the user selects or deselects a libido score.
 * @param onWaterIncrement Callback when the user taps the water increment button.
 * @param onWaterDecrement Callback when the user taps the water decrement button.
 * @param onShowEducationalSheet Callback to display educational content for the given tag.
 * @param coachMarkState Optional coach-mark state for walkthrough integration.
 * @param activeHintKey The currently active walkthrough hint key, or `null` when no
 *        walkthrough is running. Used to disable non-target sections during task steps.
 */
@Composable
internal fun WellnessPage(
    moodScore: Int?,
    energyLevel: Int?,
    libidoScore: Int?,
    waterCups: Int,
    showWellnessPrompt: Boolean = false,
    onMoodChanged: (Int?) -> Unit,
    onEnergyChanged: (Int?) -> Unit,
    onLibidoChanged: (Int?) -> Unit,
    onWaterIncrement: () -> Unit,
    onWaterDecrement: () -> Unit,
    onShowEducationalSheet: (String) -> Unit,
    coachMarkState: CoachMarkState? = null,
    activeHintKey: HintKey? = null,
) {
    val dims = LocalDimensions.current

    // During task walkthrough steps, only the target section is interactive.
    val walkthroughActive = activeHintKey != null
    val moodEnabled = !walkthroughActive || activeHintKey == HintKey.DAILY_LOG_MOOD
    val energyEnabled = !walkthroughActive || activeHintKey == HintKey.DAILY_LOG_ENERGY
    val waterEnabled = !walkthroughActive || activeHintKey == HintKey.DAILY_LOG_WATER
    val libidoEnabled = !walkthroughActive
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.md),
        verticalArrangement = Arrangement.spacedBy(dims.md),
    ) {
        Spacer(Modifier.height(dims.sm))

        AnimatedVisibility(
            visible = showWellnessPrompt,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dims.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dims.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.EditNote,
                    contentDescription = stringResource(R.string.daily_log_wellness_empty_icon_cd),
                    modifier = Modifier.size(dims.iconMd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.daily_log_wellness_empty_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        SectionCard(
            title = stringResource(R.string.daily_log_mood_title),
            icon = Icons.Outlined.SelfImprovement,
            onInfoClick = { onShowEducationalSheet("Mood") },
            modifier = (if (coachMarkState != null) {
                Modifier.coachMarkTarget(HintKey.DAILY_LOG_MOOD, coachMarkState)
            } else {
                Modifier
            }).alpha(if (moodEnabled) 1f else 0.38f),
        ) {
            MoodSelector(
                selectedMood = moodScore,
                onSelectionChanged = onMoodChanged,
                enabled = moodEnabled,
            )
        }

        SectionCard(
            title = stringResource(R.string.energy_section_title),
            icon = Icons.Outlined.Bedtime,
            onInfoClick = { onShowEducationalSheet("Energy") },
            modifier = (if (coachMarkState != null) {
                Modifier.coachMarkTarget(HintKey.DAILY_LOG_ENERGY, coachMarkState)
            } else {
                Modifier
            }).alpha(if (energyEnabled) 1f else 0.38f),
        ) {
            ScoreSelector(
                selectedScore = energyLevel,
                onSelectionChanged = onEnergyChanged,
                contentDescriptionPrefix = stringResource(R.string.energy_section_title),
                enabled = energyEnabled,
            )
        }

        SectionCard(
            title = stringResource(R.string.libido_section_title),
            icon = Icons.Outlined.FavoriteBorder,
            onInfoClick = { onShowEducationalSheet("Libido") },
            modifier = Modifier.alpha(if (libidoEnabled) 1f else 0.38f),
        ) {
            ScoreSelector(
                selectedScore = libidoScore,
                onSelectionChanged = onLibidoChanged,
                contentDescriptionPrefix = stringResource(R.string.libido_section_title),
                enabled = libidoEnabled,
            )
        }

        SectionCard(
            title = stringResource(R.string.water_section_title),
            icon = Icons.Outlined.WaterDrop,
            onInfoClick = { onShowEducationalSheet("Hydration") },
            modifier = (if (coachMarkState != null) {
                Modifier.coachMarkTarget(HintKey.DAILY_LOG_WATER, coachMarkState)
            } else {
                Modifier
            }).alpha(if (waterEnabled) 1f else 0.38f),
        ) {
            WaterTrackerCounter(
                cups = waterCups,
                onIncrement = onWaterIncrement,
                onDecrement = onWaterDecrement,
                yesterdayCupsForPrompt = null,
                modifier = Modifier.fillMaxWidth(),
                enabled = waterEnabled,
            )
        }

        // Bottom spacer for comfortable scrolling past bottom nav
        Spacer(Modifier.height(dims.xl))
    }
}

/**
 * 1-5 star rating selector for mood score.
 *
 * Renders a row of star icons; filled stars indicate the selected score,
 * outlined stars indicate unselected values. Tapping a star sets that score.
 * Tapping the already-selected star clears the rating back to `null`.
 *
 * @param selectedMood Currently selected mood score (1-5), or `null` if unset.
 * @param onSelectionChanged Callback invoked with the tapped score, or `null` when deselecting.
 * @param enabled Whether the selector is interactive. When `false`, taps are ignored.
 */
@Composable
internal fun MoodSelector(
    selectedMood: Int?,
    onSelectionChanged: (Int?) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        (1..5).forEach { score ->
            IconButton(
                onClick = { onSelectionChanged(if (score == selectedMood) null else score) },
                enabled = enabled,
            ) {
                val icon = if (score <= (selectedMood ?: 0)) Icons.Filled.Star else Icons.Outlined.StarOutlined
                Icon(
                    icon,
                    contentDescription = stringResource(R.string.daily_log_mood_score, score),
                    modifier = Modifier.size(LocalDimensions.current.xl),
                    tint = if (score <= (selectedMood ?: 0))
                        RhythmWiseColors.StarGold
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Reusable 1-5 star rating selector for numeric wellness scores (energy, libido).
 *
 * Tapping the already-selected star clears the rating back to `null`.
 *
 * @param selectedScore Currently selected score (1-5), or null if unset.
 * @param onSelectionChanged Callback invoked with the tapped score, or `null` when deselecting.
 * @param contentDescriptionPrefix Prefix for accessibility labels (e.g., "Energy").
 * @param enabled Whether the selector is interactive. When `false`, taps are ignored.
 */
@Composable
internal fun ScoreSelector(
    selectedScore: Int?,
    onSelectionChanged: (Int?) -> Unit,
    contentDescriptionPrefix: String,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        (1..5).forEach { score ->
            IconButton(
                onClick = { onSelectionChanged(if (score == selectedScore) null else score) },
                enabled = enabled,
            ) {
                val icon = if (score <= (selectedScore ?: 0)) Icons.Filled.Star else Icons.Outlined.StarOutlined
                Icon(
                    icon,
                    contentDescription = stringResource(R.string.daily_log_score, contentDescriptionPrefix, score),
                    modifier = Modifier.size(LocalDimensions.current.xl),
                    tint = if (score <= (selectedScore ?: 0))
                        RhythmWiseColors.StarGold
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
