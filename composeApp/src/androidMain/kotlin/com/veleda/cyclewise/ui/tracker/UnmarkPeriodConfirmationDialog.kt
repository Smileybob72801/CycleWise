package com.veleda.cyclewise.ui.tracker

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R

/**
 * Confirmation dialog shown when the user long-presses a single period day that has
 * logged flow, color, or consistency data. Warns that the data will be permanently
 * deleted if the day is unmarked.
 */
@Composable
internal fun UnmarkPeriodDayConfirmationDialog(
    uiState: TrackerUiState,
    onEvent: (TrackerEvent) -> Unit,
) {
    if (uiState.showUnmarkConfirmation && uiState.unmarkDate != null) {
        AlertDialog(
            onDismissRequest = { onEvent(TrackerEvent.UnmarkPeriodDismissed) },
            title = { Text(stringResource(R.string.unmark_period_day_title)) },
            text = { Text(stringResource(R.string.unmark_period_day_message)) },
            confirmButton = {
                Button(
                    onClick = { onEvent(TrackerEvent.UnmarkPeriodDayConfirmed(uiState.unmarkDate)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.unmark_period_day_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onEvent(TrackerEvent.UnmarkPeriodDismissed) }) {
                    Text(stringResource(R.string.tracker_cancel))
                }
            },
        )
    }
}

/**
 * Confirmation dialog shown when a drag-shrink operation would remove multiple period
 * days, some of which have logged flow, color, or consistency data. Displays the total
 * number of days being removed and how many contain data.
 */
@Composable
internal fun UnmarkPeriodRangeConfirmationDialog(
    uiState: TrackerUiState,
    onEvent: (TrackerEvent) -> Unit,
) {
    if (uiState.showUnmarkConfirmation && uiState.unmarkDates.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { onEvent(TrackerEvent.UnmarkPeriodDismissed) },
            title = { Text(stringResource(R.string.unmark_period_range_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.unmark_period_range_message,
                        uiState.unmarkDates.size,
                        uiState.unmarkDaysWithDataCount,
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(TrackerEvent.UnmarkPeriodRangeConfirmed(uiState.unmarkDates)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.unmark_period_range_confirm, uiState.unmarkDates.size))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onEvent(TrackerEvent.UnmarkPeriodDismissed) }) {
                    Text(stringResource(R.string.tracker_cancel))
                }
            },
        )
    }
}
