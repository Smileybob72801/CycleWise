package com.veleda.cyclewise.ui.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.veleda.cyclewise.domain.models.ArticleCategory
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.domain.providers.EducationalContentProvider
import com.veleda.cyclewise.reminders.ReminderScheduler
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.coachmark.HintPreferences
import com.veleda.cyclewise.ui.theme.ThemeMode
import com.veleda.cyclewise.ui.tracker.CyclePhaseColors
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
 * Mocks [AppSettings] (all Flows return `flowOf(default)`) and [ReminderScheduler]
 * to verify state updates and side-effect invocations in isolation.
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockAppSettings = mockk(relaxed = true)
        mockReminderScheduler = mockk(relaxed = true)
        mockEducationalContentProvider = mockk(relaxed = true)
        mockHintPreferences = mockk(relaxed = true)

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            appSettings = mockAppSettings,
            reminderScheduler = mockReminderScheduler,
            educationalContentProvider = mockEducationalContentProvider,
            hintPreferences = mockHintPreferences,
        )
    }

    // ── Init ─────────────────────────────────────────────────────────

    @Test
    fun `init WHEN created THEN uiStateHasDefaultValues`() = runTest {
        // GIVEN — default AppSettings flows
        // WHEN — ViewModel is created
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN — state matches defaults
        val state = viewModel.uiState.value
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(10, state.autolockMinutes)
        assertEquals(3, state.topSymptomsCount)
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
        assertFalse(state.showAboutDialog)
        assertFalse(state.showPrivacyDialog)
        assertFalse(state.showPermissionRationale)
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
        assertEquals(15, viewModel.uiState.value.autolockMinutes)
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
        assertEquals(5, viewModel.uiState.value.topSymptomsCount)
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
        assertFalse(viewModel.uiState.value.showMood)
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
        assertFalse(viewModel.uiState.value.showEnergy)
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
        assertFalse(viewModel.uiState.value.showLibido)
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
        assertFalse(viewModel.uiState.value.showFollicular)
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
        assertFalse(viewModel.uiState.value.showOvulation)
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
        assertFalse(viewModel.uiState.value.showLuteal)
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
        assertEquals("FF0000", viewModel.uiState.value.menstruationColorHex)
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
        val state = viewModel.uiState.value
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
        assertTrue(viewModel.uiState.value.periodReminderEnabled)
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
        assertFalse(viewModel.uiState.value.periodReminderEnabled)
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
        assertTrue(viewModel.uiState.value.medicationReminderEnabled)
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
        assertEquals(14, viewModel.uiState.value.medicationHour)
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
        assertTrue(viewModel.uiState.value.hydrationReminderEnabled)
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
        assertEquals(2, viewModel.uiState.value.hydrationFrequencyHours)
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
        assertTrue(viewModel.uiState.value.showAboutDialog)
    }

    @Test
    fun `onEvent DismissAboutDialog THEN showAboutDialogIsFalse`() = runTest {
        // GIVEN — ViewModel with about dialog open
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.ShowAboutDialog)
        assertTrue(viewModel.uiState.value.showAboutDialog)

        // WHEN — dismiss about dialog dispatched
        viewModel.onEvent(SettingsEvent.DismissAboutDialog)

        // THEN — dialog state is false
        assertFalse(viewModel.uiState.value.showAboutDialog)
    }

    @Test
    fun `onEvent PeriodPrivacyAccepted THEN enablesReminderAndAcceptsPrivacy`() = runTest {
        // GIVEN — ViewModel with privacy dialog shown
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.ShowPrivacyDialog)
        assertTrue(viewModel.uiState.value.showPrivacyDialog)

        // WHEN — privacy accepted
        viewModel.onEvent(SettingsEvent.PeriodPrivacyAccepted)
        advanceUntilIdle()

        // THEN — privacy accepted, reminder enabled, dialog dismissed
        val state = viewModel.uiState.value
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
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        coVerify(atLeast = 1) { mockAppSettings.setThemeMode("dark") }
    }

    @Test
    fun `init WHEN themeModeIsLight THEN uiStateReflectsLight`() = runTest {
        // GIVEN — AppSettings returns "light" for themeMode
        every { mockAppSettings.themeMode } returns flowOf("light")

        // WHEN — ViewModel is created
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN — state reflects LIGHT mode
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
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
        assertNotNull(viewModel.uiState.value.educationalArticles)
        assertEquals(1, viewModel.uiState.value.educationalArticles!!.size)
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
        assertNull(viewModel.uiState.value.educationalArticles)
    }

    @Test
    fun `onEvent DismissEducationalSheet WHEN shown THEN educationalArticlesNull`() = runTest {
        // GIVEN — educational sheet is showing
        every { mockEducationalContentProvider.getByTag("CyclePhase.Colors") } returns listOf(testArticle)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.ShowEducationalSheet("CyclePhase.Colors"))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.educationalArticles)

        // WHEN — dismiss educational sheet dispatched
        viewModel.onEvent(SettingsEvent.DismissEducationalSheet)
        advanceUntilIdle()

        // THEN — educationalArticles is null
        assertNull(viewModel.uiState.value.educationalArticles)
    }
}
