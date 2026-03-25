package com.veleda.cyclewise.ui.tracker

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R

/**
 * Modal bottom sheet wrapper for displaying the day-log summary.
 *
 * @param uiState   Current tracker UI state with sheet data.
 * @param showMood  Whether mood score is visible in summary.
 * @param showEnergy Whether energy level is visible in summary.
 * @param showLibido Whether libido score is visible in summary.
 * @param sheetState Material3 sheet state.
 * @param onEvent   Callback for tracker events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayDetailSheet(
    uiState: TrackerUiState,
    showMood: Boolean,
    showEnergy: Boolean,
    showLibido: Boolean,
    sheetState: SheetState,
    onEvent: (TrackerEvent) -> Unit,
) {
    if (uiState.logForSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(TrackerEvent.DismissLogSheet) },
            sheetState = sheetState
        ) {
            val sheetPhase = uiState.logForSheet?.let { log ->
                uiState.dayDetails[log.entry.entryDate]?.cyclePhase
            }
            LogSummarySheetContent(
                log = uiState.logForSheet!!,
                periodId = uiState.periodIdForSheet,
                cyclePhase = sheetPhase,
                symptomLibrary = uiState.symptomLibrary,
                medicationLibrary = uiState.medicationLibrary,
                customTagLibrary = uiState.customTagLibrary,
                waterCups = uiState.waterCupsForSheet,
                showMood = showMood,
                showEnergy = showEnergy,
                showLibido = showLibido,
                onEditClick = { date -> onEvent(TrackerEvent.EditLogClicked(date)) },
                onDeleteClick = { periodId -> onEvent(TrackerEvent.DeletePeriodRequested(periodId)) },
                onViewFullLogClick = { date -> onEvent(TrackerEvent.EditLogClicked(date)) }
            )
        }
    }
}

/**
 * Confirmation dialog for deleting a period.
 *
 * @param uiState Current tracker UI state with delete confirmation flags.
 * @param onEvent Callback for tracker events.
 */
@Composable
internal fun DeletePeriodConfirmationDialog(
    uiState: TrackerUiState,
    onEvent: (TrackerEvent) -> Unit,
) {
    if (uiState.showDeleteConfirmation && uiState.periodIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { onEvent(TrackerEvent.DeletePeriodDismissed) },
            title = { Text(stringResource(R.string.tracker_delete_title)) },
            text = { Text(stringResource(R.string.tracker_delete_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onEvent(TrackerEvent.DeletePeriodConfirmed(uiState.periodIdToDelete!!))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.tracker_delete_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onEvent(TrackerEvent.DeletePeriodDismissed) }) {
                    Text(stringResource(R.string.tracker_cancel))
                }
            }
        )
    }
}
