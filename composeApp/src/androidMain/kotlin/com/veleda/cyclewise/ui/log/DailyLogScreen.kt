package com.veleda.cyclewise.ui.log

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.domain.usecases.TutorialCleanupUseCase
import com.veleda.cyclewise.domain.usecases.TutorialSeederUseCase
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.settings.parseSeedManifest
import com.veleda.cyclewise.settings.runSeedCleanupIfNeeded
import com.veleda.cyclewise.settings.toJson
import com.veleda.cyclewise.ui.tracker.TRACKER_HINTS
import com.veleda.cyclewise.ui.coachmark.CoachMarkOverlay
import com.veleda.cyclewise.ui.coachmark.CoachMarkState
import com.veleda.cyclewise.ui.coachmark.HintKey
import com.veleda.cyclewise.ui.coachmark.HintPreferences
import com.veleda.cyclewise.ui.coachmark.coachMarkTarget
import com.veleda.cyclewise.ui.components.ContentContainer
import com.veleda.cyclewise.ui.components.EducationalBottomSheet
import com.veleda.cyclewise.ui.log.pages.MedicationsPage
import com.veleda.cyclewise.ui.log.pages.NotesTagsPage
import com.veleda.cyclewise.ui.log.pages.PeriodPage
import com.veleda.cyclewise.ui.log.pages.SymptomsPage
import com.veleda.cyclewise.ui.log.pages.WellnessPage
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.utils.toLocalizedDateString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf

/** Maximum characters for medication, symptom, and tag name inputs. */
internal const val MAX_NAME_LENGTH = 100

/** Maximum characters for the notes field. */
internal const val MAX_NOTE_LENGTH = 2000

/** Number of pages in the daily log pager. */
private const val PAGE_COUNT = 5

/** Page indices for the daily log pager. */
private const val PAGE_WELLNESS = 0
private const val PAGE_PERIOD = 1
private const val PAGE_SYMPTOMS = 2
private const val PAGE_MEDICATIONS = 3
private const val PAGE_NOTES = 4

/** Delay after pager scroll to let [ScrollableTabRow] finish scrolling to the selected tab. */
private const val TAB_ROW_SETTLE_MS = 500L

