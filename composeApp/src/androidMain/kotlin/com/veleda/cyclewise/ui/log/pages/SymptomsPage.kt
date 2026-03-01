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
import androidx.compose.material.icons.outlined.LocalHospital
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.models.SymptomLog
import com.veleda.cyclewise.ui.log.MAX_NAME_LENGTH
import com.veleda.cyclewise.ui.log.components.SectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions

@Composable
internal fun SymptomsPage(
    loggedSymptoms: List<SymptomLog>,
    symptomLibrary: List<Symptom>,
    onToggleSymptom: (Symptom) -> Unit,
    onCreateAndAddSymptom: (String) -> Unit,
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

        if (loggedSymptoms.isNotEmpty()) {
            Text(
                text = stringResource(R.string.daily_log_symptoms_count, loggedSymptoms.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(
            title = stringResource(R.string.daily_log_symptoms_title),
            icon = Icons.Outlined.LocalHospital,
            onInfoClick = { onShowEducationalSheet("Symptoms") },
        ) {
            SymptomLogger(
                loggedSymptoms = loggedSymptoms,
                symptomLibrary = symptomLibrary,
                onToggleSymptom = onToggleSymptom,
                onCreateAndAddSymptom = onCreateAndAddSymptom,
            )
        }

        Spacer(Modifier.height(dims.xl))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SymptomLogger(
    loggedSymptoms: List<SymptomLog>,
    symptomLibrary: List<Symptom>,
    onToggleSymptom: (Symptom) -> Unit,
    onCreateAndAddSymptom: (String) -> Unit
) {
    var newSymptomName by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.md)) {
        if (symptomLibrary.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.sm),
            ) {
                symptomLibrary.forEach { symptom ->
                    val isSelected = loggedSymptoms.any { it.symptomId == symptom.id }
                    FilterChip(
                        modifier = Modifier.testTag("chip-${symptom.name.uppercase()}"),
                        selected = isSelected,
                        onClick = { onToggleSymptom(symptom) },
                        label = { Text(symptom.name) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = newSymptomName,
            onValueChange = { if (it.length <= MAX_NAME_LENGTH) newSymptomName = it },
            label = { Text(stringResource(R.string.daily_log_add_symptom)) },
            modifier = Modifier
                .testTag("create-symptom-textbox")
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (newSymptomName.isNotBlank()) {
                    onCreateAndAddSymptom(newSymptomName)
                    newSymptomName = ""
                }
            }),
            trailingIcon = {
                IconButton(
                    onClick = {
                        onCreateAndAddSymptom(newSymptomName)
                        newSymptomName = ""
                    },
                    enabled = newSymptomName.isNotBlank(),
                    modifier = Modifier.testTag("create-symptom-button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.daily_log_create_symptom))
                }
            }
        )
    }
}
