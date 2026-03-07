package com.veleda.cyclewise.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.veleda.cyclewise.R
import com.veleda.cyclewise.ui.components.InfoButton
import com.veleda.cyclewise.ui.theme.LocalDimensions

/**
 * Reusable section wrapper that groups related settings inside an [OutlinedCard]
 * with a colored section title.
 *
 * The card applies only vertical padding. The [title] and any non-[ListItem] content
 * should add their own horizontal padding (`dims.md`). [ListItem] composables handle
 * their own 16 dp horizontal padding natively, avoiding double padding.
 *
 * @param title       Section header text, rendered in [titleMedium] with [primary] color.
 * @param modifier    Modifier applied to the outer [OutlinedCard].
 * @param onInfoClick Optional callback for an info button aligned to the end of the title row.
 *                    When non-null, an [InfoButton] is displayed. When null, no button is shown.
 * @param content     Card body content (typically [ListItem]s, [Switch]es, and custom rows).
 */
@Composable
internal fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    onInfoClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dims = LocalDimensions.current

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = dims.md),
            verticalArrangement = Arrangement.spacedBy(dims.sm)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
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
