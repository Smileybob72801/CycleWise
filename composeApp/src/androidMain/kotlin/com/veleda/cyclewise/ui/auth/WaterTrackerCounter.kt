package com.veleda.cyclewise.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R

@Composable
fun WaterTrackerCounter(
    cups: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    yesterdayCupsForPrompt: Int?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.water_tracker_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledIconButton(
                onClick = onDecrement,
                enabled = cups > 0,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("water-decrement"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("\u2212", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = stringResource(R.string.water_cups_count, cups),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag("water-count")
            )
            FilledIconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("water-increment"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
        if (yesterdayCupsForPrompt != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.water_yesterday_message, yesterdayCupsForPrompt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
