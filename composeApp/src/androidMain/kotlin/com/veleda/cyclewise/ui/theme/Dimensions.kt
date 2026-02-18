package com.veleda.cyclewise.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens for the RhythmWise design system.
 *
 * A small, fixed set of multiples so that padding and margins stay consistent
 * across every screen. Access via `LocalDimensions.current`.
 *
 * | Token | Value |
 * |-------|-------|
 * | xs    | 4 dp  |
 * | sm    | 8 dp  |
 * | md    | 16 dp |
 * | lg    | 24 dp |
 * | xl    | 32 dp |
 *
 * @property xs Extra-small spacing (4 dp).
 * @property sm Small spacing (8 dp).
 * @property md Medium / default spacing (16 dp).
 * @property lg Large spacing (24 dp).
 * @property xl Extra-large spacing (32 dp).
 */
@Immutable
data class Dimensions(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
)

/**
 * Composition-local providing the current [Dimensions] token set.
 *
 * Set inside [RhythmWiseTheme]; read via `LocalDimensions.current`.
 */
val LocalDimensions = staticCompositionLocalOf { Dimensions() }
