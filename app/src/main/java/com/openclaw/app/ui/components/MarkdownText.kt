package com.openclaw.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compose-native Markdown renderer.
 * Supports: headers, bold, italic, code, code blocks, links, lists, blockquotes.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    SelectionContainer {
        Column(modifier = modifier) {
            val blocks = parseMarkdownBlocks(markdown)
            blocks.forEach { block ->
                when (block) {
                    is MdBlock.Heading -> HeadingBlock(block, color)
                    is MdBlock.CodeBlock -> CodeBlock(block)
                    is MdBlock.BlockQuote -> BlockQuoteBlock(block, color)
                    is MdBlock.ListBlock -> ListBlock(block, color, style)
                    is MdBlock.Paragraph -> ParagraphBlock(block, color, style)
                    is MdBlock.HorizontalRule -> HorizontalRule()
                }
            }
        }
    }
}

// --- Block types ---
private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class CodeBlock(val language: String, val code: String) : MdBlock()
    data class BlockQuote(val text: String) : MdBlock()
    data class ListBlock(val items: List<String>, val ordered: Boolean) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data object HorizontalRule : MdBlock()
}

// --- Block parser ---
private fun parseMarkdownBlocks(markdown: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = markdown.split("\n")
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Empty line
        if (line.isBlank()) {
            i++
            continue
        }

        // Code block
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            blocks.add(MdBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            continue
        }

        // Heading
        val headingMatch = Regex("^(#{1,6})\\s+(.+)").find(line)
        if (headingMatch != null) {
            blocks.add(MdBlock.Heading(headingMatch.groupValues[1].length, headingMatch.groupValues[2]))
            i++
            continue
        }

        // Horizontal rule
        if (Regex("^([-*_])\\s*\\1\\s*\\1[\\s\\1]*$").matches(line.trim())) {
            blocks.add(MdBlock.HorizontalRule)
            i++
            continue
        }

        // Blockquote
        if (line.trimStart().startsWith("> ")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trimStart().startsWith("> ")) {
                quoteLines.add(lines[i].trimStart().removePrefix("> "))
                i++
            }
            blocks.add(MdBlock.BlockQuote(quoteLines.joinToString("\n")))
            continue
        }

        // Unordered list
        if (Regex("^\\s*[-*+]\\s+").containsMatchIn(line)) {
            val items = mutableListOf<String>()
            while (i < lines.size && Regex("^\\s*[-*+]\\s+").containsMatchIn(lines[i])) {
                items.add(lines[i].replace(Regex("^\\s*[-*+]\\s+"), ""))
                i++
            }
            blocks.add(MdBlock.ListBlock(items, ordered = false))
            continue
        }

        // Ordered list
        if (Regex("^\\s*\\d+\\.\\s+").containsMatchIn(line)) {
            val items = mutableListOf<String>()
            while (i < lines.size && Regex("^\\s*\\d+\\.\\s+").containsMatchIn(lines[i])) {
                items.add(lines[i].replace(Regex("^\\s*\\d+\\.\\s+"), ""))
                i++
            }
            blocks.add(MdBlock.ListBlock(items, ordered = true))
            continue
        }

        // Paragraph (collect consecutive non-blank lines)
        val paraLines = mutableListOf<String>()
        while (i < lines.size && lines[i].isNotBlank() &&
            !lines[i].trimStart().startsWith("```") &&
            !lines[i].trimStart().startsWith("#") &&
            !lines[i].trimStart().startsWith("> ") &&
            !Regex("^\\s*[-*+]\\s+").containsMatchIn(lines[i]) &&
            !Regex("^\\s*\\d+\\.\\s+").containsMatchIn(lines[i]) &&
            !Regex("^([-*_])\\s*\\1\\s*\\1[\\s\\1]*$").matches(lines[i].trim())
        ) {
            paraLines.add(lines[i])
            i++
        }
        if (paraLines.isNotEmpty()) {
            blocks.add(MdBlock.Paragraph(paraLines.joinToString("\n")))
        }
    }

    return blocks
}

// --- Block composables ---

@Composable
private fun HeadingBlock(block: MdBlock.Heading, baseColor: Color) {
    val style = when (block.level) {
        1 -> MaterialTheme.typography.headlineLarge
        2 -> MaterialTheme.typography.headlineMedium
        3 -> MaterialTheme.typography.titleLarge
        4 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    Text(
        text = block.text,
        style = style.copy(fontWeight = FontWeight.Bold),
        color = if (baseColor != Color.Unspecified) baseColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun CodeBlock(block: MdBlock.CodeBlock) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp
    ) {
        Column {
            if (block.language.isNotBlank()) {
                Text(
                    text = block.language,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp)
                )
            }
            Text(
                text = block.code,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(12.dp)
                    .horizontalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun BlockQuoteBlock(block: MdBlock.BlockQuote, baseColor: Color) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row {
            Surface(
                modifier = Modifier
                    .width(3.dp)
                    .height(IntrinsicSize.Max),
                color = MaterialTheme.colorScheme.primary
            ) {}
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (baseColor != Color.Unspecified) baseColor.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 12.dp)
            )
        }
    }
}

@Composable
private fun ListBlock(block: MdBlock.ListBlock, baseColor: Color, style: TextStyle) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        block.items.forEachIndexed { index, item ->
            Row(modifier = Modifier.padding(vertical = 1.dp)) {
                val bullet = if (block.ordered) "${index + 1}." else "•"
                Text(
                    text = bullet,
                    style = style,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(24.dp)
                )
                Text(
                    text = item,
                    style = style,
                    color = if (baseColor != Color.Unspecified) baseColor
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ParagraphBlock(block: MdBlock.Paragraph, baseColor: Color, style: TextStyle) {
    Text(
        text = parseInlineMarkdown(block.text),
        style = style,
        color = if (baseColor != Color.Unspecified) baseColor
        else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun HorizontalRule() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

// --- Inline markdown parser (bold, italic, code, links) ---

private fun parseInlineMarkdown(text: String) = buildAnnotatedString {
    // Pattern: **bold**, *italic*, `code`, [text](url), ~~strikethrough~~
    val pattern = Regex(
        """\*\*(.+?)\*\*|`([^`]+)`|\*(.+?)\*|\[([^\]]+)\]\(([^)]+)\)|~~(.+?)~~"""
    )
    var lastEnd = 0

    for (match in pattern.findAll(text)) {
        // Append text before this match
        if (match.range.first > lastEnd) {
            append(text.substring(lastEnd, match.range.first))
        }

        when {
            // **bold**
            match.groupValues[1].isNotEmpty() -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[1])
                }
            }
            // `code`
            match.groupValues[2].isNotEmpty() -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        background = Color.Gray.copy(alpha = 0.15f)
                    )
                ) {
                    append(match.groupValues[2])
                }
            }
            // *italic*
            match.groupValues[3].isNotEmpty() -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(match.groupValues[3])
                }
            }
            // [text](url)
            match.groupValues[4].isNotEmpty() -> {
                withStyle(
                    SpanStyle(
                        color = Color(0xFF6750A4),
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(match.groupValues[4])
                }
            }
            // ~~strikethrough~~
            match.groupValues[6].isNotEmpty() -> {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(match.groupValues[6])
                }
            }
        }

        lastEnd = match.range.last + 1
    }

    // Append remaining text
    if (lastEnd < text.length) {
        append(text.substring(lastEnd))
    }
}
