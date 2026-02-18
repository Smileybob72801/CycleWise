package com.veleda.cyclewise.ui.theme

import androidx.compose.ui.graphics.Color
import com.veleda.cyclewise.domain.models.CyclePhase
import com.veleda.cyclewise.ui.tracker.CyclePhaseColors
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Unit tests for [CyclePhasePalette], [buildCyclePhasePalette], and the
 * [CyclePhasePalette.forPhase] helper.
 */
class CyclePhasePaletteTest {

    // --- buildCyclePhasePalette defaults ---

    @Test
    fun buildCyclePhasePalette_WHEN_lightThemeNoCustomColors_THEN_usesDefaults() {
        // GIVEN light mode with no custom colors
        // WHEN building the palette
        val palette = buildCyclePhasePalette(darkTheme = false, customColors = null)

        // THEN default fill colors match CyclePhaseColors constants
        assertEquals(CyclePhaseColors.Menstruation, palette.menstruation.fill)
        assertEquals(CyclePhaseColors.Follicular, palette.follicular.fill)
        assertEquals(CyclePhaseColors.Ovulation, palette.ovulation.fill)
        assertEquals(CyclePhaseColors.Luteal, palette.luteal.fill)
    }

    @Test
    fun buildCyclePhasePalette_WHEN_darkTheme_THEN_usesAdjustedFillSubtleAlpha() {
        // GIVEN dark mode with no custom colors
        // WHEN building the palette
        val palette = buildCyclePhasePalette(darkTheme = true, customColors = null)

        // THEN fillSubtle alpha is 0.25 for all phases
        assertEquals(0.25f, palette.menstruation.fillSubtle.alpha, 0.01f)
        assertEquals(0.25f, palette.follicular.fillSubtle.alpha, 0.01f)
        assertEquals(0.25f, palette.ovulation.fillSubtle.alpha, 0.01f)
        assertEquals(0.25f, palette.luteal.fillSubtle.alpha, 0.01f)
    }

    @Test
    fun buildCyclePhasePalette_WHEN_lightTheme_THEN_fillSubtleAlphaIs0_3() {
        // GIVEN light mode with no custom colors
        // WHEN building the palette
        val palette = buildCyclePhasePalette(darkTheme = false, customColors = null)

        // THEN fillSubtle alpha is 0.3 for all phases
        assertEquals(0.3f, palette.menstruation.fillSubtle.alpha, 0.01f)
        assertEquals(0.3f, palette.follicular.fillSubtle.alpha, 0.01f)
        assertEquals(0.3f, palette.ovulation.fillSubtle.alpha, 0.01f)
        assertEquals(0.3f, palette.luteal.fillSubtle.alpha, 0.01f)
    }

    // --- Custom colors ---

    @Test
    fun buildCyclePhasePalette_WHEN_customColorsProvided_THEN_derivesRolesFromCustomBase() {
        // GIVEN a custom hot-pink base for menstruation
        val customPink = Color(0xFFFF69B4)
        val customColors = mapOf(CyclePhase.MENSTRUATION to customPink)

        // WHEN building the palette in light mode
        val palette = buildCyclePhasePalette(darkTheme = false, customColors = customColors)

        // THEN menstruation fill uses the custom color
        assertEquals(customPink, palette.menstruation.fill)
        // AND dot equals the custom base
        assertEquals(customPink, palette.menstruation.dot)
        // AND fillSubtle has reduced alpha
        assertEquals(0.3f, palette.menstruation.fillSubtle.alpha, 0.01f)
        // AND other phases still use defaults
        assertEquals(CyclePhaseColors.Follicular, palette.follicular.fill)
    }

