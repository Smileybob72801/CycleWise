package com.veleda.cyclewise.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import dev.jeziellago.compose.markdowntext.MarkdownText as LibraryMarkdownText

/**
 * Renders Markdown text using the compose-markdown library.
 *
 * Supports bold, bullet lists, paragraph breaks, and other standard Markdown formatting.
 * Image rendering is disabled as a defense-in-depth measure against remote resource loading.
 *
 * @param text     Raw Markdown-formatted text.
 * @param modifier Modifier applied to the underlying composable.
 * @param style    [TextStyle] forwarded to the library; defaults to [LocalTextStyle].
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    val mergedStyle = style.copy(
        color = if (style.color == Color.Unspecified) {
            MaterialTheme.colorScheme.onSurface
        } else {
            style.color
        },
    )

    @Suppress("DEPRECATION")
    LibraryMarkdownText(
        markdown = text,
        modifier = modifier,
        style = mergedStyle,
        disableLinkMovementMethod = true,
    )
}
