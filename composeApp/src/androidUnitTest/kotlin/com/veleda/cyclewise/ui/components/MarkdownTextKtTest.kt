package com.veleda.cyclewise.ui.components

import androidx.compose.ui.text.font.FontWeight
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [parseMarkdown], the pure-function core of [MarkdownText].
 *
 * Uses Given-When-Then structure per project conventions.
 */
class MarkdownTextKtTest {

    // ── Bold rendering ──────────────────────────────────────────────────

    @Test
    fun `parseMarkdown WHEN text contains bold THEN applies bold SpanStyle`() {
        // GIVEN
        val input = "This is **bold** text"

        // WHEN
        val result = parseMarkdown(input)

        // THEN — plain text is correct
        assertEquals("This is bold text", result.text)

        // THEN — one bold span covering "bold"
        val spans = result.spanStyles
        assertEquals(1, spans.size)
        assertEquals(FontWeight.Bold, spans[0].item.fontWeight)
        assertEquals("bold", result.text.substring(spans[0].start, spans[0].end))
    }

    @Test
    fun `parseMarkdown WHEN text has adjacent bold spans THEN each is styled independently`() {
        // GIVEN
        val input = "**one** and **two**"

        // WHEN
        val result = parseMarkdown(input)

        // THEN
        assertEquals("one and two", result.text)
        assertEquals(2, result.spanStyles.size)
        assertEquals("one", result.text.substring(result.spanStyles[0].start, result.spanStyles[0].end))
        assertEquals("two", result.text.substring(result.spanStyles[1].start, result.spanStyles[1].end))
    }

    @Test
    fun `parseMarkdown WHEN bold is at start THEN applies correctly`() {
        // GIVEN
        val input = "**Start** of text"

        // WHEN
        val result = parseMarkdown(input)

        // THEN
        assertEquals("Start of text", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(5, result.spanStyles[0].end)
    }

    @Test
    fun `parseMarkdown WHEN bold is at end THEN applies correctly`() {
        // GIVEN
        val input = "End of **text**"

        // WHEN
        val result = parseMarkdown(input)

        // THEN
        assertEquals("End of text", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals("text", result.text.substring(result.spanStyles[0].start, result.spanStyles[0].end))
    }

    // ── Bullet replacement ──────────────────────────────────────────────

    @Test
    fun `parseMarkdown WHEN text has bullet items THEN replaces dash with bullet character`() {
        // GIVEN
        val input = "List:\n- First item\n- Second item"

        // WHEN
        val result = parseMarkdown(input)

        // THEN
        assertEquals("List:\n\u2022 First item\n\u2022 Second item", result.text)
    }

    @Test
    fun `parseMarkdown WHEN text starts with bullet THEN replaces leading dash`() {
        // GIVEN
        val input = "- First item\n- Second item"

        // WHEN
        val result = parseMarkdown(input)

        // THEN
        assertEquals("\u2022 First item\n\u2022 Second item", result.text)
    }

    // ── Pass-through ────────────────────────────────────────────────────

    @Test
    fun `parseMarkdown WHEN no markdown THEN returns unchanged text`() {
        // GIVEN
        val input = "Plain text with no formatting"

        // WHEN
        val result = parseMarkdown(input)

        // THEN
        assertEquals("Plain text with no formatting", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `parseMarkdown WHEN empty string THEN returns empty`() {
        // GIVEN / WHEN
        val result = parseMarkdown("")

        // THEN
        assertEquals("", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    // ── Edge cases ──────────────────────────────────────────────────────

    @Test
    fun `parseMarkdown WHEN unclosed bold THEN treats as literal text`() {
        // GIVEN — single ** without a matching close
        val input = "This has **unclosed bold"

        // WHEN
        val result = parseMarkdown(input)

        // THEN — the ** remains as literal text, no spans applied
        assertEquals("This has **unclosed bold", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `parseMarkdown WHEN bold and bullets combined THEN both render`() {
        // GIVEN — realistic article content
        val input = "**What you may notice:**\n- Bleeding that varies\n- Cramping"

        // WHEN
        val result = parseMarkdown(input)

        // THEN — bullet replacement + bold
        assertEquals("What you may notice:\n\u2022 Bleeding that varies\n\u2022 Cramping", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(FontWeight.Bold, result.spanStyles[0].item.fontWeight)
        assertEquals(
            "What you may notice:",
            result.text.substring(result.spanStyles[0].start, result.spanStyles[0].end),
        )
    }

    @Test
    fun `parseMarkdown WHEN paragraph breaks present THEN preserves them`() {
        // GIVEN
        val input = "First paragraph.\n\nSecond paragraph."

        // WHEN
        val result = parseMarkdown(input)

        // THEN
        assertEquals("First paragraph.\n\nSecond paragraph.", result.text)
    }
}
