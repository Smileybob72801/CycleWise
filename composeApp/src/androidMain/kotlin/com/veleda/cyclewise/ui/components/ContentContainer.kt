package com.veleda.cyclewise.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Responsive content wrapper that caps the maximum width of its [content] and
 * centers it horizontally within the available space.
 *
 * On phones (where the screen width is narrower than [maxWidth]), this composable
 * has no visible effect — content fills the screen as usual. On tablets and in
 * landscape mode, the content is constrained to [maxWidth] and centered, keeping
 * text lines at a comfortable reading length and preventing interactive elements
 * from spreading too far apart.
 *
 * The outer [Box] fills all available space and aligns the child at [Alignment.TopCenter].
 * The inner [Box] applies `widthIn(max = maxWidth)` so content never exceeds the cap.
 *
 * @param maxWidth Maximum width for the content area. Defaults to
 *   [LocalDimensions.current.contentMaxWidth][com.veleda.cyclewise.ui.theme.Dimensions.contentMaxWidth]
 *   (600 dp). Pass a different token for grid-style (840 dp) or auth (480 dp) screens.
 * @param modifier Modifier applied to the outer [Box] (the full-size container).
 * @param content  The composable content to constrain.
 */
@Composable
fun ContentContainer(
    maxWidth: Dp = LocalDimensions.current.contentMaxWidth,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = maxWidth).fillMaxSize()) {
            content()
        }
    }
}
