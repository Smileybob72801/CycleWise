package com.veleda.cyclewise.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.components.ContentContainer
import com.veleda.cyclewise.ui.nav.NavRoute
import com.veleda.cyclewise.ui.settings.pages.AboutPage
import com.veleda.cyclewise.ui.settings.pages.AppearancePage
import com.veleda.cyclewise.ui.settings.pages.ColorsPage
import com.veleda.cyclewise.domain.usecases.DebugSeederUseCase
import com.veleda.cyclewise.session.SessionManager
import com.veleda.cyclewise.ui.settings.pages.NotificationsPage
import com.veleda.cyclewise.ui.settings.pages.SecurityPage
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

/** Number of pages in the settings pager. */
private const val PAGE_COUNT = 5

/** Page indices for the settings pager. */
private const val PAGE_SECURITY = 0
private const val PAGE_APPEARANCE = 1
private const val PAGE_COLORS = 2
private const val PAGE_NOTIFICATIONS = 3
private const val PAGE_ABOUT = 4

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
    val sessionManager: SessionManager = koin.get()
    val securityState by viewModel.securityState.collectAsState()
    val appearanceState by viewModel.appearanceState.collectAsState()
    val colorsState by viewModel.colorsState.collectAsState()
    val notificationState by viewModel.notificationState.collectAsState()
    val aboutState by viewModel.aboutState.collectAsState()
    val context = LocalContext.current
    val passphraseChangedMessage = stringResource(R.string.settings_change_passphrase_success)
    val exportSuccessMessage = stringResource(R.string.settings_export_success)
    val importSuccessMessage = stringResource(R.string.backup_import_success)

    // SAF launcher for export (create a new file)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let { viewModel.onEvent(SettingsEvent.ExportToUri(it)) }
    }

    // SAF launcher for import (open an existing file)
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.onEvent(SettingsEvent.ImportFileSelected(it)) }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.DataDeleted -> {
                    navController.navigate(NavRoute.Passphrase.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }

                is SettingsEffect.PassphraseChanged -> {
                    Toast.makeText(context, passphraseChangedMessage, Toast.LENGTH_SHORT).show()
                    navController.navigate(NavRoute.Passphrase.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }

                is SettingsEffect.LaunchExportPicker -> {
                    exportLauncher.launch(effect.suggestedName)
                }

                is SettingsEffect.LaunchImportPicker -> {
                    importLauncher.launch(arrayOf("*/*"))
                }

                is SettingsEffect.ExportSuccess -> {
                    Toast.makeText(context, exportSuccessMessage, Toast.LENGTH_SHORT).show()
                }

                is SettingsEffect.BackupImported -> {
                    Toast.makeText(context, importSuccessMessage, Toast.LENGTH_LONG).show()
                    navController.navigate(NavRoute.Passphrase.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        SettingsContent(
            securityState = securityState,
            appearanceState = appearanceState,
            colorsState = colorsState,
            notificationState = notificationState,
            aboutState = aboutState,
            onEvent = viewModel::onEvent,
            isSessionActive = sessionManager.isSessionActive,
            onLockNow = {
                sessionManager.closeSession()
                navController.navigate(NavRoute.Passphrase.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onSeedDebugData = {
                koin.getScopeOrNull("session")?.let {
                    val seeder: DebugSeederUseCase = it.get()
                    seeder
                }
            },
            modifier = Modifier.padding(padding)
        )
    }
}

/**
 * Testable content composable that renders the settings UI as a 5-page [HorizontalPager].
 *
 * Accepts the five sub-state objects and an event callback instead of direct dependencies,
 * so it can be tested in isolation without navigation, DI, or ViewModel concerns.
 *
 * @param securityState      The current Security page state from [SettingsViewModel].
 * @param appearanceState    The current Appearance page state from [SettingsViewModel].
 * @param colorsState        The current Colors page state from [SettingsViewModel].
 * @param notificationState  The current Notifications page state from [SettingsViewModel].
 * @param aboutState         The current About page state from [SettingsViewModel].
 * @param onEvent            Event dispatcher for [SettingsEvent] variants.
 * @param isSessionActive    Whether the session scope is active (database unlocked).
 * @param onLockNow          Callback invoked when the user taps "Lock Now".
 * @param onSeedDebugData    Returns a [DebugSeederUseCase] if the session is active, or `null`.
 *                           Only used in debug builds for the developer seeder button.
 * @param modifier           Modifier applied to the root column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    securityState: SecuritySettingsState,
    appearanceState: AppearanceSettingsState,
    colorsState: ColorsSettingsState,
    notificationState: NotificationSettingsState,
    aboutState: AboutSettingsState,
    onEvent: (SettingsEvent) -> Unit,
    isSessionActive: Boolean,
    onLockNow: () -> Unit,
    onExportClicked: () -> Unit = { onEvent(SettingsEvent.ExportBackupClicked) },
    onImportClicked: () -> Unit = { onEvent(SettingsEvent.ImportBackupClicked) },
    onSeedDebugData: (() -> DebugSeederUseCase?)? = null,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()

    // Predictive back: return to the Security page from any sub-tab.
    // Disabled on the Security page so the system handles back normally (navigate out).
    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(PAGE_SECURITY)
        }
    }

    val pageLabels = listOf(
        stringResource(R.string.settings_page_security),
        stringResource(R.string.settings_page_appearance),
        stringResource(R.string.settings_page_colors),
        stringResource(R.string.settings_page_notifications),
        stringResource(R.string.settings_page_about),
    )

    ContentContainer(modifier = modifier) {
        Column(Modifier.fillMaxSize()) {
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
                    PAGE_SECURITY -> SecurityPage(
                        securityState,
                        onEvent,
                        isSessionActive,
                        onLockNow,
                        onExportClicked,
                        onImportClicked,
                    )
                    PAGE_APPEARANCE -> AppearancePage(appearanceState, onEvent)
                    PAGE_COLORS -> ColorsPage(colorsState, onEvent)
                    PAGE_NOTIFICATIONS -> NotificationsPage(notificationState, onEvent)
                    PAGE_ABOUT -> AboutPage(aboutState, onEvent, isSessionActive, onSeedDebugData)
                }
            }
        }
    }
}
