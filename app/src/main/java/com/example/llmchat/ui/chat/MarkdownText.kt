package com.example.llmchat.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * A small, dependency-free Markdown renderer for chat bubbles.
 *
 * Supports fenced code blocks, ATX headings, unordered/ordered lists,
 * blockquotes, horizontal rules, GFM tables, and inline bold / italic /
 * inline-code / strikethrough / links.
 */
@Composable
fun MarkdownText(
    text: String,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val codeBackground = MaterialTheme.colorScheme.surface
    val linkColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val blocks = remember(text) { parseMarkdownBlocks(text) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Code -> CodeBlock(block.code, style)

                is MdBlock.Heading -> Text(
                    text = parseInline(block.text, linkColor, codeBackground),
                    color = color,
                    style = headingStyle(block.level, style),
                    fontWeight = FontWeight.Bold
                )

                is MdBlock.Bullet -> Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = block.marker, color = color, style = style)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = parseInline(block.text, linkColor, codeBackground),
                        color = color,
                        style = style
                    )
                }

                is MdBlock.Quote -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .background(mutedColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = parseInline(block.text, linkColor, codeBackground),
                        color = mutedColor,
                        style = style,
                        fontStyle = FontStyle.Italic
                    )
                }

                MdBlock.Rule -> HorizontalDivider()

                is MdBlock.Table -> MarkdownTable(
                    headers = block.headers,
                    rows = block.rows,
                    color = color,
                    baseStyle = style
                )

                is MdBlock.Paragraph -> Text(
                    text = parseInline(block.text, linkColor, codeBackground),
                    color = color,
                    style = style
                )
            }
        }
    }
}

@Composable
private fun CodeBlock(code: String, baseStyle: TextStyle) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Text(
                text = code,
                style = baseStyle.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = baseStyle.fontSize * 0.9f
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MarkdownTable(
    headers: List<String>,
    rows: List<List<String>>,
    color: Color,
    baseStyle: TextStyle
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outlineVariant
    val columnCount = headers.size.coerceAtLeast(1)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val viewport = maxWidth
        Box(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .width(maxOf((columnCount * 140).dp, viewport))
                    .border(1.dp, outline)
            ) {
                TableRow(headers, header = true, color, linkColor, codeBackground, outline, baseStyle)
                rows.forEach { row ->
                    HorizontalDivider(color = outline)
                    TableRow(row, header = false, color, linkColor, codeBackground, outline, baseStyle)
                }
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    header: Boolean,
    color: Color,
    linkColor: Color,
    codeBackground: Color,
    outline: Color,
    baseStyle: TextStyle
) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        cells.forEachIndexed { index, cell ->
            if (index > 0) {
                VerticalDivider(color = outline)
            }
            val base = Modifier.weight(1f)
            val withBackground =
                if (header) base.background(MaterialTheme.colorScheme.surfaceVariant) else base
            Box(modifier = withBackground.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = parseInline(cell, linkColor, codeBackground),
                    color = color,
                    style = baseStyle.copy(fontSize = baseStyle.fontSize * 0.9f),
                    fontWeight = if (header) FontWeight.Bold else null
                )
            }
        }
    }
}

private fun headingStyle(level: Int, base: TextStyle): TextStyle = when (level) {
    1 -> base.copy(fontSize = base.fontSize * 1.5)
    2 -> base.copy(fontSize = base.fontSize * 1.3)
    3 -> base.copy(fontSize = base.fontSize * 1.15)
    else -> base
}

