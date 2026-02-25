package com.veleda.cyclewise.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Regex matching `**bold**` spans in lightweight Markdown.
 *
 * Captures the inner text (group 1) between double-asterisk delimiters.
 * Uses a non-greedy quantifier so adjacent bold spans are matched independently.
 */
private val boldRegex = Regex("""\*\*(.+?)\*\*""")

/**
 * Renders lightweight Markdown as an [AnnotatedString] inside a [Text] composable.
 *
 * Supported syntax:
 * - `**bold**` — rendered with [FontWeight.Bold]
 * - `\n- ` bullet list items — replaced with `\n\u2022 ` (bullet character)
 * - `\n\n` paragraph breaks — handled natively by [Text]
 *
 * This avoids pulling in a full Markdown library for the small subset of
 * formatting used in educational article bodies.
 *
 * @param text     Raw Markdown-formatted text.
 * @param modifier Modifier applied to the underlying [Text].
 * @param style    [TextStyle] forwarded to [Text]; defaults to [LocalTextStyle].
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(
        text = parseMarkdown(text),
        modifier = modifier,
        style = style,
    )
}

/**
 * Parses lightweight Markdown into an [AnnotatedString].
 *
 * Performs two passes:
 * 1. **Bullet replacement** — `\n- ` becomes `\n\u2022 ` for cleaner list rendering.
 *    A leading `- ` at the very start of the string is also replaced.
 * 2. **Bold spans** — `**...**` pairs are converted to [SpanStyle] with
 *    [FontWeight.Bold]. Unclosed `**` delimiters are left as literal text.
 *
 * This function is a pure function with no Compose dependency, making it
 * straightforward to unit-test.
 *
 * @param raw The raw Markdown-formatted text.
 * @return An [AnnotatedString] with bold styling applied.
 */
internal fun parseMarkdown(raw: String): AnnotatedString {
    // Step 1: replace bullet markers with bullet characters.
    val bulletReplaced = raw
        .replace("\n- ", "\n\u2022 ")
        .let { if (it.startsWith("- ")) "\u2022 " + it.removePrefix("- ") else it }

    // Step 2: parse **bold** spans.
    return buildAnnotatedString {
        var cursor = 0
        for (match in boldRegex.findAll(bulletReplaced)) {
            // Append text before the match as-is.
            append(bulletReplaced.substring(cursor, match.range.first))
            // Append the inner text with bold styling.
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            cursor = match.range.last + 1
        }
        // Append any remaining text after the last match.
        if (cursor < bulletReplaced.length) {
            append(bulletReplaced.substring(cursor))
        }
    }
}
