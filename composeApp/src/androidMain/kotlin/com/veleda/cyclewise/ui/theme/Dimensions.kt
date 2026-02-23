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
 * | Token     | Value |
 * |-----------|-------|
 * | xxs       | 2 dp  |
 * | xs        | 4 dp  |
 * | sm        | 8 dp  |
 * | md        | 16 dp |
 * | lg        | 24 dp |
 * | xl        | 32 dp |
 * | iconSm    | 28 dp |
 * | iconMd    | 48 dp |
 * | iconLg    | 64 dp |
 * | iconXl    | 96 dp |
 * | buttonMin | 48 dp |
 *
 * @property xxs Double-extra-small spacing (2 dp) — borders, hairline gaps.
 * @property xs Extra-small spacing (4 dp).
 * @property sm Small spacing (8 dp).
 * @property md Medium / default spacing (16 dp).
 * @property lg Large spacing (24 dp).
 * @property xl Extra-large spacing (32 dp).
 * @property iconSm Small icon size (28 dp).
 * @property iconMd Medium icon size (48 dp).
 * @property iconLg Large icon size (64 dp).
 * @property iconXl Extra-large icon size (96 dp).
 * @property buttonMin Minimum interactive touch-target size (48 dp) — Material 3 compliance.
 */
@Immutable
data class Dimensions(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val iconSm: Dp = 28.dp,
    val iconMd: Dp = 48.dp,
    val iconLg: Dp = 64.dp,
    val iconXl: Dp = 96.dp,
    val buttonMin: Dp = 48.dp,
)

/**
 * Composition-local providing the current [Dimensions] token set.
 *
 * Set inside [RhythmWiseTheme]; read via `LocalDimensions.current`.
 */
val LocalDimensions = staticCompositionLocalOf { Dimensions() }
