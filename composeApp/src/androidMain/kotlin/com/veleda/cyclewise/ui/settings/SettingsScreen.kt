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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.veleda.cyclewise.ui.nav.NavRoute
import com.veleda.cyclewise.ui.components.EducationalBottomSheet
import com.veleda.cyclewise.ui.components.InfoButton
import com.veleda.cyclewise.ui.components.MedicalDisclaimer
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.core.scope.Scope
import kotlin.math.roundToInt

/** Number of pages in the settings pager. */
private const val PAGE_COUNT = 4

/** Page indices for the settings pager. */
private const val PAGE_GENERAL = 0
private const val PAGE_APPEARANCE = 1
private const val PAGE_NOTIFICATIONS = 2
private const val PAGE_ABOUT = 3

/**
 * Top-level settings screen with Koin-injected dependencies.
 *
 * Obtains [SettingsViewModel] via `koinViewModel()` and delegates all rendering to
 * [SettingsContent] for testability. Session-specific operations (Lock Now, Debug Seeder)
 * are handled at this level via Koin scope access.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val koin = getKoin()
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val session = koin.getScopeOrNull("session")

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        SettingsContent(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            session = session,
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
 * Testable content composable that renders the settings UI as a 4-page [HorizontalPager].
 *
 * Accepts [SettingsUiState] and an event callback instead of direct dependencies,
 * so it can be tested in isolation without navigation, DI, or ViewModel concerns.
 *
 * @param uiState    The current settings UI state from [SettingsViewModel].
 * @param onEvent    Event dispatcher for [SettingsEvent] variants.
 * @param session    The Koin session scope, or `null` when the app is locked.
 * @param onLockNow  Callback invoked when the user taps "Lock Now".
 * @param modifier   Modifier applied to the root column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    session: Scope?,
    onLockNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()

    val pageLabels = listOf(
        stringResource(R.string.settings_page_general),
        stringResource(R.string.settings_page_appearance),
        stringResource(R.string.settings_page_notifications),
        stringResource(R.string.settings_page_about),
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Page indicator tabs
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = dims.md,
            modifier = Modifier.fillMaxWidth(),
        ) {
            pageLabels.forEachIndexed { index, label ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
        }

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                PAGE_GENERAL -> GeneralPage(uiState, onEvent, session, onLockNow)
                PAGE_APPEARANCE -> AppearancePage(uiState, onEvent)
                PAGE_NOTIFICATIONS -> NotificationsPage(uiState, onEvent)
                PAGE_ABOUT -> AboutPage(uiState, onEvent, session)
            }
        }
    }
}

/**
 * Page 0 — General: Security settings and Insights configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralPage(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    session: Scope?,
    onLockNow: () -> Unit,
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
                        onClick = { onEvent(SettingsEvent.AutolockChanged(minutes)) },
                        selected = uiState.autolockMinutes == minutes,
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

        // ── Insights Card ────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_insights_title)) {
            Text(
                stringResource(R.string.settings_top_symptoms, uiState.topSymptomsCount),
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
                        color = if (value == uiState.topSymptomsCount)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (value == uiState.topSymptomsCount)
                            FontWeight.Bold
                        else
                            FontWeight.Normal
                    )
                }
            }
            Slider(
                value = uiState.topSymptomsCount.toFloat(),
                onValueChange = { newValue ->
                    onEvent(SettingsEvent.TopSymptomsCountChanged(newValue.roundToInt()))
                },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.padding(horizontal = dims.md)
            )
        }

        // ── Tutorial Card ──────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_tutorial)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_reset_hints)) },
                supportingContent = { Text(stringResource(R.string.settings_reset_hints_description)) },
                modifier = Modifier.clickable { onEvent(SettingsEvent.ResetTutorialHints) }
            )
        }

        // Show a Toast when hints are successfully reset.
        if (uiState.showHintResetConfirmation) {
            val context = LocalContext.current
            val message = stringResource(R.string.settings_reset_hints_confirmation)
            LaunchedEffect(Unit) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        Spacer(Modifier.height(dims.xl))
    }
}

/**
 * Page 1 — Appearance: Display toggles, phase visibility, and phase colors.
 */
