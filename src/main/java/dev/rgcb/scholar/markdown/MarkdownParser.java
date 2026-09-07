package dev.rgcb.scholar.markdown;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Parser for Scholar Markdown v0.1.
 */
public final class MarkdownParser {
    public ParseResult parse(String source) {
        Objects.requireNonNull(source, "source");

        var diagnostics = new ArrayList<MarkdownDiagnostic>();
        var blocks = new ArrayList<BlockNode>();
        var paragraphLines = new ArrayList<String>();
        var lines = normalizeLineEndings(source).split("\n", -1);

        for (var lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            var line = lines[lineIndex];
            if (line.isBlank()) {
                flushParagraph(paragraphLines, blocks, diagnostics);
                continue;
            }

            var tableParse = parseTable(lines, lineIndex, diagnostics);
            if (tableParse.table() != null) {
                flushParagraph(paragraphLines, blocks, diagnostics);
                blocks.add(tableParse.table());
                lineIndex = tableParse.nextLineIndex() - 1;
                continue;
            }
            if (tableParse.consumedMalformedLines() > 0) {
                for (var malformedIndex = lineIndex; malformedIndex < lineIndex + tableParse.consumedMalformedLines(); malformedIndex++) {
                    paragraphLines.add(lines[malformedIndex].trim());
                }
                lineIndex += tableParse.consumedMalformedLines() - 1;
                continue;
            }

            var heading = parseHeading(line, diagnostics);
            if (heading != null) {
                flushParagraph(paragraphLines, blocks, diagnostics);
                blocks.add(heading);
            } else {
                paragraphLines.add(line.trim());
                detectUnsupportedBlockSyntax(line, diagnostics);
            }
        }

        flushParagraph(paragraphLines, blocks, diagnostics);
        return new ParseResult(new Document(blocks), diagnostics);
    }

    private static String normalizeLineEndings(String source) {
        return source.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static void flushParagraph(
            List<String> paragraphLines,
            List<BlockNode> blocks,
            List<MarkdownDiagnostic> diagnostics
    ) {
        if (paragraphLines.isEmpty()) {
            return;
        }

        blocks.add(new Paragraph(parseInline(String.join(" ", paragraphLines), diagnostics)));
        paragraphLines.clear();
    }

    private static Heading parseHeading(String line, List<MarkdownDiagnostic> diagnostics) {
        if (line.startsWith("\\#")) {
            return null;
        }
        if (!line.startsWith("#")) {
            return null;
        }

        var level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }

        if (level > 6) {
            diagnostics.add(new MarkdownDiagnostic(
                    MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX,
                    "ATX headings support at most six leading # characters."));
            return null;
        }
        if (level == line.length()) {
            diagnostics.add(new MarkdownDiagnostic(
                    MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX,
                    "ATX headings require whitespace after the heading marker."));
            return null;
        }
        if (!Character.isWhitespace(line.charAt(level))) {
            diagnostics.add(new MarkdownDiagnostic(
                    MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX,
                    "ATX headings require whitespace after the heading marker."));
            return null;
        }

        return new Heading(level, parseInline(line.substring(level).trim(), diagnostics));
    }

    private static TableParse parseTable(String[] lines, int lineIndex, List<MarkdownDiagnostic> diagnostics) {
        var headerCells = splitTableRow(lines[lineIndex]);
        if (headerCells == null || lineIndex + 1 >= lines.length) {
            return TableParse.none();
        }

        var separatorCells = splitTableRow(lines[lineIndex + 1]);
        if (separatorCells == null) {
            return TableParse.none();
        }

        if (!isSeparatorRow(separatorCells)) {
            if (looksLikeSeparatorRow(separatorCells)) {
                diagnostics.add(new MarkdownDiagnostic(
                        MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX,
                        "Markdown table separator cells must contain at least three hyphens and no alignment colons."));
                return TableParse.malformed(2);
            }
            return TableParse.none();
        }

        if (headerCells.isEmpty() || separatorCells.size() != headerCells.size()) {
            diagnostics.add(new MarkdownDiagnostic(
                    MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX,
                    "Markdown table header and separator rows must have matching non-empty column counts."));
            return TableParse.malformed(2);
        }

        var rows = new ArrayList<TableRow>();
        rows.add(tableRow(headerCells, diagnostics));
        var nextLineIndex = lineIndex + 2;
        while (nextLineIndex < lines.length) {
            var line = lines[nextLineIndex];
            if (line.isBlank()) {
                break;
            }
            var bodyCells = splitTableRow(line);
            if (bodyCells == null) {
                break;
            }
            if (bodyCells.size() != headerCells.size()) {
                diagnostics.add(new MarkdownDiagnostic(
                        MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX,
                        "Markdown table body rows must match the header column count."));
                return TableParse.malformed(consumeConsecutiveTableRows(lines, lineIndex));
            }
            rows.add(tableRow(bodyCells, diagnostics));
            nextLineIndex++;
        }

        return TableParse.success(new TableBlock(rows, 1), nextLineIndex);
    }

