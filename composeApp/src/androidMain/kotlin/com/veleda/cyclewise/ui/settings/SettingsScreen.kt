package com.veleda.cyclewise.ui.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.nav.NavRoute
import com.veleda.cyclewise.ui.settings.pages.AboutPage
import com.veleda.cyclewise.ui.settings.pages.AppearancePage
import com.veleda.cyclewise.ui.settings.pages.GeneralPage
import com.veleda.cyclewise.ui.settings.pages.NotificationsPage
import com.veleda.cyclewise.ui.theme.LocalDimensions
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.core.scope.Scope

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
    val generalState by viewModel.generalState.collectAsState()
    val appearanceState by viewModel.appearanceState.collectAsState()
    val notificationState by viewModel.notificationState.collectAsState()
    val aboutState by viewModel.aboutState.collectAsState()
    val session = koin.getScopeOrNull("session")

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.DataDeleted -> {
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
            generalState = generalState,
            appearanceState = appearanceState,
            notificationState = notificationState,
            aboutState = aboutState,
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
 * Accepts the four sub-state objects and an event callback instead of direct dependencies,
 * so it can be tested in isolation without navigation, DI, or ViewModel concerns.
 *
 * @param generalState       The current General page state from [SettingsViewModel].
 * @param appearanceState    The current Appearance page state from [SettingsViewModel].
 * @param notificationState  The current Notifications page state from [SettingsViewModel].
 * @param aboutState         The current About page state from [SettingsViewModel].
 * @param onEvent            Event dispatcher for [SettingsEvent] variants.
 * @param session            The Koin session scope, or `null` when the app is locked.
 * @param onLockNow          Callback invoked when the user taps "Lock Now".
 * @param modifier           Modifier applied to the root column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    generalState: GeneralSettingsState,
    appearanceState: AppearanceSettingsState,
    notificationState: NotificationSettingsState,
    aboutState: AboutSettingsState,
    onEvent: (SettingsEvent) -> Unit,
    session: Scope?,
    onLockNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()

    // Predictive back: return to the General page from any sub-tab.
    // Disabled on the General page so the system handles back normally (navigate out).
    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(PAGE_GENERAL)
        }
    }

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
                PAGE_GENERAL -> GeneralPage(generalState, onEvent, session, onLockNow)
                PAGE_APPEARANCE -> AppearancePage(appearanceState, onEvent)
                PAGE_NOTIFICATIONS -> NotificationsPage(notificationState, onEvent)
                PAGE_ABOUT -> AboutPage(aboutState, onEvent, session)
            }
        }
    }
}
