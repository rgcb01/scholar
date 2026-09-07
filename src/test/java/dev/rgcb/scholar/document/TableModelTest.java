package dev.rgcb.scholar.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TableModelTest {
    @Test
    void createsValidOneByOneAndTwoByTwoTables() {
        var oneByOne = new TableBlock(List.of(row(cell("x"))), 0);
        var twoByTwo = new TableBlock(List.of(
                row(cell("a"), cell("b")),
                row(cell("c"), cell("d"))), 1);

        assertEquals(1, oneByOne.rows().size());
        assertEquals(1, oneByOne.columnCount());
        assertEquals(2, twoByTwo.rows().size());
        assertEquals(2, twoByTwo.columnCount());
        assertEquals(1, twoByTwo.headerRowCount());
    }

    @Test
    void emptyFactoriesCreateEmptyInlineContentWithoutPlaceholderText() {
        var cell = TableCell.empty();
        var table = TableBlock.empty(2, 2);

        assertTrue(cell.content().content().nodes().isEmpty());
        assertEquals(2, table.rows().size());
        assertEquals(2, table.columnCount());
        assertTrue(table.rows().get(0).cells().get(0).content().content().nodes().isEmpty());
    }

    @Test
    void rejectsInvalidTableShapesAndHeaders() {
        assertThrows(NullPointerException.class, () -> new TableBlock(null, 0));
        assertThrows(IllegalArgumentException.class, () -> new TableBlock(List.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> new TableBlock(List.of(row()), 0));
        assertThrows(IllegalArgumentException.class, () -> new TableBlock(List.of(row(cell("a")), row(cell("b"), cell("c"))), 0));
        assertThrows(IllegalArgumentException.class, () -> new TableBlock(List.of(row(cell("a"))), -1));
        assertThrows(IllegalArgumentException.class, () -> new TableBlock(List.of(row(cell("a"))), 2));
        assertThrows(NullPointerException.class, () -> new TableRow(null));
        assertThrows(IllegalArgumentException.class, () -> new TableRow(List.of()));
        assertThrows(NullPointerException.class, () -> new TableCell(null));
        assertThrows(NullPointerException.class, () -> new TableCellContent(null));
        assertThrows(IllegalArgumentException.class, () -> TableBlock.empty(0, 1));
        assertThrows(IllegalArgumentException.class, () -> TableBlock.empty(1, 0));
    }

    @Test
    void rejectsNullRowsAndCells() {
        var rows = new ArrayList<TableRow>();
        rows.add(null);
        var cells = new ArrayList<TableCell>();
        cells.add(null);

        assertThrows(NullPointerException.class, () -> new TableBlock(rows, 0));
        assertThrows(NullPointerException.class, () -> new TableRow(cells));
    }

    @Test
    void defensivelyCopiesCollections() {
        var cells = new ArrayList<>(List.of(cell("a")));
        var row = new TableRow(cells);
        cells.add(cell("b"));

        var rows = new ArrayList<>(List.of(row));
        var table = new TableBlock(rows, 0);
        rows.add(row);

        assertEquals(1, row.cells().size());
        assertEquals(1, table.rows().size());
        assertThrows(UnsupportedOperationException.class, () -> row.cells().add(cell("x")));
        assertThrows(UnsupportedOperationException.class, () -> table.rows().add(row));
    }

    @Test
    void structuralEqualityUsesCellContent() {
        var first = new TableBlock(List.of(row(cell("a"), cell("b"))), 0);
        var second = new TableBlock(List.of(row(cell("a"), cell("b"))), 0);

        assertEquals(first, second);
        assertNotSame(first, second);
    }

    @Test
    void cellContentPreservesInlineMarks() {
        var content = new TableCellContent(new InlineContent(List.of(
                (InlineNode) new Text("bold", Set.of(TextMark.BOLD)),
                (InlineNode) new Text(" italic", Set.of(TextMark.ITALIC)),
                (InlineNode) new Text(" both", Set.of(TextMark.BOLD, TextMark.ITALIC)))));

        assertEquals(3, content.content().nodes().size());
    }

    private static TableRow row(TableCell... cells) {
        return new TableRow(List.of(cells));
    }

    private static TableCell cell(String text) {
        return new TableCell(new TableCellContent(new InlineContent(List.of((InlineNode) new Text(text, Set.of())))));
    }
}