    private static TableRow tableRow(List<String> cellSources, List<MarkdownDiagnostic> diagnostics) {
        return new TableRow(cellSources.stream()
                .map(cell -> new TableCell(new TableCellContent(parseInline(cell, diagnostics))))
                .toList());
    }

    private static List<String> splitTableRow(String line) {
        var trimmed = line.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '|' || trimmed.charAt(trimmed.length() - 1) != '|') {
            return null;
        }

        var cells = new ArrayList<String>();
        var cell = new StringBuilder();
        for (var index = 1; index < trimmed.length() - 1; index++) {
            var current = trimmed.charAt(index);
            if (current == '|' && !isEscaped(trimmed, index)) {
                cells.add(cell.toString().trim());
                cell.setLength(0);
            } else {
                cell.append(current);
            }
        }
        cells.add(cell.toString().trim());
        return cells;
    }

    private static boolean isEscaped(String source, int index) {
        var backslashes = 0;
        for (var cursor = index - 1; cursor >= 0 && source.charAt(cursor) == '\\'; cursor--) {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }

    private static boolean isSeparatorRow(List<String> cells) {
        return !cells.isEmpty() && cells.stream().allMatch(MarkdownParser::isSeparatorCell);
    }

    private static boolean isSeparatorCell(String cell) {
        if (cell.length() < 3) {
            return false;
        }
        for (var index = 0; index < cell.length(); index++) {
            if (cell.charAt(index) != '-') {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeSeparatorRow(List<String> cells) {
        return !cells.isEmpty() && cells.stream().allMatch(cell -> !cell.isEmpty() && cell.chars()
                .allMatch(character -> character == '-' || character == ':'));
    }

    private static int consumeConsecutiveTableRows(String[] lines, int startLineIndex) {
        var count = 0;
        for (var index = startLineIndex; index < lines.length; index++) {
            if (lines[index].isBlank() || splitTableRow(lines[index]) == null) {
                break;
            }
            count++;
        }
        return count;
    }

    private static void detectUnsupportedBlockSyntax(String line, List<MarkdownDiagnostic> diagnostics) {
        var trimmed = line.trim();
        if (trimmed.startsWith("![") || trimmed.startsWith("[") && trimmed.contains("](")) {
            diagnostics.add(new MarkdownDiagnostic(
                    MarkdownDiagnosticKind.UNSUPPORTED_SYNTAX,
                    "Links and images are not supported in Scholar Markdown v0.1."));
        } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
            diagnostics.add(new MarkdownDiagnostic(
                    MarkdownDiagnosticKind.UNSUPPORTED_SYNTAX,
                    "Lists are not supported in Scholar Markdown v0.1."));
        } else if (trimmed.startsWith(">")) {
            diagnostics.add(new MarkdownDiagnostic(
                    MarkdownDiagnosticKind.UNSUPPORTED_SYNTAX,
                    "Blockquotes are not supported in Scholar Markdown v0.1."));
        } else if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
            diagnostics.add(new MarkdownDiagnostic(
                    MarkdownDiagnosticKind.UNSUPPORTED_SYNTAX,
                    "Code blocks are not supported in Scholar Markdown v0.1."));
        }
    }

    private static InlineContent parseInline(String source, List<MarkdownDiagnostic> diagnostics) {
        var nodes = new ArrayList<InlineNode>();
        var plain = new StringBuilder();

        for (var index = 0; index < source.length(); ) {
            var escaped = escapedCharacter(source, index);
            if (escaped != null) {
                plain.append(escaped);
                index += 2;
                continue;
            }

            var delimiter = delimiterAt(source, index);
            if (delimiter == null) {
                plain.append(source.charAt(index));
                index++;
                continue;
            }

            var closingIndex = findClosingDelimiter(source, index + delimiter.length(), delimiter);
            if (closingIndex < 0) {
                diagnostics.add(new MarkdownDiagnostic(
                        MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX,
                        "Unmatched emphasis delimiter."));
                plain.append(delimiter);
                index += delimiter.length();
                continue;
            }

            var rawContent = source.substring(index + delimiter.length(), closingIndex);
            if (rawContent.isEmpty() || containsUnescapedAsterisk(rawContent)) {
                diagnostics.add(new MarkdownDiagnostic(
                        MarkdownDiagnosticKind.MALFORMED_SUPPORTED_SYNTAX,
                        "Nested, overlapping, or empty emphasis is not supported in Scholar Markdown v0.1."));
                plain.append(source, index, closingIndex + delimiter.length());
                index = closingIndex + delimiter.length();
                continue;
            }

            appendText(nodes, plain.toString(), Set.of());
            plain.setLength(0);
            appendText(nodes, unescapeSupportedCharacters(rawContent), marksFor(delimiter));
            index = closingIndex + delimiter.length();
        }

        appendText(nodes, plain.toString(), Set.of());
        return new InlineContent(nodes);
    }

    private static Character escapedCharacter(String source, int index) {
        if (source.charAt(index) != '\\' || index + 1 >= source.length()) {
            return null;
        }

        var next = source.charAt(index + 1);
        if (next == '#' || next == '*' || next == '\\' || next == '|') {
            return next;
        }
        return null;
    }

    private static String delimiterAt(String source, int index) {
        if (source.startsWith("***", index)) {
            return "***";
        }
        if (source.startsWith("**", index)) {
            return "**";
        }
        if (source.charAt(index) == '*') {
            return "*";
        }
        return null;
    }

    private static int findClosingDelimiter(String source, int start, String delimiter) {
        for (var index = start; index <= source.length() - delimiter.length(); index++) {
            if (source.charAt(index) == '\\') {
                index++;
                continue;
            }
            if (source.startsWith(delimiter, index)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean containsUnescapedAsterisk(String source) {
        for (var index = 0; index < source.length(); index++) {
            if (source.charAt(index) == '\\') {
                index++;
            } else if (source.charAt(index) == '*') {
                return true;
            }
        }
        return false;
    }

    private static String unescapeSupportedCharacters(String source) {
        var unescaped = new StringBuilder();
        for (var index = 0; index < source.length(); index++) {
            var escaped = escapedCharacter(source, index);
            if (escaped != null) {
                unescaped.append(escaped);
                index++;
            } else {
                unescaped.append(source.charAt(index));
            }
        }
        return unescaped.toString();
    }

    private static Set<TextMark> marksFor(String delimiter) {
        return switch (delimiter) {
            case "***" -> Set.of(TextMark.BOLD, TextMark.ITALIC);
            case "**" -> Set.of(TextMark.BOLD);
            case "*" -> Set.of(TextMark.ITALIC);
            default -> throw new IllegalArgumentException("Unknown delimiter: " + delimiter);
        };
    }

    private static void appendText(List<InlineNode> nodes, String content, Set<TextMark> marks) {
        if (!content.isEmpty()) {
            nodes.add(new Text(content, marks));
        }
    }

    private record TableParse(TableBlock table, int nextLineIndex, int consumedMalformedLines) {
        static TableParse none() {
            return new TableParse(null, -1, 0);
        }

        static TableParse success(TableBlock table, int nextLineIndex) {
            return new TableParse(Objects.requireNonNull(table, "table"), nextLineIndex, 0);
        }

        static TableParse malformed(int consumedLines) {
            return new TableParse(null, -1, consumedLines);
        }
    }
}
