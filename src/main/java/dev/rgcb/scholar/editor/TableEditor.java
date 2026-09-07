package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class TableEditor {
    public void validateSelection(TableBlock table, TableCellTextSelection selection) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(selection, "selection");
        cellContent(table, selection.cell());
        var text = cellText(table, selection.cell());
        TextBoundary.validateOffset(text, selection.anchorOffset());
        TextBoundary.validateOffset(text, selection.activeOffset());
    }

    public TableCellTextSelection firstCellStart(TableBlock table) {
        Objects.requireNonNull(table, "table");
        return TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 0);
    }

    public boolean canDeleteRow(TableBlock table) {
        Objects.requireNonNull(table, "table");
        return table.rows().size() > 1;
    }

    public boolean canDeleteColumn(TableBlock table) {
        Objects.requireNonNull(table, "table");
        return table.columnCount() > 1;
    }

    public TableEditResult insertRowAbove(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        var rowIndex = selection.cell().rowIndex();
        var rows = new ArrayList<TableRow>(table.rows());
        rows.add(rowIndex, emptyRow(table.columnCount()));
        var target = TableCellTextSelection.caret(new TableCellCoordinate(rowIndex, selection.cell().columnIndex()), 0);
        return new TableEditResult(new TableBlock(rows, table.headerRowCount()), target, Optional.empty(), true);
    }

    public TableEditResult insertRowBelow(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        var rowIndex = selection.cell().rowIndex() + 1;
        var rows = new ArrayList<TableRow>(table.rows());
        rows.add(rowIndex, emptyRow(table.columnCount()));
        var target = TableCellTextSelection.caret(new TableCellCoordinate(rowIndex, selection.cell().columnIndex()), 0);
        return new TableEditResult(new TableBlock(rows, table.headerRowCount()), target, Optional.empty(), true);
    }

    public TableEditResult deleteRow(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        if (!canDeleteRow(table)) {
            return new TableEditResult(table, selection, Optional.empty(), false);
        }
        var oldRow = selection.cell().rowIndex();
        var rows = new ArrayList<TableRow>(table.rows());
        rows.remove(oldRow);
        var targetRow = Math.min(oldRow, rows.size() - 1);
        var targetColumn = Math.min(selection.cell().columnIndex(), table.columnCount() - 1);
        var target = TableCellTextSelection.caret(new TableCellCoordinate(targetRow, targetColumn), 0);
        return new TableEditResult(new TableBlock(rows, table.headerRowCount()), target, Optional.empty(), true);
    }

    public TableEditResult insertColumnLeft(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        return insertColumn(table, selection, selection.cell().columnIndex());
    }

    public TableEditResult insertColumnRight(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        return insertColumn(table, selection, selection.cell().columnIndex() + 1);
    }

    public TableEditResult deleteColumn(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        if (!canDeleteColumn(table)) {
            return new TableEditResult(table, selection, Optional.empty(), false);
        }
        var oldColumn = selection.cell().columnIndex();
        var rows = new ArrayList<TableRow>();
        for (var row : table.rows()) {
            var cells = new ArrayList<TableCell>(row.cells());
            cells.remove(oldColumn);
            rows.add(new TableRow(cells));
        }
        var targetColumn = Math.min(oldColumn, table.columnCount() - 2);
        var target = TableCellTextSelection.caret(new TableCellCoordinate(selection.cell().rowIndex(), targetColumn), 0);
        return new TableEditResult(new TableBlock(rows, table.headerRowCount()), target, Optional.empty(), true);
    }

    public TableEditResult insertText(TableBlock table, TableCellTextSelection selection, String text, Set<TextMark> marks) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(marks, "marks");
        validateSelection(table, selection);
        if (text.isEmpty()) {
            return new TableEditResult(table, selection, Optional.empty(), false);
        }
        var replacementMarks = Set.copyOf(marks);
        var updated = replaceSelectedText(table, selection, text, replacementMarks);
        var caret = selection.startOffset() + TextBoundary.characterCount(text);
        return new TableEditResult(updated, TableCellTextSelection.caret(selection.cell(), caret), Optional.empty(), !updated.equals(table));
    }

    public TableEditResult deleteBackward(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        if (!selection.isCaret()) {
            return deleteSelection(table, selection);
        }
        if (selection.activeOffset() == 0) {
            return new TableEditResult(table, selection, Optional.empty(), false);
        }
        var text = cellText(table, selection.cell());
        var start = TextBoundary.previousOffset(text, selection.activeOffset());
        return deleteSelection(table, new TableCellTextSelection(selection.cell(), start, selection.activeOffset()));
    }

    public TableEditResult deleteForward(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        if (!selection.isCaret()) {
            return deleteSelection(table, selection);
        }
        var text = cellText(table, selection.cell());
        if (selection.activeOffset() == TextBoundary.characterCount(text)) {
            return new TableEditResult(table, selection, Optional.empty(), false);
        }
        var end = TextBoundary.nextOffset(text, selection.activeOffset());
        return deleteSelection(table, new TableCellTextSelection(selection.cell(), selection.activeOffset(), end));
    }

    public TableCellTextSelection moveLeft(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        if (!selection.isCaret()) {
            return TableCellTextSelection.caret(selection.cell(), selection.startOffset());
        }
        var text = cellText(table, selection.cell());
        return TableCellTextSelection.caret(selection.cell(), TextBoundary.previousOffset(text, selection.activeOffset()));
    }

    public TableCellTextSelection moveRight(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        if (!selection.isCaret()) {
            return TableCellTextSelection.caret(selection.cell(), selection.endOffset());
        }
        var text = cellText(table, selection.cell());
        return TableCellTextSelection.caret(selection.cell(), TextBoundary.nextOffset(text, selection.activeOffset()));
    }

    public TableCellTextSelection extendLeft(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        var text = cellText(table, selection.cell());
        return new TableCellTextSelection(selection.cell(), selection.anchorOffset(), TextBoundary.previousOffset(text, selection.activeOffset()));
    }

    public TableCellTextSelection extendRight(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        var text = cellText(table, selection.cell());
        return new TableCellTextSelection(selection.cell(), selection.anchorOffset(), TextBoundary.nextOffset(text, selection.activeOffset()));
    }

    public TableCellTextSelection moveNextCell(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        var coordinate = selection.cell();
        if (coordinate.rowIndex() == table.rows().size() - 1 && coordinate.columnIndex() == table.columnCount() - 1) {
            return TableCellTextSelection.caret(coordinate, 0);
        }
        var nextColumn = coordinate.columnIndex() + 1;
        var nextRow = coordinate.rowIndex();
        if (nextColumn >= table.columnCount()) {
            nextColumn = 0;
            nextRow++;
        }
        return TableCellTextSelection.caret(new TableCellCoordinate(nextRow, nextColumn), 0);
    }

    public TableCellTextSelection movePreviousCell(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        var coordinate = selection.cell();
        if (coordinate.rowIndex() == 0 && coordinate.columnIndex() == 0) {
            return TableCellTextSelection.caret(coordinate, 0);
        }
        var previousColumn = coordinate.columnIndex() - 1;
        var previousRow = coordinate.rowIndex();
        if (previousColumn < 0) {
            previousRow--;
            previousColumn = table.columnCount() - 1;
        }
        return TableCellTextSelection.caret(new TableCellCoordinate(previousRow, previousColumn), 0);
    }

    public FormattingState formattingState(TableBlock table, TableCellTextSelection selection, TextMark mark) {
        Objects.requireNonNull(mark, "mark");
        validateSelection(table, selection);
        if (selection.isCaret()) {
            return marksForInsertion(table, selection.cell(), selection.activeOffset()).contains(mark)
                    ? FormattingState.ON
                    : FormattingState.OFF;
        }
        var marked = 0;
        var unmarked = 0;
        var logicalOffset = 0;
        for (var node : cellContent(table, selection.cell()).nodes()) {
            var text = (Text) node;
            var nodeLength = TextBoundary.characterCount(text.content());
            var selectedStart = Math.max(selection.startOffset(), logicalOffset);
            var selectedEnd = Math.min(selection.endOffset(), logicalOffset + nodeLength);
            if (selectedStart < selectedEnd) {
                var selectedLength = selectedEnd - selectedStart;
                if (text.marks().contains(mark)) {
                    marked += selectedLength;
                } else {
                    unmarked += selectedLength;
                }
            }
            logicalOffset += nodeLength;
        }
        if (marked > 0 && unmarked > 0) {
            return FormattingState.MIXED;
        }
        if (marked == 0 && unmarked == 0) {
            return FormattingState.NOT_APPLICABLE;
        }
        return marked > 0 ? FormattingState.ON : FormattingState.OFF;
    }

    public TableEditResult toggleMark(TableBlock table, TableCellTextSelection selection, TextMark mark) {
        validateSelection(table, selection);
        if (selection.isCaret()) {
            return new TableEditResult(table, selection, Optional.empty(), false);
        }
        var state = formattingState(table, selection, mark);
        if (state == FormattingState.NOT_APPLICABLE) {
            return new TableEditResult(table, selection, Optional.empty(), false);
        }
        return transformMark(table, selection, mark, state != FormattingState.ON);
    }

    public Optional<String> copy(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        if (selection.isCaret()) {
            return Optional.empty();
        }
        return Optional.of(TextBoundary.substring(cellText(table, selection.cell()), selection.startOffset(), selection.endOffset()));
    }

    public Optional<ClipboardEditResult> cut(DocumentSource documentSource, TableCellTextSelection selection) {
        Objects.requireNonNull(documentSource, "documentSource");
        var copied = copy(documentSource.table(), selection);
        if (copied.isEmpty()) {
            return Optional.empty();
        }
        var result = deleteSelection(documentSource.table(), selection);
        return Optional.of(new ClipboardEditResult(copied.orElseThrow(), documentSource.toEditResult(result)));
    }

    public Optional<TableEditResult> paste(TableBlock table, TableCellTextSelection selection, String clipboardText) {
        Objects.requireNonNull(clipboardText, "clipboardText");
        validateSelection(table, selection);
        if (clipboardText.isEmpty()) {
            return Optional.empty();
        }
        var normalized = clipboardText
                .replace("\r\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ');
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(insertText(table, selection, normalized, marksForReplacement(table, selection)));
    }

    public Set<TextMark> marksForInsertion(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        return marksForInsertion(table, selection.cell(), selection.activeOffset());
    }

    public Set<TextMark> marksForReplacement(TableBlock table, TableCellTextSelection selection) {
        validateSelection(table, selection);
        return selection.isCaret()
                ? marksForInsertion(table, selection)
                : marksForInsertion(table, selection.cell(), selection.startOffset());
    }

    public InlineContent cellContent(TableBlock table, TableCellCoordinate coordinate) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(coordinate, "coordinate");
        if (coordinate.rowIndex() >= table.rows().size()) {
            throw new IllegalArgumentException("rowIndex is outside the table.");
        }
        if (coordinate.columnIndex() >= table.columnCount()) {
            throw new IllegalArgumentException("columnIndex is outside the table.");
        }
        return table.rows().get(coordinate.rowIndex()).cells().get(coordinate.columnIndex()).content().content();
    }

    public String cellText(TableBlock table, TableCellCoordinate coordinate) {
        var text = new StringBuilder();
        for (var node : cellContent(table, coordinate).nodes()) {
            if (!(node instanceof Text textNode)) {
                throw new IllegalArgumentException("Only Text inline nodes are editable in table cells: " + node.getClass().getName());
            }
            text.append(textNode.content());
        }
        return text.toString();
    }

    private TableEditResult deleteSelection(TableBlock table, TableCellTextSelection selection) {
        var updated = replaceSelectedText(table, selection, "", Set.of());
        return new TableEditResult(updated, TableCellTextSelection.caret(selection.cell(), selection.startOffset()), Optional.empty(), !updated.equals(table));
    }

    private TableEditResult transformMark(TableBlock table, TableCellTextSelection selection, TextMark mark, boolean add) {
        var updatedContent = new InlineContent(transformInlineMark(cellContent(table, selection.cell()), selection.startOffset(), selection.endOffset(), mark, add));
        var updated = replaceCellContent(table, selection.cell(), updatedContent);
        return new TableEditResult(updated, selection, Optional.empty(), !updated.equals(table));
    }

    private TableBlock replaceSelectedText(TableBlock table, TableCellTextSelection selection, String replacement, Set<TextMark> replacementMarks) {
        var content = cellContent(table, selection.cell());
        var beforeSelection = InlineContentEditor.split(content, selection.startOffset());
        var selectedAndAfter = InlineContentEditor.split(beforeSelection.right(), selection.endOffset() - selection.startOffset());
        var updatedNodes = new ArrayList<InlineNode>(beforeSelection.left().nodes());
        addReplacement(updatedNodes, replacement, replacementMarks);
        updatedNodes.addAll(selectedAndAfter.right().nodes());
        return replaceCellContent(table, selection.cell(), new InlineContent(updatedNodes));
    }

    private static List<InlineNode> transformInlineMark(InlineContent content, int startOffset, int endOffset, TextMark mark, boolean add) {
        var updatedNodes = new ArrayList<InlineNode>();
        var logicalOffset = 0;
        for (var node : content.nodes()) {
            var text = (Text) node;
            var nodeLength = TextBoundary.characterCount(text.content());
            var nodeStart = logicalOffset;
            var nodeEnd = nodeStart + nodeLength;
            var selectedStart = Math.max(startOffset, nodeStart);
            var selectedEnd = Math.min(endOffset, nodeEnd);
            if (selectedStart >= selectedEnd) {
                updatedNodes.add(text);
            } else {
                addTextSegment(updatedNodes, text, 0, selectedStart - nodeStart, text.marks());
                addTextSegment(updatedNodes, text, selectedStart - nodeStart, selectedEnd - nodeStart, transformedMarks(text.marks(), mark, add));
                addTextSegment(updatedNodes, text, selectedEnd - nodeStart, nodeLength, text.marks());
            }
            logicalOffset = nodeEnd;
        }
        return List.copyOf(updatedNodes);
    }

    private static void addReplacement(List<InlineNode> nodes, String replacement, Set<TextMark> replacementMarks) {
        if (!replacement.isEmpty()) {
            nodes.add(new Text(replacement, replacementMarks));
        }
    }

    private static void addTextSegment(List<InlineNode> nodes, Text source, int startOffset, int endOffset, Set<TextMark> marks) {
        if (startOffset >= endOffset) {
            return;
        }
        nodes.add(new Text(TextBoundary.substring(source.content(), startOffset, endOffset), marks));
    }

    private static Set<TextMark> transformedMarks(Set<TextMark> source, TextMark mark, boolean add) {
        var marks = source.isEmpty() ? EnumSet.noneOf(TextMark.class) : EnumSet.copyOf(source);
        if (add) {
            marks.add(mark);
        } else {
            marks.remove(mark);
        }
        return Set.copyOf(marks);
    }

    private Set<TextMark> marksForInsertion(TableBlock table, TableCellCoordinate coordinate, int characterOffset) {
        var content = cellContent(table, coordinate);
        TextBoundary.validateOffset(cellText(table, coordinate), characterOffset);
        var logicalOffset = 0;
        Text previous = null;
        Text following = null;
        for (var node : content.nodes()) {
            var text = (Text) node;
            var nodeStart = logicalOffset;
            var nodeEnd = nodeStart + TextBoundary.characterCount(text.content());
            if (characterOffset > nodeStart && characterOffset <= nodeEnd) {
                return text.marks();
            }
            if (characterOffset == nodeStart) {
                following = text;
                break;
            }
            previous = text;
            logicalOffset = nodeEnd;
        }
        if (previous != null) {
            return previous.marks();
        }
        if (following != null) {
            return following.marks();
        }
        return Set.of();
    }

    private static TableBlock replaceCellContent(TableBlock table, TableCellCoordinate coordinate, InlineContent content) {
        var rows = new ArrayList<TableRow>(table.rows());
        var sourceRow = rows.get(coordinate.rowIndex());
        var cells = new ArrayList<TableCell>(sourceRow.cells());
        cells.set(coordinate.columnIndex(), new TableCell(new TableCellContent(content)));
        rows.set(coordinate.rowIndex(), new TableRow(cells));
        return new TableBlock(rows, table.headerRowCount());
    }

    private static TableEditResult insertColumn(TableBlock table, TableCellTextSelection selection, int columnIndex) {
        var rows = new ArrayList<TableRow>();
        for (var row : table.rows()) {
            var cells = new ArrayList<TableCell>(row.cells());
            cells.add(columnIndex, TableCell.empty());
            rows.add(new TableRow(cells));
        }
        var target = TableCellTextSelection.caret(new TableCellCoordinate(selection.cell().rowIndex(), columnIndex), 0);
        return new TableEditResult(new TableBlock(rows, table.headerRowCount()), target, Optional.empty(), true);
    }

    private static TableRow emptyRow(int columnCount) {
        var cells = new ArrayList<TableCell>();
        for (var column = 0; column < columnCount; column++) {
            cells.add(TableCell.empty());
        }
        return new TableRow(cells);
    }

    public record DocumentSource(dev.rgcb.scholar.document.Document document, int blockIndex, TableBlock table) {
        public DocumentSource {
            document = Objects.requireNonNull(document, "document");
            table = Objects.requireNonNull(table, "table");
        }

        public EditResult toEditResult(TableEditResult tableResult) {
            var blocks = new ArrayList<dev.rgcb.scholar.document.BlockNode>(document.blocks());
            blocks.set(blockIndex, tableResult.table());
            return new EditResult(
                    new dev.rgcb.scholar.document.Document(blocks),
                    new TableEditingSelection(blockIndex, tableResult.selection()),
                    tableResult.explicitTypingMarks(),
                    tableResult.changed());
        }
    }
}
