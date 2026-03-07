package com.veleda.cyclewise.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.auth.SCRIM_ALPHA
import com.veleda.cyclewise.ui.theme.LocalDimensions
import com.veleda.cyclewise.ui.utils.isReducedMotionEnabled
import kotlinx.coroutines.delay

/** Duration to show the static checkmark when reduced motion is enabled. */
private const val REDUCED_MOTION_DISPLAY_MS = 800L

/**
 * Full-screen success animation overlay with a scrim background.
 *
 * Shows a centered Lottie checkmark animation that plays once, then invokes
 * [onAnimationComplete]. When reduced motion is enabled, shows a static
 * checkmark briefly before dismissing.
 *
 * @param visible             Whether the overlay is visible.
 * @param onAnimationComplete Callback invoked when the animation finishes or the
 *                            reduced-motion delay elapses.
 * @param modifier            Modifier applied to the root container.
 */
@Composable
fun SuccessAnimation(
    visible: Boolean,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current
    val reducedMotion = isReducedMotionEnabled()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            if (reducedMotion) {
                LottieAnimationBox(
                    animationResId = R.raw.anim_success_checkmark,
                    modifier = Modifier.size(dims.iconXl),
                    iterations = 1,
                    contentDescription = stringResource(R.string.lottie_cd_success),
                )
                LaunchedEffect(Unit) {
                    delay(REDUCED_MOTION_DISPLAY_MS)
                    onAnimationComplete()
                }
            } else {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.anim_success_checkmark)
                )
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = 1,
                )
                LottieAnimationBox(
                    animationResId = R.raw.anim_success_checkmark,
                    modifier = Modifier.size(dims.iconXl),
                    iterations = 1,
                    contentDescription = stringResource(R.string.lottie_cd_success),
                )
                LaunchedEffect(progress) {
                    if (progress == 1f) {
                        onAnimationComplete()
                    }
                }
            }
        }
    }
}
