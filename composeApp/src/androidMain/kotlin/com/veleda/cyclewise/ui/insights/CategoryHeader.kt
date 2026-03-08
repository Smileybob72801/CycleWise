package com.veleda.cyclewise.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.insights.InsightCategory
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Clickable category header for the categorized insights accordion.
 *
 * Shows the category name, insight count, and an animated chevron that rotates
 * when expanded.
 */
@Composable
internal fun CategoryHeader(
    category: InsightCategory,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation",
    )

    val categoryLabel = when (category) {
        InsightCategory.PREDICTION -> stringResource(R.string.insights_category_prediction)
        InsightCategory.PATTERN -> stringResource(R.string.insights_category_pattern)
        InsightCategory.CORRELATION -> stringResource(R.string.insights_category_correlation)
        InsightCategory.TREND -> stringResource(R.string.insights_category_trend)
        InsightCategory.SUMMARY -> stringResource(R.string.insights_category_summary)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = dims.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$categoryLabel ($count)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) {
                stringResource(R.string.insights_content_description_collapse_category, categoryLabel)
            } else {
                stringResource(R.string.insights_content_description_expand_category, categoryLabel)
            },
            modifier = Modifier.rotate(rotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
