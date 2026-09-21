package dev.rgcb.scholar.layout;

import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.editor.TextBoundary;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.text.BreakIterator;
import java.util.Locale;

public final class TableLayoutEngine {
    public static final int BORDER_THICKNESS = 1;
    public static final int CELL_PADDING_X = 6;
    public static final int CELL_PADDING_Y = 4;

    /**
     * Returns the narrowest table width that can keep every whitespace-delimited
     * token intact at the authored typography size.
     */
    public int minimumReadableWidth(TableBlock table, TextMeasurer textMeasurer) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        var columnMinimums = new int[table.columnCount()];
        for (var rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
            var header = rowIndex < table.headerRowCount();
            for (var columnIndex = 0; columnIndex < table.columnCount(); columnIndex++) {
                var minimum = CELL_PADDING_X * 2 + 1;
                for (var node : table.rows().get(rowIndex).cells().get(columnIndex).content().content().nodes()) {
                    if (!(node instanceof Text text)) {
                        if (node instanceof dev.rgcb.scholar.document.QuantityInline quantity) {
                            var style = TextStyle.paragraph(header ? Set.of(TextMark.BOLD) : Set.of());
                            minimum = Math.max(minimum, CELL_PADDING_X * 2 + textMeasurer.measureWidth(
                                    new dev.rgcb.scholar.quantity.ScientificNumberFormatter()
                                            .format(quantity.value(), quantity.notation(), true), style));
                        }
                        continue;
                    }
                    var style = TextStyle.paragraph(header ? headerMarks(text.marks()) : text.marks())
                            .withFormat(text.format());
                    for (var token : text.content().split("\\s+")) {
                        if (!token.isEmpty()) {
                            minimum = Math.max(minimum,
                                    CELL_PADDING_X * 2 + textMeasurer.measureWidth(token, style));
                        }
                    }
                }
                columnMinimums[columnIndex] = Math.max(columnMinimums[columnIndex], minimum);
            }
        }
        return java.util.Arrays.stream(columnMinimums).sum();
    }

    public LaidOutTable layout(TableBlock table, int sourceBlockIndex, int x, int y, int width, TextMeasurer textMeasurer) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(textMeasurer, "textMeasurer");
        if (sourceBlockIndex < 0) {
            throw new IllegalArgumentException("sourceBlockIndex must not be negative.");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive.");
        }

        var columnWidths = columnWidths(width, table.columnCount());
        var rows = new ArrayList<LaidOutTableRow>();
        var currentY = y;
        for (var rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
            var row = table.rows().get(rowIndex);
            var isHeader = rowIndex < table.headerRowCount();
            var measuredCells = new ArrayList<MeasuredCell>();
            var rowHeight = 0;
            var currentX = x;
            for (var columnIndex = 0; columnIndex < row.cells().size(); columnIndex++) {
                var columnWidth = columnWidths.get(columnIndex);
                var contentX = currentX + CELL_PADDING_X;
                var contentY = currentY + CELL_PADDING_Y;
                var contentWidth = Math.max(1, columnWidth - CELL_PADDING_X * 2);
                var lines = layoutCellContent(
                        row.cells().get(columnIndex),
                        sourceBlockIndex,
                        contentX,
                        contentY,
                        contentWidth,
                        isHeader,
                        textMeasurer);
                var contentHeight = linesHeight(lines, contentY);
                var cellHeight = contentHeight + CELL_PADDING_Y * 2;
                rowHeight = Math.max(rowHeight, cellHeight);
                measuredCells.add(new MeasuredCell(columnIndex, currentX, columnWidth, contentX, contentY, contentWidth, contentHeight, lines));
                currentX += columnWidth;
            }

            var laidOutCells = new ArrayList<LaidOutTableCell>();
            for (var cell : measuredCells) {
                laidOutCells.add(new LaidOutTableCell(
                        rowIndex,
                        cell.columnIndex(),
                        cell.x(),
                        currentY,
                        cell.width(),
                        rowHeight,
                        cell.contentX(),
                        cell.contentY(),
                        cell.contentWidth(),
                        cell.contentHeight(),
                        cell.lines()));
            }
            rows.add(new LaidOutTableRow(rowIndex, x, currentY, width, rowHeight, laidOutCells));
            currentY += rowHeight;
        }

        return new LaidOutTable(x, y, width, currentY - y, table.headerRowCount(), rows);
    }

    private static List<Integer> columnWidths(int width, int columnCount) {
        var base = width / columnCount;
        var remainder = width % columnCount;
        var widths = new ArrayList<Integer>();
        for (var column = 0; column < columnCount; column++) {
            widths.add(base + (column < remainder ? 1 : 0));
        }
        return List.copyOf(widths);
    }

    private static List<LaidOutLine> layoutCellContent(
            dev.rgcb.scholar.document.TableCell cell,
            int sourceBlockIndex,
            int x,
            int y,
            int width,
            boolean header,
            TextMeasurer textMeasurer
    ) {
        var builder = new LineBuilder(width, x, y, textMeasurer);
        var sourceOffset = 0;
        for (InlineNode node : cell.content().content().nodes()) {
            if (node instanceof Text text) {
                var style = TextStyle.paragraph(header ? headerMarks(text.marks()) : text.marks())
                        .withFormat(text.format());
                builder.append(text.content(), style, sourceBlockIndex, sourceOffset);
                sourceOffset += TextBoundary.characterCount(text.content());
            } else if (node instanceof dev.rgcb.scholar.document.QuantityInline quantity) {
                var style = TextStyle.paragraph(header ? Set.of(TextMark.BOLD) : Set.of());
                builder.append(new dev.rgcb.scholar.quantity.ScientificNumberFormatter()
                        .format(quantity.value(), quantity.notation(), true), style, sourceBlockIndex, sourceOffset);
                sourceOffset += 1;
            } else {
                throw new IllegalArgumentException("Unsupported inline node in table cell: " + node.getClass().getName());
            }
        }
        var lines = builder.finish();
        if (lines.isEmpty()) {
            var emptyHeight = textMeasurer.lineHeight(TextStyle.paragraph(header ? Set.of(TextMark.BOLD) : Set.of()));
            return List.of(new LaidOutLine(x, y, 0, emptyHeight, List.of()));
        }
        return lines;
    }

    private static Set<TextMark> headerMarks(Set<TextMark> marks) {
        var updated = marks.isEmpty() ? EnumSet.noneOf(TextMark.class) : EnumSet.copyOf(marks);
        updated.add(TextMark.BOLD);
        return Set.copyOf(updated);
    }

    private static int linesHeight(List<LaidOutLine> lines, int y) {
        var last = lines.get(lines.size() - 1);
        return last.y() + last.height() - y;
    }

    private record MeasuredCell(
            int columnIndex,
            int x,
            int width,
            int contentX,
            int contentY,
            int contentWidth,
            int contentHeight,
            List<LaidOutLine> lines
    ) {
    }

    private static final class LineBuilder {
        private final int contentWidth;
        private final int originX;
        private final TextMeasurer textMeasurer;
        private final List<LaidOutLine> lines = new ArrayList<>();
        private final List<LaidOutText> currentRuns = new ArrayList<>();
        private int y;
        private int x;
        private int lineHeight;
        private boolean pendingSpace;
        private int pendingSpaceBlockIndex;
        private int pendingSpaceSourceStart;
        private TextStyle pendingSpaceStyle;

        private LineBuilder(int contentWidth, int originX, int y, TextMeasurer textMeasurer) {
            this.contentWidth = contentWidth;
            this.originX = originX;
            this.y = y;
            this.textMeasurer = textMeasurer;
        }

        private void append(String text, TextStyle style, int sourceBlockIndex, int sourceStart) {
            for (var token : tokenize(text)) {
                if (token.text().isBlank()) {
                    pendingSpace = true;
                    pendingSpaceBlockIndex = sourceBlockIndex;
                    pendingSpaceSourceStart = sourceStart + token.sourceStart();
                    pendingSpaceStyle = style;
                    continue;
                }
                appendWord(
                        token.text(),
                        style,
                        sourceBlockIndex,
                        sourceStart + token.sourceStart(),
                        pendingSpace ? sourceStart + token.sourceStart() - 1 : sourceStart + token.sourceStart());
            }
        }

        private void appendWord(String word, TextStyle style, int sourceBlockIndex, int wordSourceStart, int sourceStartWithPendingSpace) {
            var prefix = pendingSpace && !currentRuns.isEmpty() ? " " : "";
            pendingSpace = false;
            var token = prefix + word;
            if (fits(token, style) || currentRuns.isEmpty()) {
                appendFittingOrSplit(token, style, sourceBlockIndex, prefix.isEmpty() ? wordSourceStart : sourceStartWithPendingSpace);
                return;
            }

            finishLine();
            appendFittingOrSplit(word, style, sourceBlockIndex, wordSourceStart);
        }

        private void appendFittingOrSplit(String text, TextStyle style, int sourceBlockIndex, int sourceStart) {
            var remaining = text;
            var remainingSourceStart = sourceStart;
            while (!remaining.isEmpty()) {
                if (!currentRuns.isEmpty() && !fits(remaining, style)) {
                    finishLine();
                }

                var fittingLength = fittingLength(remaining, style);
                var piece = remaining.substring(0, fittingLength);
                var pieceCharacters = TextBoundary.characterCount(piece);
                appendPiece(piece, style, sourceBlockIndex, remainingSourceStart, remainingSourceStart + pieceCharacters);
                remaining = remaining.substring(fittingLength);
                remainingSourceStart += pieceCharacters;
                if (!remaining.isEmpty()) {
                    finishLine();
                }
            }
        }

        private boolean fits(String text, TextStyle style) {
            return x + textMeasurer.measureWidth(text, style) <= contentWidth;
        }

        private int fittingLength(String text, TextStyle style) {
            if (fits(text, style)) {
                return text.length();
            }

            var iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
            iterator.setText(text);
            iterator.first();
            var length = iterator.next();
            for (var next = iterator.next(); next != BreakIterator.DONE; next = iterator.next()) {
                if (x + textMeasurer.measureWidth(text.substring(0, next), style) > contentWidth) {
                    break;
                }
                length = next;
            }
            return length;
        }

        private void appendPiece(String text, TextStyle style, int sourceBlockIndex, int sourceStart, int sourceEnd) {
            var width = textMeasurer.measureWidth(text, style);
            var height = textMeasurer.lineHeight(style);
            currentRuns.add(new LaidOutText(text, style, originX + x, y, width, sourceBlockIndex, sourceStart, sourceEnd));
            x += width;
            lineHeight = Math.max(lineHeight, height);
        }

        private void finishLine() {
            appendPendingSpace();
            if (currentRuns.isEmpty()) {
                return;
            }
            lines.add(new LaidOutLine(originX, y, x, lineHeight, currentRuns));
            currentRuns.clear();
            y += lineHeight;
            x = 0;
            lineHeight = 0;
            pendingSpace = false;
        }

        private List<LaidOutLine> finish() {
            finishLine();
            return List.copyOf(lines);
        }

        private void appendPendingSpace() {
            if (!pendingSpace || pendingSpaceStyle == null) {
                return;
            }
            if (!fits(" ", pendingSpaceStyle) && !currentRuns.isEmpty()) {
                lines.add(new LaidOutLine(originX, y, x, lineHeight, currentRuns));
                currentRuns.clear();
                y += lineHeight;
                x = 0;
                lineHeight = 0;
            }
            appendPiece(" ", pendingSpaceStyle, pendingSpaceBlockIndex, pendingSpaceSourceStart, pendingSpaceSourceStart + 1);
            pendingSpace = false;
            pendingSpaceStyle = null;
        }

        private static List<SourceToken> tokenize(String text) {
            var tokens = new ArrayList<SourceToken>();
            var current = new StringBuilder();
            var whitespace = false;
            var tokenStart = 0;
            var sourceOffset = 0;
            var iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
            iterator.setText(text);
            var index = iterator.first();
            for (var next = iterator.next(); next != BreakIterator.DONE; next = iterator.next()) {
                var characterWhitespace = Character.isWhitespace(text.codePointAt(index));
                if (!current.isEmpty() && characterWhitespace != whitespace) {
                    tokens.add(new SourceToken(current.toString(), tokenStart, sourceOffset));
                    current.setLength(0);
                    tokenStart = sourceOffset;
                }
                current.append(text, index, next);
                whitespace = characterWhitespace;
                sourceOffset++;
                index = next;
            }
            if (!current.isEmpty()) {
                tokens.add(new SourceToken(current.toString(), tokenStart, sourceOffset));
            }
            return tokens;
        }

        private record SourceToken(String text, int sourceStart, int sourceEnd) {
        }
    }
}
