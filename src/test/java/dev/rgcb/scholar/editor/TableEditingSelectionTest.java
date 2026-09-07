package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TableEditingSelectionTest {
    @Test
    void validSelectionStoresBlockCellAndDirectionalOffsets() {
        var selection = new TableEditingSelection(1, new TableCellTextSelection(new TableCellCoordinate(0, 1), 3, 1));
        var state = new EditorState(document(paragraph("before"), table(), paragraph("after")), selection, Optional.empty());

        assertTrue(state.isTableEditingSelection());
        assertEquals(selection, state.tableEditingSelection());
        assertEquals(1, selection.selection().startOffset());
        assertEquals(3, selection.selection().endOffset());
    }

    @Test
    void validatesBlockRowColumnAndOffsets() {
        var document = document(paragraph("before"), table(), paragraph("after"));

        assertThrows(IllegalArgumentException.class, () -> new TableEditingSelection(-1, TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 0)));
        assertThrows(IllegalArgumentException.class, () -> new TableCellCoordinate(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new TableCellCoordinate(0, -1));
        assertThrows(IllegalArgumentException.class, () -> new TableCellTextSelection(new TableCellCoordinate(0, 0), -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new EditorState(document, new TableEditingSelection(0, TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 0)), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new EditorState(document, new TableEditingSelection(1, TableCellTextSelection.caret(new TableCellCoordinate(3, 0), 0)), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new EditorState(document, new TableEditingSelection(1, TableCellTextSelection.caret(new TableCellCoordinate(0, 3), 0)), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new EditorState(document, new TableEditingSelection(1, TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 99)), Optional.empty()));
    }

    private static Document document(dev.rgcb.scholar.document.BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }

    private static TableBlock table() {
        return new TableBlock(List.of(row(cell("abc"), cell("def")), row(cell("ghi"), cell(""))), 1);
    }

    private static TableRow row(TableCell... cells) {
        return new TableRow(List.of(cells));
    }

    private static TableCell cell(String text) {
        return new TableCell(new TableCellContent(new InlineContent(text.isEmpty()
                ? List.of()
                : List.of((InlineNode) new Text(text, Set.of())))));
    }
}
