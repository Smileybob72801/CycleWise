package com.veleda.cyclewise.ui.tracker

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import kotlinx.datetime.LocalDate
import com.veleda.cyclewise.R
import androidx.navigation.NavController
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.coachmark.CoachMarkOverlay
import com.veleda.cyclewise.ui.coachmark.CoachMarkState
import com.veleda.cyclewise.ui.coachmark.HintKey
import com.veleda.cyclewise.ui.coachmark.HintPreferences
import com.veleda.cyclewise.ui.coachmark.coachMarkTarget
import com.veleda.cyclewise.ui.nav.NavRoute
import com.veleda.cyclewise.ui.components.ContentContainer
import com.veleda.cyclewise.ui.components.EducationalBottomSheet
import com.veleda.cyclewise.ui.components.InfoButton
import kotlinx.coroutines.flow.first
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.theme.buildCyclePhasePalette
import com.veleda.cyclewise.ui.utils.toLocalizedDateString
import com.veleda.cyclewise.ui.utils.toLocalizedMonthYearString
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import com.veleda.cyclewise.domain.usecases.TutorialCleanupUseCase
import com.veleda.cyclewise.settings.parseSeedManifest
import com.veleda.cyclewise.settings.toJson
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import kotlin.time.Clock
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.YearMonth as JavaYearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(navController: NavController) {
    val dims = LocalDimensions.current
    val viewModel: TrackerViewModel = koinInject(scope = getKoin().getScope("session"))
    val uiState by viewModel.uiState.collectAsState()
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // Coach mark system
    val koin = getKoin()
    val hintPreferences: HintPreferences = koin.get()
    val coachMarkState = remember { CoachMarkState(hintPreferences, coroutineScope) }

    val activeHint by coachMarkState.active.collectAsState()
    val pendingKey by coachMarkState.pendingHintKey.collectAsState()

    // Tracks whether the Tracker walkthrough was started this composition.
    var trackerWalkthroughActive by remember { mutableStateOf(false) }

    // Predictive back: dismiss the coach mark walkthrough instead of navigating away.
    // When no walkthrough is active, the handler is disabled and back navigates normally.
    BackHandler(enabled = activeHint != null || pendingKey != null) {
        coachMarkState.skipAll(TRACKER_HINTS)
        trackerWalkthroughActive = false
    }

    // Start the Tracker walkthrough if the user hasn't seen it yet.
    LaunchedEffect(Unit) {
        val seen = hintPreferences.isHintSeen(HintKey.TRACKER_WELCOME).first()
        if (!seen) {
            trackerWalkthroughActive = true
            TRACKER_HINTS[HintKey.TRACKER_WELCOME]?.let { coachMarkState.showHint(it) }
        }
    }

    val appSettings: AppSettings = koin.get()

    // Detect Tracker walkthrough completion (normal advance past last hint, or skipAll)
    // and clean up seed data.
    LaunchedEffect(activeHint, pendingKey, trackerWalkthroughActive) {
        if (trackerWalkthroughActive && activeHint == null && pendingKey == null) {
            trackerWalkthroughActive = false
            val manifestJson = appSettings.seedManifestJson.first()
            if (manifestJson.isNotEmpty()) {
                val manifest = parseSeedManifest(manifestJson)
                if (manifest != null) {
                    val sessionScope = koin.getScope("session")
                    val cleanup: TutorialCleanupUseCase = sessionScope.get()
                    cleanup(manifest)
                }
                appSettings.clearSeedManifest()
            }
        }
    }

    // Track periods created by user during the Tracker walkthrough (long-press/drag).
    LaunchedEffect(trackerWalkthroughActive) {
        if (!trackerWalkthroughActive) return@LaunchedEffect
        val manifestJson = appSettings.seedManifestJson.first()
        if (manifestJson.isEmpty()) return@LaunchedEffect
        val initialManifest = parseSeedManifest(manifestJson) ?: return@LaunchedEffect
        val trackedIds = initialManifest.periodUuids.toMutableSet()

        viewModel.uiState.collect { state ->
            val newIds = state.periods.map { it.id }.filter { it !in trackedIds }
            if (newIds.isNotEmpty()) {
                trackedIds.addAll(newIds)
                val latest = parseSeedManifest(
                    appSettings.seedManifestJson.first()
                ) ?: return@collect
                val updated = latest.copy(
                    periodUuids = (latest.periodUuids.toSet() + newIds).toList()
                )
                appSettings.setSeedManifestJson(updated.toJson())
            }
        }
    }

    val showMood by appSettings.showMoodInSummary.collectAsState(initial = true)
    val showEnergy by appSettings.showEnergyInSummary.collectAsState(initial = true)
    val showLibido by appSettings.showLibidoInSummary.collectAsState(initial = true)

    val showFollicular by appSettings.showFollicularPhase.collectAsState(initial = true)
    val showOvulation by appSettings.showOvulationPhase.collectAsState(initial = true)
    val showLuteal by appSettings.showLutealPhase.collectAsState(initial = true)

    val menstruationHex by appSettings.menstruationColor.collectAsState(initial = CyclePhaseColors.DEFAULT_MENSTRUATION_HEX)
    val follicularHex by appSettings.follicularColor.collectAsState(initial = CyclePhaseColors.DEFAULT_FOLLICULAR_HEX)
    val ovulationHex by appSettings.ovulationColor.collectAsState(initial = CyclePhaseColors.DEFAULT_OVULATION_HEX)
    val lutealHex by appSettings.lutealColor.collectAsState(initial = CyclePhaseColors.DEFAULT_LUTEAL_HEX)

    val darkTheme = isSystemInDarkTheme()
    val customColors: Map<CyclePhase, Color>? = remember(menstruationHex, follicularHex, ovulationHex, lutealHex) {
        val map = buildMap {
            parseHexColor(menstruationHex)?.let { put(CyclePhase.MENSTRUATION, it) }
            parseHexColor(follicularHex)?.let { put(CyclePhase.FOLLICULAR, it) }
            parseHexColor(ovulationHex)?.let { put(CyclePhase.OVULATION, it) }
            parseHexColor(lutealHex)?.let { put(CyclePhase.LUTEAL, it) }
        }
        map.ifEmpty { null }
    }
    val palette = remember(darkTheme, customColors) {
        buildCyclePhasePalette(darkTheme, customColors)
    }

    val phaseVisible: Map<CyclePhase, Boolean> = remember(showFollicular, showOvulation, showLuteal) {
        mapOf(
            CyclePhase.MENSTRUATION to true,
            CyclePhase.FOLLICULAR to showFollicular,
            CyclePhase.OVULATION to showOvulation,
            CyclePhase.LUTEAL to showLuteal
        )
    }

    // Trigger auto-close logic when the screen is first composed/entered.
    LaunchedEffect(Unit) {
        viewModel.onEvent(TrackerEvent.ScreenEntered)
    }

    // Collect one-time effects (navigation).
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TrackerEffect.NavigateToDailyLog -> {
                    navController.navigate(
                        NavRoute.DailyLog.createRoute(effect.date)
                    )
                }
            }
        }
    }

    val currentMonth = remember { JavaYearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val calendarState = rememberCalendarState(startMonth, endMonth, currentMonth, firstDayOfWeek)

    val boundsRegistry = remember { DayBoundsRegistry() }
    var dragAnchor by remember { mutableStateOf<LocalDate?>(null) }
    var dragCurrent by remember { mutableStateOf<LocalDate?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    // Coordinates of the calendar container Box for pointer mapping.
    var calendarBoxCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    DayDetailSheet(
        uiState = uiState,
        showMood = showMood,
        showEnergy = showEnergy,
        showLibido = showLibido,
        sheetState = sheetState,
        onEvent = viewModel::onEvent,
    )

    DeletePeriodConfirmationDialog(
        uiState = uiState,
        onEvent = viewModel::onEvent,
    )

    uiState.educationalArticles?.let { articles ->
        EducationalBottomSheet(
            articles = articles,
            onDismiss = { viewModel.onEvent(TrackerEvent.DismissEducationalSheet) },
        )
    }

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        ContentContainer(maxWidth = dims.gridMaxWidth) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.md)
                    .coachMarkTarget(HintKey.TRACKER_NAV, coachMarkState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    coroutineScope.launch {
                        calendarState.animateScrollToMonth(
                            calendarState.firstVisibleMonth.yearMonth.minusMonths(1)
                        )
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.tracker_previous_month)
                    )
                }
                Text(
                    text = calendarState.firstVisibleMonth.yearMonth.toLocalizedMonthYearString(),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = {
                    coroutineScope.launch {
                        calendarState.animateScrollToMonth(
                            calendarState.firstVisibleMonth.yearMonth.plusMonths(1)
                        )
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.tracker_next_month)
                    )
                }
            }

            FilledTonalButton(
                onClick = {
                    coroutineScope.launch {
                        calendarState.animateScrollToMonth(currentMonth)
                    }
                }
            ) {
                Text(stringResource(R.string.tracker_today))
            }

            DaysOfWeekTitle(daysOfWeek = firstDayOfWeek.let {
                val days = JavaDayOfWeek.entries
                days.subList(it.value - 1, days.size) + days.subList(0, it.value - 1)
            })
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = dims.sm)
                    .coachMarkTarget(HintKey.TRACKER_PHASE_LEGEND, coachMarkState),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhaseLegend(
                    palette = palette,
                    phaseVisible = phaseVisible,
                    modifier = Modifier.weight(1f),
                )
                InfoButton(
                    onClick = { viewModel.onEvent(TrackerEvent.ShowEducationalSheet("CyclePhase")) },
                    contentDescription = stringResource(R.string.educational_info_button_cd, stringResource(R.string.tracker_phase_label)),
                )
            }

            CalendarGrid(
                uiState = uiState,
                calendarState = calendarState,
                palette = palette,
                phaseVisible = phaseVisible,
                today = today,
                boundsRegistry = boundsRegistry,
                isDragging = isDragging,
                dragAnchor = dragAnchor,
                dragCurrent = dragCurrent,
                activeHint = activeHint,
                pendingKey = pendingKey,
                coachMarkState = coachMarkState,
                calendarBoxCoords = calendarBoxCoords,
                onCalendarBoxPositioned = { calendarBoxCoords = it },
                onDragStateChanged = { anchor, current, dragging ->
                    dragAnchor = anchor
                    dragCurrent = current
                    isDragging = dragging
                },
                onEvent = viewModel::onEvent,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.height(dims.md))

            AnimatedVisibility(
                visible = uiState.periods.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = dims.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dims.sm),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = stringResource(R.string.tracker_empty_icon_cd),
                        modifier = Modifier.size(dims.iconLg),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.tracker_empty_title),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.tracker_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            AnimatedVisibility(
                visible = uiState.periods.isNotEmpty() && uiState.ongoingPeriod != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val ongoingStartDate = uiState.ongoingPeriod?.startDate?.toLocalizedDateString() ?: ""
                Text(
                    stringResource(R.string.tracker_ongoing_period, ongoingStartDate),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(dims.sm),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(
                visible = uiState.periods.isNotEmpty() && uiState.ongoingPeriod == null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    stringResource(R.string.tracker_instructions_drag),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(dims.sm)
                )
            }
            Spacer(Modifier.height(dims.md))
        }
        }

        // Coach mark overlay draws on top of all screen content.
        CoachMarkOverlay(state = coachMarkState, allDefs = TRACKER_HINTS)
        }
    }
}
