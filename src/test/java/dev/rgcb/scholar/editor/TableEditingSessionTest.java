package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TableEditingSessionTest {
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();

    @Test
    void enterSelectedTableAndEscapeReturnsToBlockSelection() {
        var document = document(paragraph("Before"), table(), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));

        assertFalse(session.enter());
        assertEquals(new TableEditingSelection(1, caret(0, 0, 0)), session.current().selection());

        session.exitTableEditing();
        assertEquals(new BlockSelection(1), session.current().selection());
    }

    @Test
    void typingUndoRedoRestoresTableEditingSelection() {
        var document = document(paragraph("Before"), table(), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new TableEditingSelection(1, caret(0, 0, 1)), Optional.empty()));

        assertTrue(session.typeText("X"));
        assertEquals("aXbc", cellText(currentTable(session), 0, 0));
        assertEquals(new TableEditingSelection(1, caret(0, 0, 2)), session.current().selection());

        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertEquals(new TableEditingSelection(1, caret(0, 0, 1)), session.current().selection());

        assertTrue(session.redo());
        assertEquals("aXbc", cellText(currentTable(session), 0, 0));
    }

    @Test
    void tabShiftTabAndArrowNavigationStayCellLocal() {
        var document = document(paragraph("Before"), table(), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new TableEditingSelection(1, caret(0, 0, 3)), Optional.empty()));

        session.moveRight();
        assertEquals(new TableEditingSelection(1, caret(0, 0, 3)), session.current().selection());

        session.moveNextTableCell();
        assertEquals(new TableEditingSelection(1, caret(0, 1, 0)), session.current().selection());

        session.movePreviousTableCell();
        assertEquals(new TableEditingSelection(1, caret(0, 0, 0)), session.current().selection());

        session.moveLeft();
        assertEquals(new TableEditingSelection(1, caret(0, 0, 0)), session.current().selection());
    }

    @Test
    void verticalNavigationAndHomeEndStayInsideWrappedCell() {
        var document = document(paragraph("Before"), table(cell("alpha beta gamma")), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new TableEditingSelection(1, caret(0, 0, 2)), Optional.empty()));
        var layout = layoutEngine.layout(document, 80, textMeasurer);

        session.moveDown(layout, textMeasurer);
        assertInstanceOf(TableEditingSelection.class, session.current().selection());
        assertTrue(session.current().tableEditingSelection().selection().activeOffset() > 2);

        session.moveHome(layout);
        assertEquals(6, session.current().tableEditingSelection().selection().activeOffset());

        session.extendEnd(layout);
        assertFalse(session.current().tableEditingSelection().selection().isCaret());
    }

    @Test
    void boldItalicAndTypingMarksWorkThroughSharedActions() {
        var document = document(paragraph("Before"), table(), paragraph("After"));
        var session = new EditorSession(document, 0);
        var bold = BuiltInEditorActions.bold();
        var italic = BuiltInEditorActions.italic();
        session.setCurrent(new EditorState(document, new TableEditingSelection(1, new TableCellTextSelection(new TableCellCoordinate(0, 0), 0, 2)), Optional.empty()));

        assertTrue(bold.isEnabled(context(session)));
        assertEquals(ActionSelectionState.OFF, bold.selectionState(context(session)));
        assertTrue(bold.execute(context(session)).documentChanged());
        assertEquals(ActionSelectionState.ON, bold.selectionState(context(session)));

        session.setCurrent(new EditorState(session.current().document(), new TableEditingSelection(1, caret(0, 1, 0)), Optional.empty()));
        italic.execute(context(session));
        assertTrue(session.current().explicitTypingMarks().orElseThrow().contains(TextMark.ITALIC));
        assertTrue(session.typeText("i"));
        assertEquals(Set.of(TextMark.ITALIC), ((Text) currentTable(session).rows().get(0).cells().get(1).content().content().nodes().get(0)).marks());
    }

    @Test
    void copyCutPasteInsideCellUsePlainTextAndHistory() {
        var document = document(paragraph("Before"), table(), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new TableEditingSelection(1, new TableCellTextSelection(new TableCellCoordinate(0, 0), 1, 3)), Optional.empty()));

        assertEquals("bc", session.copyForClipboard().orElseThrow().plainText());
        var cut = session.cutForClipboard().orElseThrow();
        assertEquals("bc", cut.plainText());
        assertTrue(session.applyCut(cut));
        assertEquals("a", cellText(currentTable(session), 0, 0));
        assertTrue(session.undo());
        assertEquals("abc", cellText(currentTable(session), 0, 0));

        session.setCurrent(new EditorState(session.current().document(), new TableEditingSelection(1, caret(0, 1, 1)), Optional.empty()));
        assertTrue(session.pasteFromClipboard(Optional.empty(), "x\ny"));
        assertEquals("dx ye", cellText(currentTable(session), 0, 1));
    }

    @Test
    void blockStyleAndStructuralInsertAreDisabledInsideCellEditing() {
        var document = document(paragraph("Before"), table(), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new TableEditingSelection(1, caret(0, 0, 0)), Optional.empty()));

        assertFalse(session.supportsBlockStyle());
        assertFalse(session.supportsInsertEquation());
        assertFalse(session.supportsInsertTable());
    }

    @Test
    void rowColumnOperationsUseHistoryAndPreserveTableEditingSelection() {
        var document = document(paragraph("Before"), twoByTwoTable(), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new TableEditingSelection(1, new TableCellTextSelection(new TableCellCoordinate(1, 1), 1, 0)), Optional.empty()));

        assertTrue(session.insertTableRowAbove());
        assertEquals(3, currentTable(session).rows().size());
        assertEquals("", cellText(currentTable(session), 1, 1));
        assertEquals(new TableEditingSelection(1, caret(1, 1, 0)), session.current().selection());

        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertEquals(new TableEditingSelection(1, new TableCellTextSelection(new TableCellCoordinate(1, 1), 1, 0)), session.current().selection());

        assertTrue(session.redo());
        assertEquals(new TableEditingSelection(1, caret(1, 1, 0)), session.current().selection());

        assertTrue(session.insertTableColumnLeft());
        assertEquals(3, currentTable(session).columnCount());
        assertEquals("", cellText(currentTable(session), 1, 1));
        assertEquals(new TableEditingSelection(1, caret(1, 1, 0)), session.current().selection());
    }

    @Test
    void deleteOnlyRowOrColumnIsUnsupportedAndDoesNotCreateHistory() {
        var oneCell = document(paragraph("Before"), table(cell("x")), paragraph("After"));
        var session = new EditorSession(oneCell, 0);
        session.setCurrent(new EditorState(oneCell, new TableEditingSelection(1, caret(0, 0, 0)), Optional.empty()));

        assertFalse(session.supportsDeleteTableRow());
        assertFalse(session.supportsDeleteTableColumn());
        assertFalse(session.deleteTableRow());
        assertFalse(session.deleteTableColumn());
        assertFalse(session.canUndo());
    }

    @Test
    void structuralTableMutationBreaksTypingCoalescingAndResetsPreferredX() {
        var document = document(paragraph("Before"), twoByTwoTable(), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new TableEditingSelection(1, caret(0, 0, 1)), Optional.empty()));
        var layout = layoutEngine.layout(document, 90, textMeasurer);

        session.moveDown(layout, textMeasurer);
        assertTrue(session.preferredCaretX().isPresent());
        assertTrue(session.typeText("x"));
        assertTrue(session.insertTableRowBelow());
        assertTrue(session.preferredCaretX().isEmpty());
        assertTrue(session.typeText("y"));

        assertTrue(session.undo());
        assertEquals("", cellText(currentTable(session), 1, 0));
        assertTrue(session.undo());
        assertEquals(2, currentTable(session).rows().size());
        assertEquals("ax", cellText(currentTable(session), 0, 0));
        assertTrue(session.undo());
        assertEquals("a", cellText(currentTable(session), 0, 0));
    }

    @Test
    void tableRowColumnActionsAreApplicableOnlyForValidTableEditingState() {
        var document = document(paragraph("Before"), twoByTwoTable(), paragraph("After"));
        var session = new EditorSession(document, 0);
        var context = context(session);

        assertFalse(BuiltInEditorActions.insertTableRowAbove().isEnabled(context));
        assertFalse(BuiltInEditorActions.deleteTableColumn().isEnabled(context));

        session.setCurrent(new EditorState(document, new TableEditingSelection(1, caret(0, 0, 0)), Optional.empty()));
        context = context(session);
        assertTrue(BuiltInEditorActions.insertTableRowAbove().isEnabled(context));
        assertTrue(BuiltInEditorActions.insertTableRowBelow().isEnabled(context));
        assertTrue(BuiltInEditorActions.deleteTableRow().isEnabled(context));
        assertTrue(BuiltInEditorActions.insertTableColumnLeft().isEnabled(context));
        assertTrue(BuiltInEditorActions.insertTableColumnRight().isEnabled(context));
        assertTrue(BuiltInEditorActions.deleteTableColumn().isEnabled(context));
        assertTrue(BuiltInEditorActions.insertTableColumnRight().execute(context).documentChanged());
        assertEquals(3, currentTable(session).columnCount());

        var oneCell = document(paragraph("Before"), table(cell("x")), paragraph("After"));
        session.setCurrent(new EditorState(oneCell, new TableEditingSelection(1, caret(0, 0, 0)), Optional.empty()));
        context = context(session);
        assertFalse(BuiltInEditorActions.deleteTableRow().isEnabled(context));
        assertFalse(BuiltInEditorActions.deleteTableColumn().isEnabled(context));
    }

    private static EditorActionContext context(EditorSession session) {
        return new EditorActionContext(session, new FakeClipboard());
    }

    private static TableCellTextSelection caret(int row, int column, int offset) {
        return TableCellTextSelection.caret(new TableCellCoordinate(row, column), offset);
    }

    private static TableBlock currentTable(EditorSession session) {
        return (TableBlock) session.current().document().blocks().get(1);
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }

    private static TableBlock table() {
        return table(cell("abc"), cell("de"));
    }

    private static TableBlock table(TableCell... cells) {
        return new TableBlock(List.of(new TableRow(List.of(cells))), 0);
    }

    private static TableBlock twoByTwoTable() {
        return new TableBlock(List.of(
                new TableRow(List.of(cell("a"), cell("b"))),
                new TableRow(List.of(cell("c"), cell("d")))), 1);
    }

    private static TableCell cell(String text) {
        return new TableCell(new TableCellContent(new InlineContent(List.of((InlineNode) new Text(text, Set.of())))));
    }

    private static String cellText(TableBlock table, int row, int column) {
        var builder = new StringBuilder();
        for (var node : table.rows().get(row).cells().get(column).content().content().nodes()) {
            builder.append(((Text) node).content());
        }
        return builder.toString();
    }

    private static final class FakeClipboard implements ClipboardAdapter {
        private String value = "";

        @Override
        public String getText() {
            return value;
        }

        @Override
        public boolean setText(String text) {
            value = text;
            return true;
        }
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            return TextBoundary.characterCount(text) * 10;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return 10;
        }
    }
}
