package com.veleda.cyclewise.ui.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.veleda.cyclewise.domain.models.ArticleCategory
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.domain.providers.EducationalContentProvider
import com.veleda.cyclewise.domain.usecases.DeleteAllDataUseCase
import com.veleda.cyclewise.reminders.ReminderScheduler
import com.veleda.cyclewise.session.ChangePassphraseResult
import com.veleda.cyclewise.session.SessionManager
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.coachmark.HintPreferences
import com.veleda.cyclewise.ui.theme.ThemeMode
import com.veleda.cyclewise.ui.tracker.CyclePhaseColors
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SettingsViewModel] following the Given-When-Then convention.
 *
 * Mocks [AppSettings] (all Flows return `flowOf(default)`), [ReminderScheduler],
 * and [SessionManager] to verify state updates and side-effect invocations in isolation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockAppSettings: AppSettings
    private lateinit var mockReminderScheduler: ReminderScheduler
    private lateinit var mockEducationalContentProvider: EducationalContentProvider
    private lateinit var mockHintPreferences: HintPreferences
    private lateinit var mockDeleteAllDataUseCase: DeleteAllDataUseCase
    private lateinit var mockSessionManager: SessionManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockAppSettings = mockk(relaxed = true)
        mockReminderScheduler = mockk(relaxed = true)
        mockEducationalContentProvider = mockk(relaxed = true)
        mockHintPreferences = mockk(relaxed = true)
        mockDeleteAllDataUseCase = mockk(relaxed = true)
        mockSessionManager = mockk(relaxed = true)

        // Configure all AppSettings flows to return defaults.
        every { mockAppSettings.themeMode } returns flowOf("system")
        every { mockAppSettings.autolockMinutes } returns flowOf(10)
        every { mockAppSettings.topSymptomsCount } returns flowOf(3)
        every { mockAppSettings.showMoodInSummary } returns flowOf(true)
        every { mockAppSettings.showEnergyInSummary } returns flowOf(true)
        every { mockAppSettings.showLibidoInSummary } returns flowOf(true)
        every { mockAppSettings.showFollicularPhase } returns flowOf(true)
        every { mockAppSettings.showOvulationPhase } returns flowOf(true)
        every { mockAppSettings.showLutealPhase } returns flowOf(true)
        every { mockAppSettings.menstruationColor } returns flowOf(CyclePhaseColors.DEFAULT_MENSTRUATION_HEX)
        every { mockAppSettings.follicularColor } returns flowOf(CyclePhaseColors.DEFAULT_FOLLICULAR_HEX)
        every { mockAppSettings.ovulationColor } returns flowOf(CyclePhaseColors.DEFAULT_OVULATION_HEX)
        every { mockAppSettings.lutealColor } returns flowOf(CyclePhaseColors.DEFAULT_LUTEAL_HEX)
        every { mockAppSettings.reminderPeriodEnabled } returns flowOf(false)
        every { mockAppSettings.reminderPeriodDaysBefore } returns flowOf(2)
        every { mockAppSettings.reminderPeriodPrivacyAccepted } returns flowOf(false)
        every { mockAppSettings.reminderMedicationEnabled } returns flowOf(false)
        every { mockAppSettings.reminderMedicationHour } returns flowOf(9)
        every { mockAppSettings.reminderMedicationMinute } returns flowOf(0)
        every { mockAppSettings.reminderHydrationEnabled } returns flowOf(false)
        every { mockAppSettings.reminderHydrationGoalCups } returns flowOf(8)
        every { mockAppSettings.reminderHydrationFrequencyHours } returns flowOf(3)
        every { mockAppSettings.reminderHydrationStartHour } returns flowOf(8)
        every { mockAppSettings.reminderHydrationEndHour } returns flowOf(20)

        // Stub android.util.Log so Log.e() returns 0 instead of throwing
        // "Method e in android.util.Log not mocked" in non-Robolectric tests.
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            appSettings = mockAppSettings,
            reminderScheduler = mockReminderScheduler,
            educationalContentProvider = mockEducationalContentProvider,
            hintPreferences = mockHintPreferences,
            deleteAllDataUseCase = mockDeleteAllDataUseCase,
            sessionManager = mockSessionManager,
        )
    }

    // ── Init — General state defaults ────────────────────────────────

    @Test
    fun `init WHEN created THEN generalStateHasDefaultValues`() = runTest {
        // GIVEN — default AppSettings flows
        // WHEN — ViewModel is created
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN — general state matches defaults
        val state = viewModel.generalState.value
        assertEquals(10, state.autolockMinutes)
        assertEquals(3, state.topSymptomsCount)
        assertFalse(state.showHintResetConfirmation)
        assertFalse(state.showPrivacyPolicyDialog)
        assertFalse(state.showTermsOfServiceDialog)
        assertFalse(state.showChangePassphraseDialog)
        assertNull(state.changePassphraseError)
        assertFalse(state.isChangingPassphrase)
        assertFalse(state.showPassphraseSuccessDialog)
        assertFalse(state.showDeleteFirstConfirmation)
        assertFalse(state.showDeleteSecondConfirmation)
        assertEquals("", state.deleteConfirmText)
        assertFalse(state.isDeletingData)
    }

    // ── Init — Appearance state defaults ─────────────────────────────

    @Test
    fun `init WHEN created THEN appearanceStateHasDefaultValues`() = runTest {
        // GIVEN — default AppSettings flows
        // WHEN — ViewModel is created
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN — appearance state matches defaults
        val state = viewModel.appearanceState.value
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertTrue(state.showMood)
        assertTrue(state.showEnergy)
        assertTrue(state.showLibido)
        assertTrue(state.showFollicular)
        assertTrue(state.showOvulation)
        assertTrue(state.showLuteal)
        assertEquals(CyclePhaseColors.DEFAULT_MENSTRUATION_HEX, state.menstruationColorHex)
        assertEquals(CyclePhaseColors.DEFAULT_FOLLICULAR_HEX, state.follicularColorHex)
        assertEquals(CyclePhaseColors.DEFAULT_OVULATION_HEX, state.ovulationColorHex)
        assertEquals(CyclePhaseColors.DEFAULT_LUTEAL_HEX, state.lutealColorHex)
        assertNull(state.educationalArticles)
    }

    // ── Init — Notification state defaults ───────────────────────────

    @Test
    fun `init WHEN created THEN notificationStateHasDefaultValues`() = runTest {
        // GIVEN — default AppSettings flows
        // WHEN — ViewModel is created
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN — notification state matches defaults
        val state = viewModel.notificationState.value
        assertFalse(state.periodReminderEnabled)
        assertEquals(2, state.periodDaysBefore)
        assertFalse(state.periodPrivacyAccepted)
        assertFalse(state.medicationReminderEnabled)
        assertEquals(9, state.medicationHour)
        assertEquals(0, state.medicationMinute)
        assertFalse(state.hydrationReminderEnabled)
        assertEquals(8, state.hydrationGoalCups)
        assertEquals(3, state.hydrationFrequencyHours)
        assertEquals(8, state.hydrationStartHour)
        assertEquals(20, state.hydrationEndHour)
        assertFalse(state.showPermissionRationale)
        assertFalse(state.showPrivacyDialog)
    }

    // ── Init — About state defaults ──────────────────────────────────

    @Test
    fun `init WHEN created THEN aboutStateHasDefaultValues`() = runTest {
        // GIVEN — default AppSettings flows
        // WHEN — ViewModel is created
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN — about state matches defaults
        assertFalse(viewModel.aboutState.value.showAboutDialog)
    }

    // ── AutolockChanged ──────────────────────────────────────────────

    @Test
    fun `onEvent AutolockChanged WHEN dispatched THEN updatesStateAndPersists`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — autolock changed to 15 minutes
        viewModel.onEvent(SettingsEvent.AutolockChanged(15))
        advanceUntilIdle()

        // THEN — state updated and persistence called
        assertEquals(15, viewModel.generalState.value.autolockMinutes)
        coVerify(atLeast = 1) { mockAppSettings.setAutolockMinutes(15) }
    }

    // ── TopSymptomsCountChanged ──────────────────────────────────────

    @Test
    fun `onEvent TopSymptomsCountChanged WHEN dispatched THEN updatesStateAndPersists`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — top symptoms count changed to 5
        viewModel.onEvent(SettingsEvent.TopSymptomsCountChanged(5))
        advanceUntilIdle()

        // THEN — state updated and persistence called
        assertEquals(5, viewModel.generalState.value.topSymptomsCount)
        coVerify(atLeast = 1) { mockAppSettings.setTopSymptomsCount(5) }
    }

    // ── Display toggles ──────────────────────────────────────────────

    @Test
    fun `onEvent ShowMoodToggled WHEN false THEN updatesStateAndPersists`() = runTest {
        // GIVEN — ViewModel with defaults (showMood = true)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — mood toggled to false
        viewModel.onEvent(SettingsEvent.ShowMoodToggled(false))
        advanceUntilIdle()

        // THEN — state updated and persistence called
        assertFalse(viewModel.appearanceState.value.showMood)
        coVerify(atLeast = 1) { mockAppSettings.setShowMoodInSummary(false) }
    }

    @Test
    fun `onEvent ShowEnergyToggled WHEN false THEN updatesStateAndPersists`() = runTest {
        // GIVEN — ViewModel with defaults (showEnergy = true)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — energy toggled to false
        viewModel.onEvent(SettingsEvent.ShowEnergyToggled(false))
        advanceUntilIdle()

        // THEN — state updated and persistence called
        assertFalse(viewModel.appearanceState.value.showEnergy)
        coVerify(atLeast = 1) { mockAppSettings.setShowEnergyInSummary(false) }
    }

    @Test
    fun `onEvent ShowLibidoToggled WHEN false THEN updatesStateAndPersists`() = runTest {
        // GIVEN — ViewModel with defaults (showLibido = true)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — libido toggled to false
        viewModel.onEvent(SettingsEvent.ShowLibidoToggled(false))
        advanceUntilIdle()

        // THEN — state updated and persistence called
        assertFalse(viewModel.appearanceState.value.showLibido)
        coVerify(atLeast = 1) { mockAppSettings.setShowLibidoInSummary(false) }
    }

    // ── Phase visibility toggles ─────────────────────────────────────

    @Test
    fun `onEvent ShowFollicularToggled WHEN false THEN updatesStateAndPersists`() = runTest {
        // GIVEN — ViewModel with defaults (showFollicular = true)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — follicular toggled to false
        viewModel.onEvent(SettingsEvent.ShowFollicularToggled(false))
        advanceUntilIdle()

        // THEN — state updated and persistence called
        assertFalse(viewModel.appearanceState.value.showFollicular)
        coVerify(atLeast = 1) { mockAppSettings.setShowFollicularPhase(false) }
    }

    @Test
    fun `onEvent ShowOvulationToggled WHEN false THEN updatesStateAndPersists`() = runTest {
        // GIVEN — ViewModel with defaults (showOvulation = true)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — ovulation toggled to false
        viewModel.onEvent(SettingsEvent.ShowOvulationToggled(false))
        advanceUntilIdle()

        // THEN — state updated and persistence called
        assertFalse(viewModel.appearanceState.value.showOvulation)
        coVerify(atLeast = 1) { mockAppSettings.setShowOvulationPhase(false) }
    }

    @Test
    fun `onEvent ShowLutealToggled WHEN false THEN updatesStateAndPersists`() = runTest {
        // GIVEN — ViewModel with defaults (showLuteal = true)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — luteal toggled to false
        viewModel.onEvent(SettingsEvent.ShowLutealToggled(false))
        advanceUntilIdle()

        // THEN — state updated and persistence called
        assertFalse(viewModel.appearanceState.value.showLuteal)
        coVerify(atLeast = 1) { mockAppSettings.setShowLutealPhase(false) }
    }

    // ── Phase color customization ────────────────────────────────────

    @Test
    fun `onEvent MenstruationColorChanged WHEN newHex THEN updatesStateAndPersists`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — menstruation color changed
        viewModel.onEvent(SettingsEvent.MenstruationColorChanged("FF0000"))
        advanceUntilIdle()

        // THEN — state updated and persistence called
        assertEquals("FF0000", viewModel.appearanceState.value.menstruationColorHex)
        coVerify(atLeast = 1) { mockAppSettings.setMenstruationColor("FF0000") }
    }

    @Test
    fun `onEvent ResetPhaseColorsToDefaults WHEN dispatched THEN resetsAllFourColorsAndPersists`() = runTest {
        // GIVEN — ViewModel with custom colors
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.MenstruationColorChanged("FF0000"))
        viewModel.onEvent(SettingsEvent.FollicularColorChanged("00FF00"))
        viewModel.onEvent(SettingsEvent.OvulationColorChanged("0000FF"))
        viewModel.onEvent(SettingsEvent.LutealColorChanged("FFFFFF"))
        advanceUntilIdle()

        // WHEN — reset to defaults dispatched
        viewModel.onEvent(SettingsEvent.ResetPhaseColorsToDefaults)
        advanceUntilIdle()

        // THEN — all four colors reset to defaults and persistence called
        val state = viewModel.appearanceState.value
        assertEquals(CyclePhaseColors.DEFAULT_MENSTRUATION_HEX, state.menstruationColorHex)
        assertEquals(CyclePhaseColors.DEFAULT_FOLLICULAR_HEX, state.follicularColorHex)
        assertEquals(CyclePhaseColors.DEFAULT_OVULATION_HEX, state.ovulationColorHex)
        assertEquals(CyclePhaseColors.DEFAULT_LUTEAL_HEX, state.lutealColorHex)
        coVerify(atLeast = 1) { mockAppSettings.setMenstruationColor(CyclePhaseColors.DEFAULT_MENSTRUATION_HEX) }
        coVerify(atLeast = 1) { mockAppSettings.setFollicularColor(CyclePhaseColors.DEFAULT_FOLLICULAR_HEX) }
        coVerify(atLeast = 1) { mockAppSettings.setOvulationColor(CyclePhaseColors.DEFAULT_OVULATION_HEX) }
        coVerify(atLeast = 1) { mockAppSettings.setLutealColor(CyclePhaseColors.DEFAULT_LUTEAL_HEX) }
    }

    // ── Period prediction reminder ───────────────────────────────────

    @Test
    fun `onEvent PeriodReminderToggled WHEN enabled THEN schedulesReminderAndPersists`() = runTest {
        // GIVEN — ViewModel with defaults (period reminder disabled)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — period reminder enabled
        viewModel.onEvent(SettingsEvent.PeriodReminderToggled(true))
        advanceUntilIdle()

        // THEN — state updated, scheduler called, persistence called
        assertTrue(viewModel.notificationState.value.periodReminderEnabled)
        coVerify(atLeast = 1) { mockAppSettings.setReminderPeriodEnabled(true) }
        verify(atLeast = 1) { mockReminderScheduler.schedulePeriodPrediction(true) }
    }

    @Test
    fun `onEvent PeriodReminderToggled WHEN disabled THEN cancelsReminderAndPersists`() = runTest {
        // GIVEN — ViewModel with period reminder enabled
        every { mockAppSettings.reminderPeriodEnabled } returns flowOf(true)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — period reminder disabled
        viewModel.onEvent(SettingsEvent.PeriodReminderToggled(false))
        advanceUntilIdle()

        // THEN — state updated, scheduler cancels, persistence called
        assertFalse(viewModel.notificationState.value.periodReminderEnabled)
        coVerify(atLeast = 1) { mockAppSettings.setReminderPeriodEnabled(false) }
        verify(atLeast = 1) { mockReminderScheduler.schedulePeriodPrediction(false) }
    }

    // ── Medication reminder ──────────────────────────────────────────

    @Test
    fun `onEvent MedicationReminderToggled WHEN enabled THEN schedulesWithCurrentTime`() = runTest {
        // GIVEN — ViewModel with defaults (medication hour=9, minute=0)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — medication reminder enabled
        viewModel.onEvent(SettingsEvent.MedicationReminderToggled(true))
        advanceUntilIdle()

        // THEN — scheduler called with current time, persistence called
        assertTrue(viewModel.notificationState.value.medicationReminderEnabled)
        coVerify(atLeast = 1) { mockAppSettings.setReminderMedicationEnabled(true) }
        verify(atLeast = 1) { mockReminderScheduler.scheduleMedication(true, 9, 0) }
    }

    @Test
    fun `onEvent MedicationHourChanged WHEN dispatched THEN reschedulesWithNewHour`() = runTest {
        // GIVEN — ViewModel with medication enabled
        every { mockAppSettings.reminderMedicationEnabled } returns flowOf(true)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — medication hour changed to 14
        viewModel.onEvent(SettingsEvent.MedicationHourChanged(14))
        advanceUntilIdle()

        // THEN — state updated, scheduler rescheduled with new hour
        assertEquals(14, viewModel.notificationState.value.medicationHour)
        coVerify(atLeast = 1) { mockAppSettings.setReminderMedicationHour(14) }
        verify(atLeast = 1) { mockReminderScheduler.scheduleMedication(true, 14, 0) }
    }

    // ── Hydration reminder ───────────────────────────────────────────

    @Test
    fun `onEvent HydrationReminderToggled WHEN enabled THEN schedulesWithCurrentFrequency`() = runTest {
        // GIVEN — ViewModel with defaults (frequency = 3h)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — hydration reminder enabled
        viewModel.onEvent(SettingsEvent.HydrationReminderToggled(true))
        advanceUntilIdle()

        // THEN — scheduler called with current frequency, persistence called
        assertTrue(viewModel.notificationState.value.hydrationReminderEnabled)
        coVerify(atLeast = 1) { mockAppSettings.setReminderHydrationEnabled(true) }
        verify(atLeast = 1) { mockReminderScheduler.scheduleHydration(true, 3) }
    }

    @Test
    fun `onEvent HydrationFrequencyChanged WHEN dispatched THEN reschedulesWithNewFrequency`() = runTest {
        // GIVEN — ViewModel with hydration enabled
        every { mockAppSettings.reminderHydrationEnabled } returns flowOf(true)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — frequency changed to 2 hours
        viewModel.onEvent(SettingsEvent.HydrationFrequencyChanged(2))
        advanceUntilIdle()

        // THEN — state updated, scheduler rescheduled with new frequency
        assertEquals(2, viewModel.notificationState.value.hydrationFrequencyHours)
        coVerify(atLeast = 1) { mockAppSettings.setReminderHydrationFrequencyHours(2) }
        verify(atLeast = 1) { mockReminderScheduler.scheduleHydration(true, 2) }
    }

    // ── Dialog events ────────────────────────────────────────────────

    @Test
    fun `onEvent ShowAboutDialog THEN showAboutDialogIsTrue`() = runTest {
        // GIVEN — ViewModel with defaults (showAboutDialog = false)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — show about dialog dispatched
        viewModel.onEvent(SettingsEvent.ShowAboutDialog)

        // THEN — dialog state is true
        assertTrue(viewModel.aboutState.value.showAboutDialog)
    }

    @Test
    fun `onEvent DismissAboutDialog THEN showAboutDialogIsFalse`() = runTest {
        // GIVEN — ViewModel with about dialog open
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.ShowAboutDialog)
        assertTrue(viewModel.aboutState.value.showAboutDialog)

        // WHEN — dismiss about dialog dispatched
        viewModel.onEvent(SettingsEvent.DismissAboutDialog)

        // THEN — dialog state is false
        assertFalse(viewModel.aboutState.value.showAboutDialog)
    }

    @Test
    fun `onEvent PeriodPrivacyAccepted THEN enablesReminderAndAcceptsPrivacy`() = runTest {
        // GIVEN — ViewModel with privacy dialog shown
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.ShowPrivacyDialog)
        assertTrue(viewModel.notificationState.value.showPrivacyDialog)

        // WHEN — privacy accepted
        viewModel.onEvent(SettingsEvent.PeriodPrivacyAccepted)
        advanceUntilIdle()

        // THEN — privacy accepted, reminder enabled, dialog dismissed
        val state = viewModel.notificationState.value
        assertTrue(state.periodPrivacyAccepted)
        assertTrue(state.periodReminderEnabled)
        assertFalse(state.showPrivacyDialog)
        coVerify(atLeast = 1) { mockAppSettings.setReminderPeriodPrivacyAccepted(true) }
        coVerify(atLeast = 1) { mockAppSettings.setReminderPeriodEnabled(true) }
        verify(atLeast = 1) { mockReminderScheduler.schedulePeriodPrediction(true) }
    }

    // ── Theme mode ──────────────────────────────────────────────────

    @Test
    fun `onEvent ThemeModeChanged WHEN dark THEN updatesStateAndPersists`() = runTest {
        // GIVEN — ViewModel with defaults (themeMode = SYSTEM)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — theme mode changed to DARK
        viewModel.onEvent(SettingsEvent.ThemeModeChanged(ThemeMode.DARK))
        advanceUntilIdle()

        // THEN — state updated and persistence called
        assertEquals(ThemeMode.DARK, viewModel.appearanceState.value.themeMode)
        coVerify(atLeast = 1) { mockAppSettings.setThemeMode("dark") }
    }

    @Test
    fun `init WHEN themeModeIsLight THEN appearanceStateReflectsLight`() = runTest {
        // GIVEN — AppSettings returns "light" for themeMode
        every { mockAppSettings.themeMode } returns flowOf("light")

        // WHEN — ViewModel is created
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN — state reflects LIGHT mode
        assertEquals(ThemeMode.LIGHT, viewModel.appearanceState.value.themeMode)
    }

    // ── Educational sheet tests ────────────────────────────────────

    private val testArticle = EducationalArticle(
        id = "test-article",
        title = "Test",
        body = "Body",
        category = ArticleCategory.CYCLE_BASICS,
        contentTags = listOf("CyclePhase.Colors"),
        sourceName = "Test Source",
        sourceUrl = "https://example.com",
        sortOrder = 1,
    )

    @Test
    fun `onEvent ShowEducationalSheet WHEN tagHasArticles THEN educationalArticlesPopulated`() = runTest {
        // GIVEN — provider returns articles for the tag
        every { mockEducationalContentProvider.getByTag("CyclePhase.Colors") } returns listOf(testArticle)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — show educational sheet dispatched
        viewModel.onEvent(SettingsEvent.ShowEducationalSheet("CyclePhase.Colors"))
        advanceUntilIdle()

        // THEN — educationalArticles is populated
        assertNotNull(viewModel.appearanceState.value.educationalArticles)
        assertEquals(1, viewModel.appearanceState.value.educationalArticles!!.size)
    }

    @Test
    fun `onEvent ShowEducationalSheet WHEN tagHasNoArticles THEN educationalArticlesRemainsNull`() = runTest {
        // GIVEN — provider returns empty list for the tag
        every { mockEducationalContentProvider.getByTag("NonExistent") } returns emptyList()
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — show educational sheet dispatched
        viewModel.onEvent(SettingsEvent.ShowEducationalSheet("NonExistent"))
        advanceUntilIdle()

        // THEN — educationalArticles remains null
        assertNull(viewModel.appearanceState.value.educationalArticles)
    }

    @Test
    fun `onEvent DismissEducationalSheet WHEN shown THEN educationalArticlesNull`() = runTest {
        // GIVEN — educational sheet is showing
        every { mockEducationalContentProvider.getByTag("CyclePhase.Colors") } returns listOf(testArticle)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.ShowEducationalSheet("CyclePhase.Colors"))
        advanceUntilIdle()
        assertNotNull(viewModel.appearanceState.value.educationalArticles)

        // WHEN — dismiss educational sheet dispatched
        viewModel.onEvent(SettingsEvent.DismissEducationalSheet)
        advanceUntilIdle()

        // THEN — educationalArticles is null
        assertNull(viewModel.appearanceState.value.educationalArticles)
    }

    // ── Delete All Data ─────────────────────────────────────────────

    @Test
    fun `onEvent DeleteAllDataRequested THEN showsFirstConfirmation`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — delete all data requested
        viewModel.onEvent(SettingsEvent.DeleteAllDataRequested)

        // THEN — first confirmation dialog is shown
        assertTrue(viewModel.generalState.value.showDeleteFirstConfirmation)
        assertFalse(viewModel.generalState.value.showDeleteSecondConfirmation)
    }

    @Test
    fun `onEvent DeleteAllDataCancelled THEN resetsAllConfirmationState`() = runTest {
        // GIVEN — first confirmation dialog is showing
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.DeleteAllDataRequested)
        assertTrue(viewModel.generalState.value.showDeleteFirstConfirmation)

        // WHEN — user cancels
        viewModel.onEvent(SettingsEvent.DeleteAllDataCancelled)

        // THEN — all confirmation state is reset
        val state = viewModel.generalState.value
        assertFalse(state.showDeleteFirstConfirmation)
        assertFalse(state.showDeleteSecondConfirmation)
        assertEquals("", state.deleteConfirmText)
    }

    @Test
    fun `onEvent DeleteAllDataFirstConfirmed THEN advancesToSecondConfirmation`() = runTest {
        // GIVEN — first confirmation dialog is showing
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.DeleteAllDataRequested)

        // WHEN — user confirms first dialog
        viewModel.onEvent(SettingsEvent.DeleteAllDataFirstConfirmed)

        // THEN — advances to second confirmation
        val state = viewModel.generalState.value
        assertFalse(state.showDeleteFirstConfirmation)
        assertTrue(state.showDeleteSecondConfirmation)
        assertEquals("", state.deleteConfirmText)
    }

    @Test
    fun `onEvent DeleteConfirmTextChanged THEN updatesDeleteConfirmText`() = runTest {
        // GIVEN — second confirmation dialog is showing
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.DeleteAllDataRequested)
        viewModel.onEvent(SettingsEvent.DeleteAllDataFirstConfirmed)

        // WHEN — user types in the confirmation field
        viewModel.onEvent(SettingsEvent.DeleteConfirmTextChanged("DEL"))

        // THEN — text is updated
        assertEquals("DEL", viewModel.generalState.value.deleteConfirmText)
    }

    @Test
    fun `onEvent DeleteAllDataConfirmed THEN closesSessionAndInvokesUseCaseAndEmitsEffect`() = runTest {
        // GIVEN — second confirmation completed
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.DeleteAllDataRequested)
        viewModel.onEvent(SettingsEvent.DeleteAllDataFirstConfirmed)
        viewModel.onEvent(SettingsEvent.DeleteConfirmTextChanged("DELETE"))

        // WHEN — user confirms final deletion
        viewModel.effect.test {
            viewModel.onEvent(SettingsEvent.DeleteAllDataConfirmed)
            advanceUntilIdle()

            // THEN — use case was invoked and DataDeleted effect was emitted
            coVerify(exactly = 1) { mockDeleteAllDataUseCase() }
            assertEquals(SettingsEffect.DataDeleted, awaitItem())
        }

        // THEN — session was closed and confirmation state is reset
        verify(atLeast = 1) { mockSessionManager.closeSession() }
        val state = viewModel.generalState.value
        assertFalse(state.showDeleteSecondConfirmation)
        assertEquals("", state.deleteConfirmText)
    }

    // ── Change Passphrase ───────────────────────────────────────────

    @Test
    fun `onEvent ChangePassphraseRequested THEN showsDialog`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — change passphrase requested
        viewModel.onEvent(SettingsEvent.ChangePassphraseRequested)

        // THEN — dialog is shown
        assertTrue(viewModel.generalState.value.showChangePassphraseDialog)
    }

    @Test
    fun `onEvent ChangePassphraseDismissed THEN closesDialogAndClearsError`() = runTest {
        // GIVEN — dialog is open with an error
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.ChangePassphraseRequested)
        // Trigger a validation error first
        viewModel.onEvent(
            SettingsEvent.ChangePassphraseSubmitted(
                current = "old",
                newPassphrase = "short",
                confirmation = "short",
            )
        )
        assertNotNull(viewModel.generalState.value.changePassphraseError)

        // WHEN — user dismisses
        viewModel.onEvent(SettingsEvent.ChangePassphraseDismissed)

        // THEN — dialog is hidden and error is cleared
        val state = viewModel.generalState.value
        assertFalse(state.showChangePassphraseDialog)
        assertNull(state.changePassphraseError)
    }

    @Test
    fun `onEvent ChangePassphraseSubmitted WHEN newTooShort THEN setsErrorTooShort`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — submitted with new passphrase shorter than 8 chars
        viewModel.onEvent(
            SettingsEvent.ChangePassphraseSubmitted(
                current = "currentpass",
                newPassphrase = "short",
                confirmation = "short",
            )
        )

        // THEN — error is "too_short"
        assertEquals("too_short", viewModel.generalState.value.changePassphraseError)
        assertFalse(viewModel.generalState.value.isChangingPassphrase)
    }

    @Test
    fun `onEvent ChangePassphraseSubmitted WHEN confirmationMismatch THEN setsErrorMismatch`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — submitted with mismatching confirmation
        viewModel.onEvent(
            SettingsEvent.ChangePassphraseSubmitted(
                current = "currentpass",
                newPassphrase = "newpassphrase",
                confirmation = "differentconfirm",
            )
        )

        // THEN — error is "mismatch"
        assertEquals("mismatch", viewModel.generalState.value.changePassphraseError)
        assertFalse(viewModel.generalState.value.isChangingPassphrase)
    }

    @Test
    fun `onEvent ChangePassphraseSubmitted WHEN shortThenMismatch THEN errorChanges`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — first submit with too-short passphrase
        viewModel.onEvent(
            SettingsEvent.ChangePassphraseSubmitted(
                current = "old",
                newPassphrase = "short",
                confirmation = "short",
            )
        )

        // THEN — error is "too_short"
        assertEquals("too_short", viewModel.generalState.value.changePassphraseError)

        // WHEN — second submit with mismatching confirmation (but valid length)
        viewModel.onEvent(
            SettingsEvent.ChangePassphraseSubmitted(
                current = "currentpass",
                newPassphrase = "newpassphrase",
                confirmation = "differentconfirm",
            )
        )

        // THEN — error changes to "mismatch"
        assertEquals("mismatch", viewModel.generalState.value.changePassphraseError)
    }

    // ── Change Passphrase — IO path (via SessionManager) ─────────────

    @Test
    fun `onEvent ChangePassphraseSubmitted WHEN wrongCurrentPassphrase THEN setsWrongCurrentError`() = runTest {
        // GIVEN — SessionManager returns WrongCurrent
        coEvery {
            mockSessionManager.changePassphrase("wrong-current", "newpassphrase123")
        } returns ChangePassphraseResult.WrongCurrent

        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — submitted with wrong current passphrase
        viewModel.onEvent(
            SettingsEvent.ChangePassphraseSubmitted(
                current = "wrong-current",
                newPassphrase = "newpassphrase123",
                confirmation = "newpassphrase123",
            )
        )
        advanceUntilIdle()

        // THEN — error is "wrong_current"
        assertEquals("wrong_current", viewModel.generalState.value.changePassphraseError)
        assertFalse(viewModel.generalState.value.isChangingPassphrase)
    }

    @Test
    fun `onEvent ChangePassphraseSubmitted WHEN correctCurrentPassphrase THEN showsSuccessDialog`() = runTest {
        // GIVEN — SessionManager returns Success
        coEvery {
            mockSessionManager.changePassphrase("correct-current", "newpassphrase123")
        } returns ChangePassphraseResult.Success

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Open the dialog first so showChangePassphraseDialog is true
        viewModel.onEvent(SettingsEvent.ChangePassphraseRequested)

        // WHEN — submitted with correct current passphrase
        viewModel.onEvent(
            SettingsEvent.ChangePassphraseSubmitted(
                current = "correct-current",
                newPassphrase = "newpassphrase123",
                confirmation = "newpassphrase123",
            )
        )
        advanceUntilIdle()

        // THEN — success dialog shown, change dialog stays open, no error, not loading
        val state = viewModel.generalState.value
        assertTrue(state.showPassphraseSuccessDialog)
        assertTrue(state.showChangePassphraseDialog)
        assertNull(state.changePassphraseError)
        assertFalse(state.isChangingPassphrase)
    }

    @Test
    fun `onEvent ChangePassphraseSuccessAcknowledged THEN closesSessionAndEmitsEffect`() = runTest {
        // GIVEN — a successful passphrase change (success dialog showing)
        coEvery {
            mockSessionManager.changePassphrase("correct-current", "newpassphrase123")
        } returns ChangePassphraseResult.Success

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.ChangePassphraseRequested)
        viewModel.onEvent(
            SettingsEvent.ChangePassphraseSubmitted(
                current = "correct-current",
                newPassphrase = "newpassphrase123",
                confirmation = "newpassphrase123",
            )
        )
        advanceUntilIdle()
        assertTrue(viewModel.generalState.value.showPassphraseSuccessDialog)

        // WHEN — user acknowledges the success dialog
        viewModel.effect.test {
            viewModel.onEvent(SettingsEvent.ChangePassphraseSuccessAcknowledged)
            advanceUntilIdle()

            // THEN — PassphraseChanged effect is emitted
            assertEquals(SettingsEffect.PassphraseChanged, awaitItem())
        }

        // THEN — both dialogs are closed
        val state = viewModel.generalState.value
        assertFalse(state.showChangePassphraseDialog)
        assertFalse(state.showPassphraseSuccessDialog)

        // THEN — session was closed (Room DB is stale after rekey)
        verify(atLeast = 1) { mockSessionManager.closeSession() }
    }

    @Test
    fun `onEvent ChangePassphraseSubmitted WHEN rekeyVerificationFails THEN setsVerificationFailedError`() = runTest {
        // GIVEN — SessionManager returns VerificationFailed
        coEvery {
            mockSessionManager.changePassphrase("correct-current", "newpassphrase123")
        } returns ChangePassphraseResult.VerificationFailed

        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — submitted with correct current passphrase but rekey verification fails
        viewModel.onEvent(
            SettingsEvent.ChangePassphraseSubmitted(
                current = "correct-current",
                newPassphrase = "newpassphrase123",
                confirmation = "newpassphrase123",
            )
        )
        advanceUntilIdle()

        // THEN — error is "verification_failed"
        assertEquals("verification_failed", viewModel.generalState.value.changePassphraseError)
        assertFalse(viewModel.generalState.value.isChangingPassphrase)
        assertFalse(viewModel.generalState.value.showPassphraseSuccessDialog)
    }

    @Test
    fun `onEvent ChangePassphraseSubmitted WHEN genericExceptionThrown THEN setsFailedError`() = runTest {
        // GIVEN — SessionManager returns Failed
        coEvery {
            mockSessionManager.changePassphrase("correct-current", "newpassphrase123")
        } returns ChangePassphraseResult.Failed

        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — submitted with correct current passphrase but change fails
        viewModel.onEvent(
            SettingsEvent.ChangePassphraseSubmitted(
                current = "correct-current",
                newPassphrase = "newpassphrase123",
                confirmation = "newpassphrase123",
            )
        )
        advanceUntilIdle()

        // THEN — error is "failed"
        assertEquals("failed", viewModel.generalState.value.changePassphraseError)
        assertFalse(viewModel.generalState.value.isChangingPassphrase)
        assertFalse(viewModel.generalState.value.showPassphraseSuccessDialog)
    }

    // ── Reduce-specific tests ──────────────────────────────────────────

    @Test
    fun `reduceGeneral AutolockChanged THEN updatesAutolockMinutes`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — autolock changed
        viewModel.onEvent(SettingsEvent.AutolockChanged(5))

        // THEN — state updated synchronously by reduce
        assertEquals(5, viewModel.generalState.value.autolockMinutes)
    }

    @Test
    fun `reduceGeneral ChangePassphraseSubmitted WHEN tooShort THEN setsErrorWithoutChanging`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — submitted with short passphrase
        viewModel.onEvent(
            SettingsEvent.ChangePassphraseSubmitted("old", "short", "short")
        )

        // THEN — reduce sets error, isChangingPassphrase stays false
        assertEquals("too_short", viewModel.generalState.value.changePassphraseError)
        assertFalse(viewModel.generalState.value.isChangingPassphrase)
    }

    @Test
    fun `reduceGeneral ChangePassphraseSubmitted WHEN mismatch THEN setsErrorWithoutChanging`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — submitted with mismatched confirmation
        viewModel.onEvent(
            SettingsEvent.ChangePassphraseSubmitted("old", "newpassphrase", "different")
        )

        // THEN — reduce sets error, isChangingPassphrase stays false
        assertEquals("mismatch", viewModel.generalState.value.changePassphraseError)
        assertFalse(viewModel.generalState.value.isChangingPassphrase)
    }

    @Test
    fun `reduceGeneral DeleteAllDataFirstConfirmed THEN transitionsToSecondConfirmation`() = runTest {
        // GIVEN — first confirmation showing
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.DeleteAllDataRequested)

        // WHEN — first dialog confirmed
        viewModel.onEvent(SettingsEvent.DeleteAllDataFirstConfirmed)

        // THEN — reduce transitions dialog state
        val state = viewModel.generalState.value
        assertFalse(state.showDeleteFirstConfirmation)
        assertTrue(state.showDeleteSecondConfirmation)
    }

    @Test
    fun `reduceAppearance ThemeModeChanged THEN updatesThemeMode`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — theme changed
        viewModel.onEvent(SettingsEvent.ThemeModeChanged(ThemeMode.DARK))

        // THEN — state updated synchronously by reduce
        assertEquals(ThemeMode.DARK, viewModel.appearanceState.value.themeMode)
    }

    @Test
    fun `reduceAppearance ResetPhaseColorsToDefaults THEN resetsAllColors`() = runTest {
        // GIVEN — ViewModel with custom colors
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.MenstruationColorChanged("FF0000"))
        viewModel.onEvent(SettingsEvent.FollicularColorChanged("00FF00"))

        // WHEN — reset to defaults
        viewModel.onEvent(SettingsEvent.ResetPhaseColorsToDefaults)

        // THEN — reduce resets all colors synchronously
        val state = viewModel.appearanceState.value
        assertEquals(CyclePhaseColors.DEFAULT_MENSTRUATION_HEX, state.menstruationColorHex)
        assertEquals(CyclePhaseColors.DEFAULT_FOLLICULAR_HEX, state.follicularColorHex)
    }

    @Test
    fun `reduceAppearance DismissEducationalSheet THEN clearsArticles`() = runTest {
        // GIVEN — educational sheet is showing
        every { mockEducationalContentProvider.getByTag("CyclePhase.Colors") } returns listOf(testArticle)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.ShowEducationalSheet("CyclePhase.Colors"))
        assertNotNull(viewModel.appearanceState.value.educationalArticles)

        // WHEN — dismissed
        viewModel.onEvent(SettingsEvent.DismissEducationalSheet)

        // THEN — reduce clears articles synchronously
        assertNull(viewModel.appearanceState.value.educationalArticles)
    }

    @Test
    fun `reduceNotification PeriodPrivacyAccepted THEN setsMultipleFields`() = runTest {
        // GIVEN — ViewModel with privacy dialog showing
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.ShowPrivacyDialog)

        // WHEN — privacy accepted
        viewModel.onEvent(SettingsEvent.PeriodPrivacyAccepted)

        // THEN — reduce sets all fields synchronously
        val state = viewModel.notificationState.value
        assertTrue(state.periodPrivacyAccepted)
        assertTrue(state.periodReminderEnabled)
        assertFalse(state.showPrivacyDialog)
    }

    @Test
    fun `reduceNotification HydrationFrequencyChanged THEN updatesFrequency`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — frequency changed
        viewModel.onEvent(SettingsEvent.HydrationFrequencyChanged(4))

        // THEN — state updated synchronously by reduce
        assertEquals(4, viewModel.notificationState.value.hydrationFrequencyHours)
    }

    @Test
    fun `reduceAbout ShowAboutDialog THEN setsDialogVisible`() = runTest {
        // GIVEN — ViewModel with defaults
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN — show about dialog
        viewModel.onEvent(SettingsEvent.ShowAboutDialog)

        // THEN — reduce sets dialog visible
        assertTrue(viewModel.aboutState.value.showAboutDialog)
    }

    @Test
    fun `reduceAbout DismissAboutDialog THEN hidesDialog`() = runTest {
        // GIVEN — about dialog visible
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.ShowAboutDialog)

        // WHEN — dismiss about dialog
        viewModel.onEvent(SettingsEvent.DismissAboutDialog)

        // THEN — reduce hides dialog
        assertFalse(viewModel.aboutState.value.showAboutDialog)
    }
}
