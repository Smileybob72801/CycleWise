package com.veleda.cyclewise.ui.settings.pages

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.BuildConfig
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.usecases.DebugSeederUseCase
import com.veleda.cyclewise.ui.components.MedicalDisclaimer
import com.veleda.cyclewise.ui.settings.SettingsEvent
import com.veleda.cyclewise.ui.settings.SettingsUiState
import com.veleda.cyclewise.ui.settings.components.SettingsSectionCard
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.coroutines.launch
import org.koin.core.scope.Scope

/**
 * Page 3 — About: App info, version dialog, and developer tools (debug only).
 */
@Composable
internal fun AboutPage(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    session: Scope?,
) {
    val dims = LocalDimensions.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.md),
        verticalArrangement = Arrangement.spacedBy(dims.md)
    ) {
        Spacer(Modifier.height(dims.sm))

        // ── About Card ──────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.about_title)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name)) },
                supportingContent = { Text(stringResource(R.string.about_description)) },
                leadingContent = {
                    Icon(Icons.Default.Info, contentDescription = null)
                },
                modifier = Modifier.clickable { onEvent(SettingsEvent.ShowAboutDialog) }
            )
        }

        if (uiState.showAboutDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(SettingsEvent.DismissAboutDialog) },
                title = { Text(stringResource(R.string.app_name)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(dims.sm)) {
                        Text(stringResource(R.string.about_description))
                        Text(stringResource(R.string.about_version_label, BuildConfig.VERSION_NAME))
                        Text(stringResource(R.string.about_license))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onEvent(SettingsEvent.DismissAboutDialog) }) {
                        Text(stringResource(R.string.about_close))
                    }
                }
            )
        }

        // ── About Health Content Card ────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_about_health_content_header)) {
            Text(
                text = stringResource(R.string.settings_about_health_content_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = dims.md),
            )
            MedicalDisclaimer(modifier = Modifier.padding(horizontal = dims.md))
        }

        // ── Developer Card (debug only) ──────────────────────────────
        if (BuildConfig.DEBUG) {
            SettingsSectionCard(title = stringResource(R.string.settings_developer_title)) {
                Button(
                    enabled = session != null,
                    onClick = {
                        session?.let {
                            val seeder: DebugSeederUseCase = it.get()
                            scope.launch {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_seeding),
                                    Toast.LENGTH_SHORT
                                ).show()
                                seeder()
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_seeding_complete),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier.padding(horizontal = dims.md)
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(dims.sm))
                    Text(stringResource(R.string.settings_seed_button))
                }
                if (session == null) {
                    Text(
                        stringResource(R.string.settings_developer_locked),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = dims.md)
                    )
                }
            }
        }

        Spacer(Modifier.height(dims.xl))
    }
}
