package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.LaidOutBlockKind;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.markdown.MarkdownSerializer;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TableBlockIntegrationTest {
    private final DocumentEditor editor = new DocumentEditor();
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void documentLayoutPlacesTableBetweenEditableBlocks() {
        var document = document(paragraph("Before"), table(), paragraph("After"));
        var layout = layoutEngine.layout(document, 120, textMeasurer);

        assertEquals(List.of(LaidOutBlockKind.PARAGRAPH, LaidOutBlockKind.TABLE, LaidOutBlockKind.PARAGRAPH),
                layout.blocks().stream().map(block -> block.kind()).toList());
        assertTrue(layout.blocks().get(1).table().isPresent());
        assertTrue(layout.blocks().get(0).y() < layout.blocks().get(1).y());
        assertTrue(layout.blocks().get(1).y() < layout.blocks().get(2).y());
    }

    @Test
    void documentLayoutSupportsHeadingEquationTableMixture() {
        var document = document(
                paragraph("Before"),
                new EquationBlock(new MathIdentifier("x")),
                table(),
                paragraph("After"));
        var layout = layoutEngine.layout(document, 120, textMeasurer, (content, kind) -> new MathTextMetrics(10, 7, 3));

        assertEquals(List.of(
                        LaidOutBlockKind.PARAGRAPH,
                        LaidOutBlockKind.EQUATION,
                        LaidOutBlockKind.TABLE,
                        LaidOutBlockKind.PARAGRAPH),
                layout.blocks().stream().map(block -> block.kind()).toList());
        assertTrue(layout.blocks().get(2).y() >= layout.blocks().get(1).y() + layout.blocks().get(1).height());
    }

    @Test
    void hitTestingSelectsTableAsAtomicBlock() {
        var layout = layoutEngine.layout(document(paragraph("Before"), table(), paragraph("After")), 120, textMeasurer);
        var tableBlock = layout.blocks().get(1);
        var hitTester = new DocumentHitTester();

        assertEquals(DocumentHit.block(1), hitTester.hit(layout, tableBlock.x() + 1, tableBlock.y() + 1, textMeasurer));
        assertTrue(hitTester.hitTest(layout, tableBlock.x() + 1, tableBlock.y() + 1, textMeasurer).isEmpty());
    }

    @Test
    void verticalNavigationTreatsTableAsAtomicBlock() {
        var document = document(paragraph("abc"), table(), paragraph("def"));
        var layout = layoutEngine.layout(document, 120, textMeasurer);
        var navigator = new VisualLineNavigator();

        var tableSelection = navigator.moveDown(new EditorState(document, new DocumentPosition(0, 2)), layout, textMeasurer, 20, true).orElseThrow();
        var below = navigator.moveDown(new EditorState(document, tableSelection, java.util.Optional.empty()), layout, textMeasurer, 20, true).orElseThrow();
        var tableAgain = navigator.moveUp(new EditorState(document, below, java.util.Optional.empty()), layout, textMeasurer, 20, true).orElseThrow();

        assertEquals(new BlockSelection(1), tableSelection);
        assertEquals(caret(2, 2), below);
        assertEquals(new BlockSelection(1), tableAgain);
    }

    @Test
    void leftRightNavigationTreatsTableAsStructuralObject() {
        var document = document(paragraph("a"), table(), paragraph("b"));

        var fromLeft = editor.moveRight(new EditorState(document, new DocumentPosition(0, 1))).editorState();
        var afterTable = editor.moveRight(fromLeft).editorState();
        var fromRight = editor.moveLeft(new EditorState(document, new DocumentPosition(2, 0))).editorState();
        var beforeTable = editor.moveLeft(fromRight).editorState();

        assertEquals(new BlockSelection(1), fromLeft.selection());
        assertEquals(caret(2, 0), afterTable.selection());
        assertEquals(new BlockSelection(1), fromRight.selection());
        assertEquals(caret(0, 1), beforeTable.selection());
    }

    @Test
    void insertDefaultTableSplitsParagraphAndSelectsTheTable() {
        var result = editor.insertDefaultTable(new EditorState(document(paragraph("abcd")), new DocumentPosition(0, 2)));

        assertTrue(result.changed());
        assertEquals(3, result.document().blocks().size());
        assertText("ab", result.document().blocks().get(0));
        assertInstanceOf(TableBlock.class, result.document().blocks().get(1));
        assertText("cd", result.document().blocks().get(2));
        assertEquals(new BlockSelection(1), result.selection());

        var table = (TableBlock) result.document().blocks().get(1);
        assertEquals(2, table.rows().size());
        assertEquals(2, table.columnCount());
        assertEquals(0, table.headerRowCount());
    }

    @Test
    void insertDefaultTableReplacesEmptyParagraphAndKeepsAuthoringFallback() {
        var result = editor.insertDefaultTable(new EditorState(document(new Paragraph(new InlineContent(List.of()))), new DocumentPosition(0, 0)));

        assertTrue(result.changed());
        assertInstanceOf(TableBlock.class, result.document().blocks().get(0));
        assertInstanceOf(Paragraph.class, result.document().blocks().get(1));
        assertEquals(new BlockSelection(0), result.selection());
    }

    @Test
    void insertDefaultTableAfterSelectedEquationKeepsBothAtomicBlocks() {
        var document = document(paragraph("Before"), new EquationBlock(new MathSequence(List.of())), paragraph("After"));
        var result = editor.insertDefaultTable(new EditorState(document, new BlockSelection(1), java.util.Optional.empty()));

        assertTrue(result.changed());
        assertInstanceOf(EquationBlock.class, result.document().blocks().get(1));
        assertInstanceOf(TableBlock.class, result.document().blocks().get(2));
        assertEquals(new BlockSelection(2), result.selection());
    }

    @Test
    void selectedTableDeletesAsWholeAndUndoRedoRestoresIt() {
        var document = document(paragraph("Before"), table(), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), java.util.Optional.empty()));

        assertTrue(session.deleteForward());
        assertEquals(2, session.current().document().blocks().size());
        assertEquals(caret(0, 6), session.current().selection());

        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertEquals(new BlockSelection(1), session.current().selection());

        assertTrue(session.redo());
        assertEquals(2, session.current().document().blocks().size());
    }

    @Test
    void insertMenuContainsTableActionAndSessionExecutesIt() {
        var ids = BuiltInEditorActions.insertMenuActions().stream().map(EditorAction::id).toList();
        var session = new EditorSession(document(paragraph("abc")), 0);

        assertTrue(ids.contains(EditorActionId.INSERT_TABLE));
        assertTrue(session.supportsInsertTable());
        assertTrue(session.insertDefaultTable());
        assertInstanceOf(TableBlock.class, session.current().document().blocks().get(1));
    }

    @Test
    void markdownSerializerSupportsHeaderedTablesAndRejectsHeaderlessTables() {
        var serializer = new MarkdownSerializer();

        assertEquals("""
                | Quantity | Value |
                | --- | --- |
                | Voltage | 12 |
                """, serializer.serialize(document(table())));
        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(document(new TableBlock(List.of(row(cell("A"))), 0))));
    }

    @Test
    void tableDoesNotSupportTextFormattingOrBlockStyleWhenSelected() {
        var document = document(paragraph("Before"), table(), paragraph("After"));
        var state = new EditorState(document, new BlockSelection(1), java.util.Optional.empty());

        assertFalse(editor.supportsInlineFormatting(state));
        assertFalse(editor.supportsBlockStyle(state));
        assertFalse(editor.toggleMark(state, TextMark.BOLD).changed());
        assertFalse(editor.setBlockStyle(state, BlockStyle.paragraph()).changed());
    }

    private static TextSelection caret(int blockIndex, int offset) {
        var position = new DocumentPosition(blockIndex, offset);
        return new TextSelection(position, position);
    }

    private static void assertText(String expected, BlockNode block) {
        var paragraph = assertInstanceOf(Paragraph.class, block);
        var text = assertInstanceOf(Text.class, paragraph.content().nodes().get(0));
        assertEquals(expected, text.content());
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }

    private static TableBlock table() {
        return new TableBlock(List.of(
                row(cell("Quantity"), cell("Value")),
                row(cell("Voltage"), cell("12"))), 1);
    }

    private static TableRow row(TableCell... cells) {
        return new TableRow(List.of(cells));
    }

    private static TableCell cell(String text) {
        return new TableCell(new TableCellContent(new InlineContent(List.of((InlineNode) new Text(text, Set.of())))));
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
