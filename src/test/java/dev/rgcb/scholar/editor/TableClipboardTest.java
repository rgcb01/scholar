package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.clipboard.ScholarClipboardService;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.table.clipboard.TableClipboardPayload;
import dev.rgcb.scholar.table.clipboard.TableTsvSerializer;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TableClipboardTest {
    private final TableTsvSerializer tsvSerializer = new TableTsvSerializer();

    @Test
    void tsvSerializerUsesCanonicalTabsNewlinesAndNoTrailingSeparators() {
        assertEquals("A", tsvSerializer.serialize(table(row(cell("A")))));
        assertEquals("A\tB\nC\tD", tsvSerializer.serialize(table(row(cell("A"), cell("B")), row(cell("C"), cell("D")))));
        assertEquals("A\t\n\tD", tsvSerializer.serialize(table(row(cell("A"), TableCell.empty()), row(TableCell.empty(), cell("D")))));
        assertFalse(tsvSerializer.serialize(table(row(cell("A"), cell("B")))).endsWith("\t"));
        assertFalse(tsvSerializer.serialize(table(row(cell("A")), row(cell("B")))).endsWith("\n"));
    }

    @Test
    void tsvSerializerDropsFormattingAndKeepsHeaderTextAndUnicode() {
        var table = new TableBlock(List.of(
                row(cell(new Text("Quantity", Set.of(TextMark.BOLD))), cell("θ")),
                row(cell(new Text("café", Set.of(TextMark.ITALIC))), cell("Δx λ Ω"))), 1);

        assertEquals("Quantity\tθ\ncafé\tΔx λ Ω", tsvSerializer.serialize(table));
    }

    @Test
    void tsvSerializerRejectsTabsAndNewlinesInsideCellContent() {
        assertThrows(IllegalArgumentException.class, () -> tsvSerializer.serialize(table(row(cell("A\tB")))));
        assertThrows(IllegalArgumentException.class, () -> tsvSerializer.serialize(table(row(cell("A\nB")))));
        assertThrows(IllegalArgumentException.class, () -> tsvSerializer.serialize(table(row(cell("A\rB")))));
    }

    @Test
    void tableClipboardPayloadPreservesExactTableAst() {
        var table = richTable();
        var payload = new TableClipboardPayload(table);

        assertEquals(table, payload.table());
        assertEquals(1, payload.table().headerRowCount());
        assertEquals(2, payload.table().rows().size());
        assertEquals(3, payload.table().columnCount());
        assertEquals(Set.of(TextMark.BOLD, TextMark.ITALIC), ((Text) payload.table().rows().get(1).cells().get(0).content().content().nodes().get(0)).marks());
        assertTrue(payload.table().rows().get(1).cells().get(1).content().content().nodes().isEmpty());
    }

    @Test
    void wholeTableCopyWritesTsvInstallsPayloadAndLeavesStateUnchanged() {
        var table = richTable();
        var document = document(paragraph("Before"), table, paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.copy().execute(new EditorActionContext(session, clipboard, sidecar));

        assertFalse(result.documentChanged());
        assertEquals("Quantity\tValue\tUnit\ncafé\t\tΩ", clipboard.text);
        var payload = dev.rgcb.scholar.editor.TransferClipboardAssertions.root(dev.rgcb.scholar.document.TableBlock.class, sidecar.snapshot().orElseThrow().payload());
        assertEquals(table, payload);
        assertEquals(new BlockSelection(1), session.current().selection());
        assertFalse(session.canUndo());
    }

    @Test
    void failedWholeTableCopyDoesNotInstallPayload() {
        var document = document(paragraph("Before"), richTable(), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        var clipboard = new FakeClipboard();
        clipboard.writeSucceeds = false;
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.copy().execute(new EditorActionContext(session, clipboard, sidecar));

        assertFalse(result.documentChanged());
        assertTrue(sidecar.snapshot().isEmpty());
        assertEquals(document, session.current().document());
    }

    @Test
    void wholeTableCutWritesClipboardBeforeDeletingAndUndoRedoRestoresSelection() {
        var table = richTable();
        var document = document(paragraph("Before"), table, paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(result.documentChanged());
        assertEquals("Quantity\tValue\tUnit\ncafé\t\tΩ", clipboard.text);
        dev.rgcb.scholar.editor.TransferClipboardAssertions.root(dev.rgcb.scholar.document.TableBlock.class, sidecar.snapshot().orElseThrow().payload());
        assertEquals(2, session.current().document().blocks().size());
        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertEquals(new BlockSelection(1), session.current().selection());
        assertTrue(session.redo());
        assertEquals(2, session.current().document().blocks().size());
    }

    @Test
    void failedWholeTableCutDoesNotDeleteOrCreateHistory() {
        var document = document(paragraph("Before"), richTable(), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        var clipboard = new FakeClipboard();
        clipboard.writeSucceeds = false;
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard, sidecar));

        assertFalse(result.documentChanged());
        assertEquals(document, session.current().document());
        assertFalse(session.canUndo());
        assertTrue(sidecar.snapshot().isEmpty());
    }

    @Test
    void nativeTablePasteAtTextCaretSplitsParagraphAndSelectsInsertedTable() {
        var copied = richTable();
        var session = new EditorSession(document(paragraph("helloworld")), 0);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 5)));
        var clipboard = new FakeClipboard("Quantity\tValue\tUnit\ncafé\t\tΩ");
        var sidecar = new ScholarClipboardService();
        sidecar.install(clipboard.text, new TableClipboardPayload(copied));

        var result = BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(result.documentChanged());
        assertText("hello", session.current().document().blocks().get(0));
        assertEquals(copied, session.current().document().blocks().get(1));
        assertText("world", session.current().document().blocks().get(2));
        assertEquals(new BlockSelection(1), session.current().selection());
    }

    @Test
    void nativeTablePasteAtStartEndEmptyParagraphAndTextSelectionUsesStructuralInsertion() {
        var copied = richTable();
        var start = sessionWithNativeTableClipboard(new EditorState(document(paragraph("abc")), new DocumentPosition(0, 0)), copied);
        var end = sessionWithNativeTableClipboard(new EditorState(document(paragraph("abc")), new DocumentPosition(0, 3)), copied);
        var empty = sessionWithNativeTableClipboard(new EditorState(document(new Paragraph(new InlineContent(List.of()))), new DocumentPosition(0, 0)), copied);
        var range = sessionWithNativeTableClipboard(new EditorState(document(paragraph("abcdef")), new DocumentPosition(0, 2), new DocumentPosition(0, 4)), copied);

        BuiltInEditorActions.paste().execute(start.context());
        BuiltInEditorActions.paste().execute(end.context());
        BuiltInEditorActions.paste().execute(empty.context());
        BuiltInEditorActions.paste().execute(range.context());

        assertEquals(copied, start.session().current().document().blocks().get(0));
        assertEquals(new BlockSelection(0), start.session().current().selection());
        assertEquals(copied, end.session().current().document().blocks().get(1));
        assertEquals(new BlockSelection(1), end.session().current().selection());
        assertEquals(copied, empty.session().current().document().blocks().get(0));
        assertEquals(new BlockSelection(0), empty.session().current().selection());
        assertText("ab", range.session().current().document().blocks().get(0));
        assertEquals(copied, range.session().current().document().blocks().get(1));
        assertText("ef", range.session().current().document().blocks().get(2));
    }

    @Test
    void nativeTablePasteReplacesSelectedTableBlockAndSupportsUndoRedo() {
        var original = table(row(cell("A"), cell("B")), row(cell("C"), cell("D")));
        var copied = richTable();
        var document = document(paragraph("Before"), original, paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        var clipboard = new FakeClipboard("Quantity\tValue\tUnit\ncafé\t\tΩ");
        var sidecar = new ScholarClipboardService();
        sidecar.install(clipboard.text, new TableClipboardPayload(copied));

        var result = BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(result.documentChanged());
        assertEquals(copied, session.current().document().blocks().get(1));
        assertEquals(new BlockSelection(1), session.current().selection());
        assertTrue(session.undo());
        assertEquals(original, session.current().document().blocks().get(1));
        assertEquals(new BlockSelection(1), session.current().selection());
        assertTrue(session.redo());
        assertEquals(copied, session.current().document().blocks().get(1));
    }

    @Test
    void nativeTablePasteDoesNotHandleOtherAtomicBlockSelectionInThisSlice() {
        var document = document(paragraph("Before"), new EquationBlock(new MathIdentifier("x")), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        var context = contextWithPayload(session, richTable());

        assertFalse(BuiltInEditorActions.paste().isEnabled(context));
        assertFalse(BuiltInEditorActions.paste().execute(context).documentChanged());
        assertEquals(document, session.current().document());
    }

    @Test
    void tableCellPasteUsesPlainTsvFallbackWithoutReplacingOrResizingTable() {
        var target = table(row(cell("A"), cell("B")), row(cell("C"), cell("D")));
        var document = document(paragraph("Before"), target, paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new TableEditingSelection(1, TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 1)), Optional.empty()));
        var clipboard = new FakeClipboard("X\tY\nZ\tW");
        var sidecar = new ScholarClipboardService();
        sidecar.install(clipboard.text, new TableClipboardPayload(richTable()));

        var result = BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(result.documentChanged());
        var table = (TableBlock) session.current().document().blocks().get(1);
        assertEquals(2, table.rows().size());
        assertEquals(2, table.columnCount());
        assertEquals("AX\tY Z\tW", cellText(table, 0, 0));
        assertEquals(new TableEditingSelection(1, TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 8)), session.current().selection());
    }

    @Test
    void staleSidecarFallsBackToPlainTextPaste() {
        var session = new EditorSession(document(paragraph("abc")), 0);
        var clipboard = new FakeClipboard("plain");
        var sidecar = new ScholarClipboardService();
        sidecar.install("A\tB", new TableClipboardPayload(richTable()));

        var result = BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(result.documentChanged());
        assertEquals("abcplain", paragraphText(session.current().document()));
        assertTrue(sidecar.snapshot().isEmpty());
    }

    @Test
    void latestNativePayloadWinsWhenTwoTablesHaveSameTsv() {
        var plain = table(row(cell("A")));
        var formatted = new TableBlock(List.of(row(cell(new Text("A", Set.of(TextMark.BOLD))))), 1);
        var session = new EditorSession(document(paragraph("x")), 0);
        var clipboard = new FakeClipboard("A");
        var sidecar = new ScholarClipboardService();
        sidecar.install("A", new TableClipboardPayload(plain));
        sidecar.install("A", new TableClipboardPayload(formatted));

        BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, sidecar));

        assertEquals(formatted, session.current().document().blocks().get(1));
    }

    private static TableClipboardFixture sessionWithNativeTableClipboard(EditorState state, TableBlock table) {
        var session = new EditorSession(state.document(), 0);
        session.setCurrent(state);
        return new TableClipboardFixture(session, contextWithPayload(session, table));
    }

    private static EditorActionContext contextWithPayload(EditorSession session, TableBlock table) {
        var clipboard = new FakeClipboard(new TableTsvSerializer().serialize(table));
        var sidecar = new ScholarClipboardService();
        sidecar.install(clipboard.text, new TableClipboardPayload(table));
        return new EditorActionContext(session, clipboard, sidecar);
    }

    private static TableBlock richTable() {
        return new TableBlock(List.of(
                row(cell("Quantity"), cell("Value"), cell("Unit")),
                row(cell(new Text("café", Set.of(TextMark.BOLD, TextMark.ITALIC))), TableCell.empty(), cell("Ω"))), 1);
    }

    private static TableBlock table(TableRow... rows) {
        return new TableBlock(List.of(rows), 0);
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

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }

    private static void assertText(String expected, BlockNode block) {
        var text = assertInstanceOf(Text.class, assertInstanceOf(Paragraph.class, block).content().nodes().get(0));
        assertEquals(expected, text.content());
    }

    private static String paragraphText(Document document) {
        return ((Paragraph) document.blocks().get(0)).content().nodes().stream()
                .map(Text.class::cast)
                .map(Text::content)
                .reduce("", String::concat);
    }

    private static String cellText(TableBlock table, int row, int column) {
        return table.rows().get(row).cells().get(column).content().content().nodes().stream()
                .map(Text.class::cast)
                .map(Text::content)
                .reduce("", String::concat);
    }

    private record TableClipboardFixture(EditorSession session, EditorActionContext context) {
    }

    private static final class FakeClipboard implements ClipboardAdapter {
        private String text;
        private boolean writeSucceeds = true;

        private FakeClipboard() {
            this("");
        }

        private FakeClipboard(String text) {
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean setText(String text) {
            if (!writeSucceeds) {
                return false;
            }
            this.text = text;
            return true;
        }
    }
}
