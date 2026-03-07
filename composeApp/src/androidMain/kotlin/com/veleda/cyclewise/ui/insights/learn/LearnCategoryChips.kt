package com.veleda.cyclewise.ui.insights.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.domain.models.ArticleCategory
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Horizontal row of [FilterChip]s for selecting an article category.
 *
 * Includes an "All" chip plus one chip per [ArticleCategory]. The currently
 * selected chip is highlighted.
 *
 * @param selectedCategory The currently active filter, or `null` for "All".
 * @param onCategorySelected Callback when a chip is tapped.
 */
@Composable
internal fun LearnCategoryChips(
    selectedCategory: ArticleCategory?,
    onCategorySelected: (ArticleCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = LocalDimensions.current

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.sm),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(R.string.article_category_all)) }
            )
        }
        items(ArticleCategory.entries.toList(), key = { it.name }) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(stringResource(categoryStringRes(category))) }
            )
        }
    }
}

/**
 * Maps an [ArticleCategory] to its string resource ID.
 */
internal fun categoryStringRes(category: ArticleCategory): Int = when (category) {
    ArticleCategory.CYCLE_BASICS -> R.string.article_category_cycle_basics
    ArticleCategory.SYMPTOMS -> R.string.article_category_symptoms
    ArticleCategory.WELLNESS -> R.string.article_category_wellness
    ArticleCategory.WHEN_TO_SEE_A_DOCTOR -> R.string.article_category_when_to_see_doctor
}