private sealed interface MdBlock {
    data class Code(val code: String) : MdBlock
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Bullet(val marker: String, val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data object Rule : MdBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MdBlock
    data class Paragraph(val text: String) : MdBlock
}

private val HEADING_REGEX = Regex("^#{1,6}\\s+.*")
private val RULE_REGEX = Regex("^([-*_])\\1{2,}$")
private val QUOTE_REGEX = Regex("^>.*")
private val BULLET_REGEX = Regex("^[-*+]\\s+.*")
private val ORDERED_REGEX = Regex("^\\d+[.)]\\s+.*")
private val DELIMITER_CELL_REGEX = Regex("^:?-+:?$")

private fun parseMarkdownBlocks(source: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = source.replace("\r\n", "\n").split("\n")
    val paragraph = StringBuilder()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks.add(MdBlock.Paragraph(paragraph.toString().trim()))
            paragraph.clear()
        }
    }

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        when {
            trimmed.startsWith("```") -> {
                flushParagraph()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    code.append(lines[i]).append('\n')
                    i++
                }
                if (i < lines.size) i++
                blocks.add(MdBlock.Code(code.toString().trimEnd('\n')))
            }

            trimmed.isEmpty() -> {
                flushParagraph()
                i++
            }

            HEADING_REGEX.matches(trimmed) -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length.coerceAtMost(6)
                blocks.add(MdBlock.Heading(level, trimmed.drop(level).trim()))
                i++
            }

            trimmed.contains('|') && i + 1 < lines.size && isTableDelimiter(lines[i + 1]) -> {
                flushParagraph()
                val headers = splitTableRow(trimmed)
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size) {
                    val rowLine = lines[i].trim()
                    if (rowLine.isEmpty() || !rowLine.contains('|')) break
                    rows.add(normalizeRow(splitTableRow(rowLine), headers.size))
                    i++
                }
                blocks.add(MdBlock.Table(headers, rows))
            }

            RULE_REGEX.matches(trimmed) -> {
                flushParagraph()
                blocks.add(MdBlock.Rule)
                i++
            }

            QUOTE_REGEX.matches(trimmed) -> {
                flushParagraph()
                blocks.add(MdBlock.Quote(trimmed.removePrefix(">").trim()))
                i++
            }

            BULLET_REGEX.matches(trimmed) -> {
                flushParagraph()
                blocks.add(MdBlock.Bullet("•", trimmed.substring(1).trim()))
                i++
            }

            ORDERED_REGEX.matches(trimmed) -> {
                flushParagraph()
                val number = trimmed.takeWhile { it.isDigit() }
                val body = trimmed.dropWhile { it.isDigit() }.drop(1).trim()
                blocks.add(MdBlock.Bullet("$number.", body))
                i++
            }

            else -> {
                if (paragraph.isNotEmpty()) paragraph.append('\n')
                paragraph.append(trimmed)
                i++
            }
        }
    }
    flushParagraph()
    return blocks
}

private fun isTableDelimiter(line: String): Boolean {
    val trimmed = line.trim()
    if (!trimmed.contains('|')) return false
    val cells = splitTableRow(trimmed)
    return cells.isNotEmpty() && cells.all { it.matches(DELIMITER_CELL_REGEX) }
}

private fun splitTableRow(line: String): List<String> {
    var s = line.trim()
    if (s.startsWith("|")) s = s.substring(1)
    if (s.endsWith("|")) s = s.dropLast(1)
    return s.split("|").map { it.trim() }
}

private fun normalizeRow(cells: List<String>, size: Int): List<String> = when {
    cells.size == size -> cells
    cells.size > size -> cells.take(size)
    else -> cells + List(size - cells.size) { "" }
}

private val INLINE_REGEX = Regex(
    "\\*\\*(.+?)\\*\\*" +                        // bold
        "|(`[^`]+`)" +                          // inline code
        "|(~~.+?~~)" +                          // strikethrough
        "|(\\*[^*\\n]+\\*)" +                    // italic *
        "|(_[^_\\n]+_)" +                        // italic _
        "|(\\[([^\\]]+)\\]\\(([^)]+)\\))"        // link
)

private fun parseInline(text: String, linkColor: Color, codeBackground: Color): AnnotatedString =
    buildAnnotatedString {
        var last = 0
        for (match in INLINE_REGEX.findAll(text)) {
            if (match.range.first > last) {
                append(text.substring(last, match.range.first))
            }
            val token = match.value
            when {
                token.startsWith("**") && token.endsWith("**") ->
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(token.substring(2, token.length - 2))
                    }

                token.startsWith("`") && token.endsWith("`") ->
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground
                        )
                    ) {
                        append(token.substring(1, token.length - 1))
                    }

                token.startsWith("~~") && token.endsWith("~~") ->
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(token.substring(2, token.length - 2))
                    }

                token.startsWith("[") && token.contains("](") -> {
                    val close = token.indexOf("](")
                    val label = token.substring(1, close)
                    withStyle(
                        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                    ) {
                        append(label)
                    }
                }

                else -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(token.substring(1, token.length - 1))
                }
            }
            last = match.range.last + 1
        }
        if (last < text.length) {
            append(text.substring(last))
        }
    }
