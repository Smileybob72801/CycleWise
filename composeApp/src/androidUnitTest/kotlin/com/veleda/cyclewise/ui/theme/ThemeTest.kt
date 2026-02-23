package com.veleda.cyclewise.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Unit tests for the RhythmWise design-system tokens defined in
 * `Color.kt`, `Dimensions.kt`, `Shape.kt`, and `Type.kt`.
 */
class ThemeTest {

    // --- RhythmWiseColors ---

    @Test
    fun RhythmWiseColors_StarGold_WHEN_accessed_THEN_equalsAppPaletteAccent() {
        // GIVEN the expected accent color from the Velvet Garden palette
        val expected = AppPalette.Light.Accent

        // WHEN accessing StarGold
        val result = RhythmWiseColors.StarGold

        // THEN it matches the palette accent value
        assertEquals(expected, result)
    }

    // --- Dimensions ---

    @Test
    fun Dimensions_WHEN_defaultConstructed_THEN_hasExpectedSpacingValues() {
        // GIVEN default Dimensions
        val dims = Dimensions()

        // WHEN inspecting each token
        // THEN they match the design-system spec
        assertEquals(4.dp, dims.xs)
        assertEquals(8.dp, dims.sm)
        assertEquals(16.dp, dims.md)
        assertEquals(24.dp, dims.lg)
        assertEquals(32.dp, dims.xl)
    }

    // --- Typography ---

    @Test
    fun RhythmWiseTypography_bodyLarge_WHEN_accessed_THEN_usesNunitoFontFamily() {
        // GIVEN the theme typography
        val typography = RhythmWiseTypography

        // WHEN accessing bodyLarge
        val bodyLarge = typography.bodyLarge

        // THEN the font family is the custom Nunito family (not the platform default)
        assertEquals(NunitoFontFamily, bodyLarge.fontFamily)
        assertNotEquals(FontFamily.Default, bodyLarge.fontFamily)
    }

    // --- Shapes ---

    @Test
    fun RhythmWiseShapes_medium_WHEN_accessed_THEN_has12dpCornerRadius() {
        // GIVEN the theme shapes
        val shapes = RhythmWiseShapes

        // WHEN accessing the medium shape
        val medium = shapes.medium

        // THEN it is a RoundedCornerShape with 12.dp corners
        assertEquals(RoundedCornerShape(12.dp), medium)
    }

    @Test
    fun RhythmWiseShapes_small_WHEN_accessed_THEN_has8dpCornerRadius() {
        // GIVEN the theme shapes
        val shapes = RhythmWiseShapes

        // WHEN accessing the small shape
        val small = shapes.small

        // THEN it is a RoundedCornerShape with 8.dp corners
        assertEquals(RoundedCornerShape(8.dp), small)
    }

    @Test
    fun RhythmWiseShapes_large_WHEN_accessed_THEN_has16dpCornerRadius() {
        // GIVEN the theme shapes
        val shapes = RhythmWiseShapes

        // WHEN accessing the large shape
        val large = shapes.large

        // THEN it is a RoundedCornerShape with 16.dp corners
        assertEquals(RoundedCornerShape(16.dp), large)
    }
}