/**
 * Full-screen daily log editor presented as a horizontal pager with five tabbed pages:
 * Wellness, Period, Symptoms, Medications, and Notes/Tags.
 *
 * Manages the coach-mark walkthrough lifecycle (start, skip, completion), tutorial
 * seed data cleanup, and predictive-back gestures for pager navigation and
 * walkthrough dismissal.
 *
 * @param date The calendar date whose daily log is being viewed/edited.
 * @param onNavigateToTracker Callback invoked when the walkthrough completes and the
 *        user should be navigated to the Tracker screen for its walkthrough.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyLogScreen(
    date: LocalDate,
    onNavigateToTracker: () -> Unit = {},
) {
    val dims = LocalDimensions.current
    val koin = getKoin()
    val sessionScope = koin.getScope("session")

    val viewModel: DailyLogViewModel = koinViewModel(
        scope = sessionScope,
        parameters = { parametersOf(date) }
    )

    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()

    // Coach mark system
    val hintPreferences: HintPreferences = koin.get()
    val coachMarkState = remember { CoachMarkState(hintPreferences, coroutineScope) }

    // Tracks whether the walkthrough has been started this session, used to detect
    // completion and navigate to the Tracker screen.
    var walkthroughActive by rememberSaveable { mutableStateOf(false) }

    // Start the walkthrough (and seed demo data) when the log finishes loading.
    // Also acts as a safety net: if a seed manifest exists from a previous
    // interrupted session, clean it up before doing anything else.
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.log != null) {
            val appSettings: AppSettings = koin.get()
            val manifestJson = appSettings.seedManifestJson.first()

            // Safety net: if seed data exists from a previous session/break-out, wipe it.
            if (manifestJson.isNotEmpty()) {
                val cleanup: TutorialCleanupUseCase = sessionScope.get()
                runSeedCleanupIfNeeded(appSettings, cleanup)
                return@LaunchedEffect // data was stale; don't restart walkthrough
            }

            val seen = hintPreferences.isHintSeen(HintKey.DAILY_LOG_WELCOME).first()
            if (!seen) {
                val seeder: TutorialSeederUseCase = sessionScope.get()
                val manifest = seeder()
                if (manifest != null) {
                    // Augment manifest with today's entry (created by DailyLogViewModel.init
                    // before the seeder ran). Ensures cleanup wipes all tutorial-modified data
                    // (mood, energy, PeriodLog, water) for today.
                    val todayEntryId = uiState.log?.entry?.id
                    val augmented = if (todayEntryId != null && todayEntryId !in manifest.dailyEntryIds) {
                        manifest.copy(
                            dailyEntryIds = manifest.dailyEntryIds + todayEntryId,
                            waterIntakeDates = manifest.waterIntakeDates + date.toString(),
                        )
                    } else {
                        manifest
                    }
                    appSettings.setSeedManifestJson(augmented.toJson())
                }
                walkthroughActive = true
                DAILY_LOG_HINTS[HintKey.DAILY_LOG_WELCOME]?.let { coachMarkState.showHint(it) }
            }
        }
    }

    // Track periods created by user during the walkthrough (e.g. period toggle).
    LaunchedEffect(walkthroughActive) {
        if (!walkthroughActive) return@LaunchedEffect
        val appSettings: AppSettings = koin.get()
        val manifestJson = appSettings.seedManifestJson.first()
        if (manifestJson.isEmpty()) return@LaunchedEffect
        val initialManifest = parseSeedManifest(manifestJson) ?: return@LaunchedEffect
        val trackedIds = initialManifest.periodUuids.toMutableSet()

        val repo: PeriodRepository = sessionScope.get()
        repo.getAllPeriods().collect { periods ->
            val newIds = periods.map { it.id }.filter { it !in trackedIds }
            if (newIds.isNotEmpty()) {
                trackedIds.addAll(newIds)
                // Re-read manifest to merge with any concurrent updates
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

    val activeHint by coachMarkState.active.collectAsState()
    val pendingKey by coachMarkState.pendingHintKey.collectAsState()

    // Predictive back: step to the previous pager page before navigating out.
    // Disabled on the first page (Wellness) so the system handles back normally.
    // Declared before the walkthrough handler so the walkthrough one (last in
    // composition order) takes priority when enabled.
    BackHandler(enabled = pagerState.currentPage > 0 && activeHint == null && pendingKey == null) {
        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    // Shared cleanup for skipping the entire tutorial (both DailyLog and Tracker).
    // Called by both the BackHandler and the CoachMarkOverlay "Hold to Skip" callback.
    // Does NOT call coachMarkState.skipAll() — callers handle that themselves.
    val skipEntireTutorial: () -> Unit = {
        walkthroughActive = false
        coroutineScope.launch {
            // Skip the Tracker walkthrough too — user opted out of the whole tutorial.
            TRACKER_HINTS.keys.forEach { hintPreferences.markHintSeen(it) }
            // Wipe seed data.
            val cleanup: TutorialCleanupUseCase = sessionScope.get()
            val appSettings: AppSettings = koin.get()
            runSeedCleanupIfNeeded(appSettings, cleanup)
        }
    }

    // Predictive back: dismiss the coach mark walkthrough instead of navigating away.
    // When no walkthrough is active, the handler is disabled and back navigates normally.
    // Declared last so it takes priority over the pager handler when a walkthrough is active.
    // Runs cleanup immediately rather than relying on the completion LaunchedEffect
    // (which misses the skip because walkthroughActive goes false first).
    BackHandler(enabled = activeHint != null || pendingKey != null) {
        coachMarkState.skipAll(DAILY_LOG_HINTS)
        skipEntireTutorial()
    }

    // Detect walkthrough completion: walkthroughActive is the reliable signal.
    // When the user advances past the last hint, activeHint/pendingKey both become
    // null while walkthroughActive is still true → the Daily Log walkthrough completed
    // normally. (BackHandler sets walkthroughActive = false first, so this condition
    // cannot fire on a skip.) Navigate to the Tracker walkthrough if it hasn't been
    // seen yet; otherwise clean up seed data here so the bottom nav re-enables.
    // All suspend work after setting walkthroughActive = false is launched in
    // coroutineScope because the state write triggers recomposition, which restarts
    // this LaunchedEffect and cancels any in-flight suspend work.
    LaunchedEffect(activeHint, pendingKey, walkthroughActive) {
        if (walkthroughActive && activeHint == null && pendingKey == null) {
            walkthroughActive = false
            coroutineScope.launch {
                val trackerSeen = hintPreferences.isHintSeen(HintKey.TRACKER_WELCOME).first()
                if (!trackerSeen) {
                    onNavigateToTracker()
                } else {
                    // Tracker walkthrough already completed — clean up seed data directly.
                    val cleanup: TutorialCleanupUseCase = sessionScope.get()
                    val appSettings: AppSettings = koin.get()
                    runSeedCleanupIfNeeded(appSettings, cleanup)
                }
            }
        }
    }

    // When a pending hint targets a page that is not currently visible, auto-scroll
    // the pager so the target composable becomes visible. Hold activation during
    // the scroll so the overlay only appears once the page has fully settled.
    LaunchedEffect(pendingKey) {
        val targetPage = when (pendingKey) {
            HintKey.DAILY_LOG_PERIOD_TAB, HintKey.DAILY_LOG_PERIOD_TOGGLE -> PAGE_PERIOD
            HintKey.DAILY_LOG_SYMPTOMS_TAB -> PAGE_SYMPTOMS
            HintKey.DAILY_LOG_MEDICATIONS_TAB -> PAGE_MEDICATIONS
            HintKey.DAILY_LOG_NOTES_TAB -> PAGE_NOTES
            else -> null
        }
        if (targetPage != null && pagerState.currentPage != targetPage) {
            coachMarkState.hold()
            try {
                pagerState.animateScrollToPage(targetPage)
                delay(TAB_ROW_SETTLE_MS)
            } finally {
                coachMarkState.release()
            }
        }
    }

    val pageLabels = listOf(
        stringResource(R.string.daily_log_page_wellness),
        stringResource(R.string.daily_log_page_period),
        stringResource(R.string.daily_log_page_symptoms),
        stringResource(R.string.daily_log_page_medications),
        stringResource(R.string.daily_log_page_notes),
    )

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DailyLogSkeletonLoader()
            }
        }
        // Generic error fallback. The "no parent cycle" path no longer reaches here
        // since GetOrCreateDailyLogUseCase now always returns a FullDailyLog.
        uiState.error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
        }
        uiState.log != null -> {
            val log = uiState.log!!
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(uiState.errorMessage) {
                val message = uiState.errorMessage ?: return@LaunchedEffect
                snackbarHostState.showSnackbar(message)
                viewModel.onEvent(DailyLogEvent.ErrorDismissed)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                ContentContainer {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    // Header
                    Text(
                        text = stringResource(R.string.daily_log_for, log.entry.entryDate.toLocalizedDateString()),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .padding(horizontal = dims.md, vertical = dims.md)
                            .coachMarkTarget(HintKey.DAILY_LOG_WELCOME, coachMarkState)
                    )

                    // Page indicator tabs
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        edgePadding = dims.md,
                        modifier = Modifier
                            .fillMaxWidth()
                            .coachMarkTarget(HintKey.DAILY_LOG_EXPLORE_TABS, coachMarkState),
                    ) {
                        pageLabels.forEachIndexed { index, label ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    val tutorialActive = activeHint != null || pendingKey != null
                                    when {
                                        // No tutorial running — allow normal navigation.
                                        !tutorialActive -> {
                                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                        }
                                        // Task tab steps: scroll to the target page, then advance.
                                        activeHint?.def?.key == HintKey.DAILY_LOG_PERIOD_TAB
                                            && index == PAGE_PERIOD -> {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                                coachMarkState.advanceOrDismiss(DAILY_LOG_HINTS)
                                            }
                                        }
                                        activeHint?.def?.key == HintKey.DAILY_LOG_SYMPTOMS_TAB
                                            && index == PAGE_SYMPTOMS -> {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                                coachMarkState.advanceOrDismiss(DAILY_LOG_HINTS)
                                            }
                                        }
                                        activeHint?.def?.key == HintKey.DAILY_LOG_MEDICATIONS_TAB
                                            && index == PAGE_MEDICATIONS -> {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                                coachMarkState.advanceOrDismiss(DAILY_LOG_HINTS)
                                            }
                                        }
                                        // All other clicks during tutorial — blocked (no-op).
                                    }
                                },
                                text = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                                modifier = when (index) {
                                    PAGE_PERIOD -> Modifier.coachMarkTarget(HintKey.DAILY_LOG_PERIOD_TAB, coachMarkState)
                                    PAGE_SYMPTOMS -> Modifier.coachMarkTarget(
                                        HintKey.DAILY_LOG_SYMPTOMS_TAB, coachMarkState,
                                        enabled = pagerState.currentPage == PAGE_SYMPTOMS,
                                    )
                                    PAGE_MEDICATIONS -> Modifier.coachMarkTarget(
                                        HintKey.DAILY_LOG_MEDICATIONS_TAB, coachMarkState,
                                        enabled = pagerState.currentPage == PAGE_MEDICATIONS,
                                    )
                                    PAGE_NOTES -> Modifier.coachMarkTarget(
                                        HintKey.DAILY_LOG_NOTES_TAB, coachMarkState,
                                        enabled = pagerState.currentPage == PAGE_NOTES,
                                    )
                                    else -> Modifier
                                },
                            )
                        }
                    }

                    // Pager — disable swiping while the coach mark walkthrough is active.
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = activeHint == null && pendingKey == null,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("daily_log_pager"),
                    ) { page ->
                        when (page) {
                            PAGE_WELLNESS -> WellnessPage(
                                moodScore = log.entry.moodScore,
                                energyLevel = log.entry.energyLevel,
                                libidoScore = log.entry.libidoScore,
                                waterCups = uiState.waterCups,
                                onMoodChanged = { score ->
                                    viewModel.onEvent(DailyLogEvent.MoodScoreChanged(score))
                                    if (activeHint?.def?.key == HintKey.DAILY_LOG_MOOD) {
                                        coachMarkState.advanceOrDismiss(DAILY_LOG_HINTS)
                                    }
                                },
                                onEnergyChanged = { score ->
                                    viewModel.onEvent(DailyLogEvent.EnergyLevelChanged(score))
                                    if (activeHint?.def?.key == HintKey.DAILY_LOG_ENERGY) {
                                        coachMarkState.advanceOrDismiss(DAILY_LOG_HINTS)
                                    }
                                },
                                onLibidoChanged = { viewModel.onEvent(DailyLogEvent.LibidoScoreChanged(it)) },
                                onWaterIncrement = {
                                    viewModel.onEvent(DailyLogEvent.WaterIncrement)
                                    if (activeHint?.def?.key == HintKey.DAILY_LOG_WATER) {
                                        coachMarkState.advanceOrDismiss(DAILY_LOG_HINTS)
                                    }
                                },
                                onWaterDecrement = { viewModel.onEvent(DailyLogEvent.WaterDecrement) },
                                onShowEducationalSheet = { tag -> viewModel.onEvent(DailyLogEvent.ShowEducationalSheet(tag)) },
                                coachMarkState = coachMarkState,
                                activeHintKey = activeHint?.def?.key,
                            )
                            PAGE_PERIOD -> PeriodPage(
                                isPeriodDay = uiState.isPeriodDay,
                                flowIntensity = log.periodLog?.flowIntensity,
                                periodColor = log.periodLog?.periodColor,
                                periodConsistency = log.periodLog?.periodConsistency,
                                onPeriodToggled = { toggled ->
                                    viewModel.onEvent(DailyLogEvent.PeriodToggled(toggled))
                                    if (activeHint?.def?.key == HintKey.DAILY_LOG_PERIOD_TOGGLE) {
                                        coachMarkState.advanceOrDismiss(DAILY_LOG_HINTS)
                                    }
                                },
                                onFlowChanged = { viewModel.onEvent(DailyLogEvent.FlowIntensityChanged(it)) },
                                onColorChanged = { viewModel.onEvent(DailyLogEvent.PeriodColorChanged(it)) },
                                onConsistencyChanged = { viewModel.onEvent(DailyLogEvent.PeriodConsistencyChanged(it)) },
                                onShowEducationalSheet = { tag -> viewModel.onEvent(DailyLogEvent.ShowEducationalSheet(tag)) },
                                coachMarkState = coachMarkState,
                                activeHintKey = activeHint?.def?.key,
                            )
                            PAGE_SYMPTOMS -> SymptomsPage(
                                loggedSymptoms = log.symptomLogs,
                                symptomLibrary = uiState.symptomLibrary,
                                onToggleSymptom = { viewModel.onEvent(DailyLogEvent.SymptomToggled(it)) },
                                onCreateAndAddSymptom = { viewModel.onEvent(DailyLogEvent.CreateAndAddSymptom(it)) },
                                onShowEducationalSheet = { tag -> viewModel.onEvent(DailyLogEvent.ShowEducationalSheet(tag)) },
                            )
                            PAGE_MEDICATIONS -> MedicationsPage(
                                loggedMedications = log.medicationLogs,
                                medicationLibrary = uiState.medicationLibrary,
                                onToggleMedication = { viewModel.onEvent(DailyLogEvent.MedicationToggled(it)) },
                                onCreateAndAddMedication = { viewModel.onEvent(DailyLogEvent.MedicationCreatedAndAdded(it)) },
                                onShowEducationalSheet = { tag -> viewModel.onEvent(DailyLogEvent.ShowEducationalSheet(tag)) },
                            )
                            PAGE_NOTES -> NotesTagsPage(
                                tags = log.entry.customTags,
                                note = log.entry.note ?: "",
                                onAddTag = { viewModel.onEvent(DailyLogEvent.TagAdded(it)) },
                                onRemoveTag = { viewModel.onEvent(DailyLogEvent.TagRemoved(it)) },
                                onNoteChanged = { viewModel.onEvent(DailyLogEvent.NoteChanged(it)) },
                            )
                        }
                    }

                    uiState.educationalArticles?.let { articles ->
                        EducationalBottomSheet(
                            articles = articles,
                            onDismiss = { viewModel.onEvent(DailyLogEvent.DismissEducationalSheet) },
                        )
                    }
                }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                // Coach mark overlay draws on top of all screen content.
                CoachMarkOverlay(
                    state = coachMarkState,
                    allDefs = DAILY_LOG_HINTS,
                    onSkipAll = skipEntireTutorial,
                )
            }
        }
    }
}
