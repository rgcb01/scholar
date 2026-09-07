package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TableEditorTest {
    private final TableEditor editor = new TableEditor();

    @Test
    void typingIntoEmptyMiddleAndEndCellsUpdatesOnlyThatCell() {
        var empty = table(cell("abc"), TableCell.empty());
        var typed = editor.insertText(empty, caret(0, 1, 0), "Ω", Set.of());
        var middle = editor.insertText(typed.table(), caret(0, 0, 1), "X", Set.of(TextMark.BOLD));
        var end = editor.insertText(middle.table(), caret(0, 0, 4), "!", Set.of());

        assertEquals("Ω", cellText(typed.table(), 0, 1));
        assertEquals("aXbc!", cellText(end.table(), 0, 0));
        assertEquals(Set.of(TextMark.BOLD), ((Text) cell(end.table(), 0, 0).content().content().nodes().get(1)).marks());
        assertEquals(caret(0, 0, 5), end.selection());
        assertEquals("Ω", cellText(end.table(), 0, 1));
    }

    @Test
    void deleteAndBackspaceStayInsideCellBoundaries() {
        var table = table(cell("abc"), cell("def"));
        var backspace = editor.deleteBackward(table, caret(0, 0, 2));
        var delete = editor.deleteForward(backspace.table(), caret(0, 0, 1));
        var startClamp = editor.deleteBackward(delete.table(), caret(0, 0, 0));
        var endClamp = editor.deleteForward(delete.table(), caret(0, 0, 1));

        assertEquals("ac", cellText(backspace.table(), 0, 0));
        assertEquals("a", cellText(delete.table(), 0, 0));
        assertFalse(startClamp.changed());
        assertFalse(endClamp.changed());
        assertEquals("def", cellText(delete.table(), 0, 1));
    }

    @Test
    void selectedRangeReplacementPreservesDirectionPolicyByCollapsingToStart() {
        var table = table(cell("abcd"), cell("ef"));
        var result = editor.insertText(table, new TableCellTextSelection(new TableCellCoordinate(0, 0), 3, 1), "X", Set.of());

        assertEquals("aXd", cellText(result.table(), 0, 0));
        assertEquals(caret(0, 0, 2), result.selection());
    }

    @Test
    void boldItalicQueriesAndTogglesInspectAuthoredMarksOnly() {
        var table = table(
                cell(new Text("ab", Set.of(TextMark.BOLD)), new Text("cd", Set.of())),
                cell("body"));
        var mixed = new TableCellTextSelection(new TableCellCoordinate(0, 0), 0, 4);
        var bolded = editor.toggleMark(table, mixed, TextMark.BOLD);
        var removed = editor.toggleMark(bolded.table(), mixed, TextMark.BOLD);
        var italic = editor.toggleMark(table, new TableCellTextSelection(new TableCellCoordinate(0, 0), 1, 3), TextMark.ITALIC);

        assertEquals(FormattingState.MIXED, editor.formattingState(table, mixed, TextMark.BOLD));
        assertEquals(FormattingState.ON, editor.formattingState(bolded.table(), mixed, TextMark.BOLD));
        assertEquals(FormattingState.OFF, editor.formattingState(removed.table(), mixed, TextMark.BOLD));
        assertEquals(Set.of(TextMark.BOLD, TextMark.ITALIC), ((Text) cell(italic.table(), 0, 0).content().content().nodes().get(1)).marks());
    }

    @Test
    void moveAndExtendClampWithinCell() {
        var table = table(cell("abc"), cell("def"));

        assertEquals(caret(0, 0, 0), editor.moveLeft(table, caret(0, 0, 0)));
        assertEquals(caret(0, 0, 3), editor.moveRight(table, caret(0, 0, 3)));
        assertEquals(new TableCellTextSelection(new TableCellCoordinate(0, 0), 2, 1), editor.extendLeft(table, caret(0, 0, 2)));
        assertEquals(new TableCellTextSelection(new TableCellCoordinate(0, 0), 2, 3), editor.extendRight(table, caret(0, 0, 2)));
    }

    @Test
    void tabMovesCellsRowMajorAndClampsAtEdges() {
        var table = new TableBlock(List.of(row(cell("a"), cell("b")), row(cell("c"), cell("d"))), 0);

        assertEquals(caret(0, 1, 0), editor.moveNextCell(table, caret(0, 0, 1)));
        assertEquals(caret(1, 0, 0), editor.moveNextCell(table, caret(0, 1, 1)));
        assertEquals(caret(1, 1, 0), editor.moveNextCell(table, caret(1, 0, 1)));
        assertEquals(caret(1, 1, 0), editor.moveNextCell(table, caret(1, 1, 1)));
        assertEquals(caret(0, 0, 0), editor.movePreviousCell(table, caret(0, 0, 1)));
        assertEquals(caret(0, 1, 0), editor.movePreviousCell(table, caret(1, 0, 1)));
    }

    @Test
    void copyCutAndPasteAreCellLocalPlainTextOperations() {
        var table = table(cell("abcd"), cell("ef"));
        var selection = new TableCellTextSelection(new TableCellCoordinate(0, 0), 1, 3);
        var paste = editor.paste(table, caret(0, 1, 1), "x\ny").orElseThrow();

        assertEquals("bc", editor.copy(table, selection).orElseThrow());
        assertEquals("ex yf", cellText(paste.table(), 0, 1));
    }

    @Test
    void insertRowsAboveFirstBodyBelowHeaderAndBelowFinalPreserveRectangularTable() {
        var table = new TableBlock(List.of(row(cell("H1"), cell("H2")), row(cell("A"), cell("B"))), 1);
        var aboveHeader = editor.insertRowAbove(table, caret(0, 1, 2));
        var aboveBody = editor.insertRowAbove(table, caret(1, 0, 1));
        var belowHeader = editor.insertRowBelow(table, caret(0, 0, 2));
        var belowFinal = editor.insertRowBelow(table, caret(1, 1, 1));

        assertRectangular(aboveHeader.table(), 3, 2);
        assertEquals(1, aboveHeader.table().headerRowCount());
        assertEquals("", cellText(aboveHeader.table(), 0, 1));
        assertEquals("H1", cellText(aboveHeader.table(), 1, 0));
        assertEquals(caret(0, 1, 0), aboveHeader.selection());

        assertEquals("A", cellText(aboveBody.table(), 2, 0));
        assertEquals(caret(1, 0, 0), aboveBody.selection());
        assertEquals("", cellText(belowHeader.table(), 1, 0));
        assertEquals(caret(1, 0, 0), belowHeader.selection());
        assertEquals("", cellText(belowFinal.table(), 2, 1));
        assertEquals(caret(2, 1, 0), belowFinal.selection());
    }

    @Test
    void deleteRowsHeaderMiddleFinalAndRejectOnlyRow() {
        var table = new TableBlock(List.of(row(cell("H"), cell("h")), row(cell("A"), cell("a")), row(cell("B"), cell("b"))), 1);
        var deleteHeader = editor.deleteRow(table, caret(0, 1, 1));
        var deleteMiddle = editor.deleteRow(table, caret(1, 0, 1));
        var deleteFinal = editor.deleteRow(table, caret(2, 1, 1));
        var onlyRow = table(cell("only"));
        var rejected = editor.deleteRow(onlyRow, caret(0, 0, 0));

        assertRectangular(deleteHeader.table(), 2, 2);
        assertEquals(1, deleteHeader.table().headerRowCount());
        assertEquals("A", cellText(deleteHeader.table(), 0, 0));
        assertEquals(caret(0, 1, 0), deleteHeader.selection());
        assertEquals("B", cellText(deleteMiddle.table(), 1, 0));
        assertEquals(caret(1, 0, 0), deleteMiddle.selection());
        assertEquals("A", cellText(deleteFinal.table(), 1, 0));
        assertEquals(caret(1, 1, 0), deleteFinal.selection());
        assertFalse(rejected.changed());
        assertEquals(onlyRow, rejected.table());
    }

    @Test
    void insertColumnsLeftFirstMiddleAndRightFinalPreserveContentsMarksUnicodeAndHeaders() {
        var table = new TableBlock(List.of(
                row(cell("H"), cell("θ")),
                row(cell(new Text("café", Set.of(TextMark.BOLD))), cell("e\u0301 Δx λ"))), 1);
        var leftFirst = editor.insertColumnLeft(table, caret(1, 0, 4));
        var leftMiddle = editor.insertColumnLeft(table, caret(1, 1, 6));
        var rightFinal = editor.insertColumnRight(table, caret(0, 1, 1));

        assertRectangular(leftFirst.table(), 2, 3);
        assertEquals(1, leftFirst.table().headerRowCount());
        assertEquals("", cellText(leftFirst.table(), 1, 0));
        assertEquals("café", cellText(leftFirst.table(), 1, 1));
        assertEquals(Set.of(TextMark.BOLD), ((Text) cell(leftFirst.table(), 1, 1).content().content().nodes().get(0)).marks());
        assertEquals(caret(1, 0, 0), leftFirst.selection());

        assertEquals("café", cellText(leftMiddle.table(), 1, 0));
        assertEquals("", cellText(leftMiddle.table(), 1, 1));
        assertEquals("e\u0301 Δx λ", cellText(leftMiddle.table(), 1, 2));
        assertEquals(caret(1, 1, 0), leftMiddle.selection());

        assertEquals("θ", cellText(rightFinal.table(), 0, 1));
        assertEquals("", cellText(rightFinal.table(), 0, 2));
        assertEquals(caret(0, 2, 0), rightFinal.selection());
    }

    @Test
    void deleteColumnsFirstMiddleFinalAndRejectOnlyColumn() {
        var table = new TableBlock(List.of(
                row(cell("A"), cell("B"), cell("C")),
                row(cell("D"), cell("E"), cell("F"))), 0);
        var deleteFirst = editor.deleteColumn(table, caret(1, 0, 1));
        var deleteMiddle = editor.deleteColumn(table, caret(0, 1, 1));
        var deleteFinal = editor.deleteColumn(table, caret(1, 2, 1));
        var onlyColumn = new TableBlock(List.of(row(cell("A")), row(cell("B"))), 0);
        var rejected = editor.deleteColumn(onlyColumn, caret(0, 0, 0));

        assertRectangular(deleteFirst.table(), 2, 2);
        assertEquals("B", cellText(deleteFirst.table(), 0, 0));
        assertEquals(caret(1, 0, 0), deleteFirst.selection());
        assertEquals("C", cellText(deleteMiddle.table(), 0, 1));
        assertEquals(caret(0, 1, 0), deleteMiddle.selection());
        assertEquals("E", cellText(deleteFinal.table(), 1, 1));
        assertEquals(caret(1, 1, 0), deleteFinal.selection());
        assertFalse(rejected.changed());
        assertEquals(onlyColumn, rejected.table());
    }

    private static TableCellTextSelection caret(int row, int column, int offset) {
        return TableCellTextSelection.caret(new TableCellCoordinate(row, column), offset);
    }

    private static TableBlock table(TableCell... cells) {
        return new TableBlock(List.of(row(cells)), 0);
    }

    private static TableRow row(TableCell... cells) {
        return new TableRow(List.of(cells));
    }

    private static TableCell cell(String text) {
        return cell(new Text(text, Set.of()));
    }

    private static TableCell cell(Text... text) {
        return new TableCell(new TableCellContent(new InlineContent(List.of(text).stream().map(InlineNode.class::cast).toList())));
    }

    private static TableCell cell(TableBlock table, int row, int column) {
        return table.rows().get(row).cells().get(column);
    }

    private static String cellText(TableBlock table, int row, int column) {
        var builder = new StringBuilder();
        for (var node : cell(table, row, column).content().content().nodes()) {
            builder.append(((Text) node).content());
        }
        return builder.toString();
    }

    private static void assertRectangular(TableBlock table, int rows, int columns) {
        assertEquals(rows, table.rows().size());
        assertEquals(columns, table.columnCount());
        assertTrue(table.rows().stream().allMatch(row -> row.cells().size() == columns));
    }
}
