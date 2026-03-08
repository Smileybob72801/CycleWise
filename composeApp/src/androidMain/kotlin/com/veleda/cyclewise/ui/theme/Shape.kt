package com.veleda.cyclewise.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner-radius tokens for the RhythmWise design system.
 *
 * Passed to [androidx.compose.material3.MaterialTheme] so that Material 3
 * components (cards, dialogs, sheets) automatically pick up the brand radii.
 *
 * | Token   | Radius |
 * |---------|--------|
 * | small   | 8 dp   |
 * | medium  | 12 dp  |
 * | large   | 16 dp  |
 */
val RhythmWiseShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)