    @Test
    fun buildCyclePhasePalette_WHEN_customColorsDarkMode_THEN_fillSubtleAlphaIs0_25() {
        // GIVEN a custom color in dark mode
        val customColor = Color(0xFF00BCD4)
        val customColors = mapOf(CyclePhase.FOLLICULAR to customColor)

        // WHEN building the palette
        val palette = buildCyclePhasePalette(darkTheme = true, customColors = customColors)

        // THEN the custom phase's fillSubtle alpha is 0.25
        assertEquals(0.25f, palette.follicular.fillSubtle.alpha, 0.01f)
    }

    // --- forPhase helper ---

    @Test
    fun forPhase_WHEN_calledWithMenstruation_THEN_returnsMenstruationColors() {
        // GIVEN a default light palette
        val palette = buildCyclePhasePalette(darkTheme = false)

        // WHEN requesting menstruation
        val result = palette.forPhase(CyclePhase.MENSTRUATION)

        // THEN the returned PhaseColors matches the menstruation property
        assertEquals(palette.menstruation, result)
    }

    @Test
    fun forPhase_WHEN_calledWithFollicular_THEN_returnsFollicularColors() {
        // GIVEN a default light palette
        val palette = buildCyclePhasePalette(darkTheme = false)

        // WHEN requesting follicular
        val result = palette.forPhase(CyclePhase.FOLLICULAR)

        // THEN the returned PhaseColors matches the follicular property
        assertEquals(palette.follicular, result)
    }

    @Test
    fun forPhase_WHEN_calledWithOvulation_THEN_returnsOvulationColors() {
        // GIVEN a default light palette
        val palette = buildCyclePhasePalette(darkTheme = false)

        // WHEN requesting ovulation
        val result = palette.forPhase(CyclePhase.OVULATION)

        // THEN the returned PhaseColors matches the ovulation property
        assertEquals(palette.ovulation, result)
    }

    @Test
    fun forPhase_WHEN_calledWithLuteal_THEN_returnsLutealColors() {
        // GIVEN a default light palette
        val palette = buildCyclePhasePalette(darkTheme = false)

        // WHEN requesting luteal
        val result = palette.forPhase(CyclePhase.LUTEAL)

        // THEN the returned PhaseColors matches the luteal property
        assertEquals(palette.luteal, result)
    }

    // --- Contrast derivation ---

    @Test
    fun PhaseColors_onFill_WHEN_lightFillColor_THEN_returnsDarkContrastColor() {
        // GIVEN a light / high-luminance custom base color (yellow)
        val lightBase = Color(0xFFFFFF00)
        val customColors = mapOf(CyclePhase.OVULATION to lightBase)

        // WHEN building the palette
        val palette = buildCyclePhasePalette(darkTheme = false, customColors = customColors)

        // THEN onFill is the dark contrast color (not white)
        val onFill = palette.ovulation.onFill
        assertNotEquals(Color.White, onFill)
        assertEquals(Color(0xFF1A1113), onFill)
    }

    @Test
    fun PhaseColors_onFill_WHEN_darkFillColor_THEN_returnsWhiteContrastColor() {
        // GIVEN a dark / low-luminance custom base color (dark blue)
        val darkBase = Color(0xFF000080)
        val customColors = mapOf(CyclePhase.FOLLICULAR to darkBase)

        // WHEN building the palette
        val palette = buildCyclePhasePalette(darkTheme = false, customColors = customColors)

        // THEN onFill is white for good contrast
        assertEquals(Color.White, palette.follicular.onFill)
    }

    // --- darken helper ---

    @Test
    fun darken_WHEN_calledWith15Percent_THEN_reducesRgbChannels() {
        // GIVEN a pure red color
        val red = Color(1f, 0f, 0f, 1f)

        // WHEN darkening by 15 %
        val result = darken(red, 0.15f)

        // THEN the red channel is reduced by 15 %
        assertEquals(0.85f, result.red, 0.001f)
        assertEquals(0f, result.green, 0.001f)
        assertEquals(0f, result.blue, 0.001f)
        assertEquals(1f, result.alpha, 0.001f)
    }
}
