package com.veleda.cyclewise.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Increment/decrement counter for daily water intake displayed as cups.
 *
 * Shows a centered cup count flanked by minus and plus buttons, with an
 * optional motivational prompt showing yesterday's intake.
 *
 * @param cups Current cup count to display.
 * @param onIncrement Callback when the user taps the plus button.
 * @param onDecrement Callback when the user taps the minus button.
 * @param yesterdayCupsForPrompt Yesterday's cup count for the motivational prompt,
 *        or `null` to hide the prompt.
 * @param modifier Modifier applied to the outer column.
 */
@Composable
fun WaterTrackerCounter(
    cups: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    yesterdayCupsForPrompt: Int?,
    modifier: Modifier = Modifier
) {
    val dims = LocalDimensions.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.water_tracker_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(dims.sm))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.md)
        ) {
            FilledIconButton(
                onClick = onDecrement,
                enabled = cups > 0,
                modifier = Modifier
                    .size(dims.buttonMin)
                    .testTag("water-decrement"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cd_water_remove))
            }
            Text(
                text = stringResource(R.string.water_cups_count, cups),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag("water-count")
            )
            FilledIconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(dims.buttonMin)
                    .testTag("water-increment"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_water_add))
            }
        }
        if (yesterdayCupsForPrompt != null) {
            Spacer(Modifier.height(dims.sm))
            Text(
                text = stringResource(R.string.water_yesterday_message, yesterdayCupsForPrompt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
