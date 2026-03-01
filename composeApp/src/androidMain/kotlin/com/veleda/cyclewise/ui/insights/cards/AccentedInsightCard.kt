package com.veleda.cyclewise.ui.insights.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Shared card wrapper with a colored left-border accent for visual differentiation.
 *
 * All insight cards use this wrapper for consistent styling: medium-rounded card shape,
 * `surfaceVariant` background, and a 4.dp accent bar drawn on the left edge.
 *
 * @param accentColor Color for the left-border accent.
 * @param modifier    Modifier applied to the outer [Card].
 * @param content     Card body content.
 */
@Composable
internal fun AccentedInsightCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dims = LocalDimensions.current
    val accentWidth = dims.xs

    Card(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = accentColor,
                    topLeft = Offset.Zero,
                    size = Size(accentWidth.toPx(), size.height)
                )
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(
                start = dims.md + dims.sm + dims.xs,
                top = dims.md,
                end = dims.md,
                bottom = dims.md
            ),
            verticalArrangement = Arrangement.spacedBy(dims.sm)
        ) {
            content()
        }
    }
}
