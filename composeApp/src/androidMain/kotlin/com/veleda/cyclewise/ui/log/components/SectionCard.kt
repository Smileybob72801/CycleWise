package com.veleda.cyclewise.ui.log.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.components.HelpButton
import com.veleda.cyclewise.ui.components.InfoButton
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Reusable card wrapper for daily log sections.
 *
 * Provides consistent visual treatment: surfaceVariant background, medium rounded
 * shape, a leading icon and title row, followed by the section [content].
 *
 * @param title       Section heading text.
 * @param icon        Leading icon displayed beside the title.
 * @param onHelpClick Optional callback for a help button aligned to the end of the title row.
 *                    When non-null, a [HelpButton] is displayed before the info button.
 * @param onInfoClick Optional callback for an info button aligned to the end of the title row.
 *                    When non-null, an [InfoButton] is displayed. When null, no button is shown.
 * @param content     Slot for the section's interactive content.
 */
@Composable
internal fun SectionCard(
    title: String,
    icon: ImageVector,
    onHelpClick: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dims = LocalDimensions.current
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(dims.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dims.sm),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dims.sm),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (onHelpClick != null) {
                    HelpButton(
                        onClick = onHelpClick,
                        contentDescription = stringResource(R.string.help_button_cd, title),
                    )
                }
                if (onInfoClick != null) {
                    InfoButton(
                        onClick = onInfoClick,
                        contentDescription = stringResource(R.string.educational_info_button_cd, title),
                    )
                }
            }
            content()
        }
    }
}

/**
 * Standalone section title text, used outside of [SectionCard] when a card wrapper
 * is not needed.
 */
@Composable
internal fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = LocalDimensions.current.lg, bottom = LocalDimensions.current.sm)
    )
}
