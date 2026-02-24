package com.veleda.cyclewise.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Modal bottom sheet displaying one or more [EducationalArticle]s with source
 * attribution and a medical disclaimer footer.
 *
 * Uses `skipPartiallyExpanded = true` to match the existing [TrackerScreen]
 * bottom sheet pattern — the sheet opens fully rather than stopping at a
 * partially-expanded state.
 *
 * @param articles  The articles to display. Must be non-empty.
 * @param onDismiss Callback invoked when the sheet is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationalBottomSheet(
    articles: List<EducationalArticle>,
    onDismiss: () -> Unit,
) {
    val dims = LocalDimensions.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = dims.md, vertical = dims.sm),
            verticalArrangement = Arrangement.spacedBy(dims.md),
        ) {
            items(articles, key = { it.id }) { article ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dims.sm),
                ) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = article.body,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    SourceAttribution(sourceName = article.sourceName)
                }
                Spacer(Modifier.height(dims.xs))
                HorizontalDivider()
            }

            item {
                MedicalDisclaimer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dims.sm, bottom = dims.md),
                )
            }
        }
    }
}
