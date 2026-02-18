package com.veleda.cyclewise.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.veleda.cyclewise.BuildConfig
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.usecases.DebugSeederUseCase
import com.veleda.cyclewise.reminders.ReminderScheduler
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.nav.NavRoute
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import kotlin.math.roundToInt

/**
 * Top-level settings screen with Koin-injected dependencies.
 *
 * Delegates all rendering to [SettingsContent] for testability, following the same
 * pattern as [com.veleda.cyclewise.ui.insights.InsightsScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val koin = getKoin()
    val appSettings: AppSettings = koin.get()
    val reminderScheduler: ReminderScheduler = koin.get()
    val session = koin.getScopeOrNull("session")

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        SettingsContent(
            appSettings = appSettings,
            session = session,
            reminderScheduler = reminderScheduler,
            onLockNow = {
                session?.close()
                navController.navigate(NavRoute.Passphrase.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier.padding(padding)
        )
    }
}

/**
 * Testable content composable that renders the settings UI grouped into [OutlinedCard] sections.
 *
 * Accepts primitives and callbacks instead of a NavController or Koin scope, so it can be
 * tested in isolation without navigation or DI concerns.
 *
 * @param appSettings       The [AppSettings] for reading/writing preferences.
 * @param session           The Koin session scope, or `null` when the app is locked.
 * @param reminderScheduler The [ReminderScheduler] for notification scheduling.
 * @param onLockNow         Callback invoked when the user taps "Lock Now".
 * @param modifier          Modifier applied to the root scrollable column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    appSettings: AppSettings,
    session: org.koin.core.scope.Scope?,
    reminderScheduler: ReminderScheduler,
    onLockNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = LocalDimensions.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val autolock by appSettings.autolockMinutes.collectAsState(initial = 10)
    val topSymptomsCount by appSettings.topSymptomsCount.collectAsState(initial = 3)
    val showMood by appSettings.showMoodInSummary.collectAsState(initial = true)
    val showEnergy by appSettings.showEnergyInSummary.collectAsState(initial = true)
    val showLibido by appSettings.showLibidoInSummary.collectAsState(initial = true)

    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.md),
        verticalArrangement = Arrangement.spacedBy(dims.md)
    ) {
        Spacer(Modifier.height(dims.sm))

        // ── Security Card ────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_security)) {
            Text(
                stringResource(R.string.settings_autolock_title),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = dims.md)
            )

            val options = listOf(5, 10, 15, 30)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.md)
            ) {
                options.forEachIndexed { index, minutes ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        onClick = { scope.launch { appSettings.setAutolockMinutes(minutes) } },
                        selected = autolock == minutes,
                        label = { Text("$minutes ${stringResource(R.string.settings_autolock_minutes_unit)}") }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = dims.md))

            Button(
                enabled = session != null,
                onClick = onLockNow,
                modifier = Modifier.padding(horizontal = dims.md)
            ) {
                Text(stringResource(R.string.settings_lock_button))
            }
            if (session == null) {
                Text(
                    stringResource(R.string.settings_locked_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = dims.md)
                )
            }
        }

        // ── Display Card ─────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_display)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_mood_label)) },
                supportingContent = { Text(stringResource(R.string.show_mood_description)) },
                trailingContent = {
                    Switch(
                        checked = showMood,
                        onCheckedChange = { scope.launch { appSettings.setShowMoodInSummary(it) } }
                    )
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_energy_label)) },
                supportingContent = { Text(stringResource(R.string.show_energy_description)) },
                trailingContent = {
                    Switch(
                        checked = showEnergy,
                        onCheckedChange = { scope.launch { appSettings.setShowEnergyInSummary(it) } }
                    )
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_libido_label)) },
                supportingContent = { Text(stringResource(R.string.show_libido_description)) },
                trailingContent = {
                    Switch(
                        checked = showLibido,
                        onCheckedChange = { scope.launch { appSettings.setShowLibidoInSummary(it) } }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = dims.md))

            PhaseVisibilitySettings(appSettings, showTitle = false)
        }

        // ── Customization Card ───────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_customization)) {
            PhaseColorSettings(appSettings, showTitle = false)
        }

        // ── Notifications Card ───────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_notifications)) {
            ReminderSettings(appSettings, reminderScheduler, showTitle = false)
        }

        // ── Insights Card ────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_insights_title)) {
            Text(
                stringResource(R.string.settings_top_symptoms, topSymptomsCount),
                modifier = Modifier.padding(horizontal = dims.md)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.md),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (1..5).forEach { value ->
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (value == topSymptomsCount)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (value == topSymptomsCount)
                            FontWeight.Bold
                        else
                            FontWeight.Normal
                    )
                }
            }
            Slider(
                value = topSymptomsCount.toFloat(),
                onValueChange = { newValue ->
                    scope.launch { appSettings.setTopSymptomsCount(newValue.roundToInt()) }
                },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.padding(horizontal = dims.md)
            )
        }

        // ── About Card ──────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.about_title)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name)) },
                supportingContent = { Text(stringResource(R.string.about_description)) },
                leadingContent = {
                    Icon(Icons.Default.Info, contentDescription = null)
                },
                modifier = Modifier.clickable { showAboutDialog = true }
            )
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text(stringResource(R.string.app_name)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(dims.sm)) {
                        Text(stringResource(R.string.about_description))
                        Text(stringResource(R.string.about_version_label, BuildConfig.VERSION_NAME))
                        Text(stringResource(R.string.about_license))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text(stringResource(R.string.about_close))
                    }
                }
            )
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

        Spacer(Modifier.height(dims.md))
    }
}

/**
 * Reusable section wrapper that groups related settings inside an [OutlinedCard]
 * with a colored section title.
 *
 * The card applies only vertical padding. The [title] and any non-[ListItem] content
 * should add their own horizontal padding (`dims.md`). [ListItem] composables handle
 * their own 16 dp horizontal padding natively, avoiding double padding.
 *
 * @param title   Section header text, rendered in [titleMedium] with [primary] color.
 * @param modifier Modifier applied to the outer [OutlinedCard].
 * @param content  Card body content (typically [ListItem]s, [Switch]es, and custom rows).
 */
@Composable
private fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val dims = LocalDimensions.current

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(vertical = dims.md),
            verticalArrangement = Arrangement.spacedBy(dims.sm)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = dims.md)
            )
            content()
        }
    }
}