@Composable
private fun AppearancePage(
    uiState: SettingsUiState,
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

        // ── Theme Card ───────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_theme)) {
            val modes = ThemeMode.entries
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.md)
            ) {
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = modes.size
                        ),
                        onClick = { onEvent(SettingsEvent.ThemeModeChanged(mode)) },
                        selected = uiState.themeMode == mode,
                        label = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
                                    ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
                                    ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
                                }
                            )
                        }
                    )
                }
            }
        }

        // ── Display Card ─────────────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_display)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_mood_label)) },
                supportingContent = { Text(stringResource(R.string.show_mood_description)) },
                trailingContent = {
                    Switch(
                        checked = uiState.showMood,
                        onCheckedChange = { onEvent(SettingsEvent.ShowMoodToggled(it)) }
                    )
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_energy_label)) },
                supportingContent = { Text(stringResource(R.string.show_energy_description)) },
                trailingContent = {
                    Switch(
                        checked = uiState.showEnergy,
                        onCheckedChange = { onEvent(SettingsEvent.ShowEnergyToggled(it)) }
                    )
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.show_libido_label)) },
                supportingContent = { Text(stringResource(R.string.show_libido_description)) },
                trailingContent = {
                    Switch(
                        checked = uiState.showLibido,
                        onCheckedChange = { onEvent(SettingsEvent.ShowLibidoToggled(it)) }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = dims.md))

            PhaseVisibilitySettings(
                showFollicular = uiState.showFollicular,
                showOvulation = uiState.showOvulation,
                showLuteal = uiState.showLuteal,
                onFollicularToggled = { onEvent(SettingsEvent.ShowFollicularToggled(it)) },
                onOvulationToggled = { onEvent(SettingsEvent.ShowOvulationToggled(it)) },
                onLutealToggled = { onEvent(SettingsEvent.ShowLutealToggled(it)) },
                showTitle = false,
            )
        }

        // ── Customization Card ───────────────────────────────────────
        SettingsSectionCard(
            title = stringResource(R.string.settings_section_customization),
            onInfoClick = { onEvent(SettingsEvent.ShowEducationalSheet("CyclePhase.Colors")) },
        ) {
            PhaseColorSettings(
                menstruationHex = uiState.menstruationColorHex,
                follicularHex = uiState.follicularColorHex,
                ovulationHex = uiState.ovulationColorHex,
                lutealHex = uiState.lutealColorHex,
                onMenstruationColorChanged = { onEvent(SettingsEvent.MenstruationColorChanged(it)) },
                onFollicularColorChanged = { onEvent(SettingsEvent.FollicularColorChanged(it)) },
                onOvulationColorChanged = { onEvent(SettingsEvent.OvulationColorChanged(it)) },
                onLutealColorChanged = { onEvent(SettingsEvent.LutealColorChanged(it)) },
                onResetDefaults = { onEvent(SettingsEvent.ResetPhaseColorsToDefaults) },
                showTitle = false,
            )
        }

        Spacer(Modifier.height(dims.xl))
    }

    uiState.educationalArticles?.let { articles ->
        EducationalBottomSheet(
            articles = articles,
            onDismiss = { onEvent(SettingsEvent.DismissEducationalSheet) },
        )
    }
}

/**
 * Page 2 — Notifications: Period prediction, medication, and hydration reminders.
 */
@Composable
private fun NotificationsPage(
    uiState: SettingsUiState,
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

        // ── Notifications Card ───────────────────────────────────────
        SettingsSectionCard(title = stringResource(R.string.settings_section_notifications)) {
            ReminderSettings(
                periodEnabled = uiState.periodReminderEnabled,
                periodDaysBefore = uiState.periodDaysBefore,
                periodPrivacyAccepted = uiState.periodPrivacyAccepted,
                medicationEnabled = uiState.medicationReminderEnabled,
                medicationHour = uiState.medicationHour,
                medicationMinute = uiState.medicationMinute,
                hydrationEnabled = uiState.hydrationReminderEnabled,
                hydrationGoalCups = uiState.hydrationGoalCups,
                hydrationFrequencyHours = uiState.hydrationFrequencyHours,
                hydrationStartHour = uiState.hydrationStartHour,
                hydrationEndHour = uiState.hydrationEndHour,
                showPermissionRationale = uiState.showPermissionRationale,
                showPrivacyDialog = uiState.showPrivacyDialog,
                onEvent = onEvent,
                showTitle = false,
            )
        }

        Spacer(Modifier.height(dims.xl))
    }
}

/**
 * Page 3 — About: App info, version dialog, and developer tools (debug only).
 */
@Composable
private fun AboutPage(
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

/**
 * Reusable section wrapper that groups related settings inside an [OutlinedCard]
 * with a colored section title.
 *
 * The card applies only vertical padding. The [title] and any non-[ListItem] content
 * should add their own horizontal padding (`dims.md`). [ListItem] composables handle
 * their own 16 dp horizontal padding natively, avoiding double padding.
 *
 * @param title       Section header text, rendered in [titleMedium] with [primary] color.
 * @param modifier    Modifier applied to the outer [OutlinedCard].
 * @param onInfoClick Optional callback for an info button aligned to the end of the title row.
 *                    When non-null, an [InfoButton] is displayed. When null, no button is shown.
 * @param content     Card body content (typically [ListItem]s, [Switch]es, and custom rows).
 */
@Composable
private fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    onInfoClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dims = LocalDimensions.current

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = dims.md),
            verticalArrangement = Arrangement.spacedBy(dims.sm)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                if (onInfoClick != null) {
                    InfoButton(
                        onClick = onInfoClick,
                        contentDescription = stringResource(R.string.educational_info_button_cd, title),
                    )
                }
            }
            content()
        }
    }
}
