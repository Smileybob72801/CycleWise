package com.veleda.cyclewise.ui.log.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.MedicationLog
import com.veleda.cyclewise.ui.log.MAX_NAME_LENGTH
import com.veleda.cyclewise.ui.log.components.SectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions

@Composable
internal fun MedicationsPage(
    loggedMedications: List<MedicationLog>,
    medicationLibrary: List<Medication>,
    onToggleMedication: (Medication) -> Unit,
    onCreateAndAddMedication: (String) -> Unit,
    onShowEducationalSheet: (String) -> Unit,
) {
    val dims = LocalDimensions.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.md),
        verticalArrangement = Arrangement.spacedBy(dims.md),
    ) {
        Spacer(Modifier.height(dims.sm))

        if (loggedMedications.isNotEmpty()) {
            Text(
                text = stringResource(R.string.daily_log_medications_count, loggedMedications.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(
            title = stringResource(R.string.daily_log_medications_title),
            icon = Icons.Outlined.MedicalServices,
            onInfoClick = { onShowEducationalSheet("Medication") },
        ) {
            MedicationLogger(
                loggedMedications = loggedMedications,
                medicationLibrary = medicationLibrary,
                onToggleMedication = onToggleMedication,
                onCreateAndAddMedication = onCreateAndAddMedication,
            )
        }

        Spacer(Modifier.height(dims.xl))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun MedicationLogger(
    loggedMedications: List<MedicationLog>,
    medicationLibrary: List<Medication>,
    onToggleMedication: (Medication) -> Unit,
    onCreateAndAddMedication: (String) -> Unit
) {
    var newMedicationName by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.md)) {
        if (medicationLibrary.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm),
            ) {
                medicationLibrary.forEach { medication ->
                    val isSelected = loggedMedications.any { it.medicationId == medication.id }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleMedication(medication) },
                        label = { Text(medication.name) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = newMedicationName,
            onValueChange = { if (it.length <= MAX_NAME_LENGTH) newMedicationName = it },
            label = { Text(stringResource(R.string.daily_log_add_medication)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                onCreateAndAddMedication(newMedicationName)
                newMedicationName = ""
            }),
            trailingIcon = {
                IconButton(
                    onClick = {
                        onCreateAndAddMedication(newMedicationName)
                        newMedicationName = ""
                    },
                    enabled = newMedicationName.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.daily_log_create_medication))
                }
            }
        )
    }
}
