package com.veleda.cyclewise.ui.components

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.veleda.cyclewise.ui.utils.isReducedMotionEnabled

/**
 * Wrapper around the Lottie library's animation composable.
 *
 * Plays a Lottie JSON animation from the `res/raw/` directory. When the system
 * has reduced motion enabled (animator duration scale = 0), a static final frame
 * is shown instead.
 *
 * @param animationResId    Raw resource ID of the Lottie JSON file.
 * @param modifier          Modifier applied to the animation.
 * @param iterations        Number of times to play the animation. Defaults to infinite.
 * @param contentDescription Accessibility content description for the animation.
 */
@Composable
fun LottieAnimationBox(
    @RawRes animationResId: Int,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    contentDescription: String? = null,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationResId))
    val reducedMotion = isReducedMotionEnabled()

    val semanticsModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    if (reducedMotion) {
        LottieAnimation(
            composition = composition,
            progress = { 1f },
            modifier = semanticsModifier,
        )
    } else {
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = iterations,
        )
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = semanticsModifier,
        )
    }
}
