package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.LaidOutBlockKind;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.markdown.MarkdownSerializer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiagramBlockIntegrationTest {
    private final DocumentEditor editor = new DocumentEditor();
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void documentLayoutPlacesDiagramBetweenEditableBlocks() {
        var layout = layoutEngine.layout(document(paragraph("Before"), diagram(), paragraph("After")), 180, textMeasurer);

        assertEquals(List.of(LaidOutBlockKind.PARAGRAPH, LaidOutBlockKind.DIAGRAM, LaidOutBlockKind.PARAGRAPH),
                layout.blocks().stream().map(block -> block.kind()).toList());
        assertTrue(layout.blocks().get(1).diagram().isPresent());
        assertEquals(180, layout.blocks().get(1).width());
        assertTrue(layout.blocks().get(1).y() < layout.blocks().get(2).y());
    }

    @Test
    void hitTestingSelectsDiagramAsAtomicBlock() {
        var layout = layoutEngine.layout(document(paragraph("Before"), diagram(), paragraph("After")), 180, textMeasurer);
        var block = layout.blocks().get(1);
        var hitTester = new DocumentHitTester();

        assertEquals(DocumentHit.block(1), hitTester.hit(layout, block.x() + 2, block.y() + 2, textMeasurer));
        assertTrue(hitTester.hitTest(layout, block.x() + 2, block.y() + 2, textMeasurer).isEmpty());
    }

    @Test
    void leftRightNavigationTreatsDiagramAsStructuralObject() {
        var document = document(paragraph("a"), diagram(), paragraph("b"));

        var fromLeft = editor.moveRight(new EditorState(document, new DocumentPosition(0, 1))).editorState();
        var after = editor.moveRight(fromLeft).editorState();
        var fromRight = editor.moveLeft(new EditorState(document, new DocumentPosition(2, 0))).editorState();
        var before = editor.moveLeft(fromRight).editorState();

        assertEquals(new BlockSelection(1), fromLeft.selection());
        assertEquals(caret(2, 0), after.selection());
        assertEquals(new BlockSelection(1), fromRight.selection());
        assertEquals(caret(0, 1), before.selection());
    }

    @Test
    void insertDefaultDiagramSplitsParagraphAndSelectsDiagram() {
        var result = editor.insertDefaultDiagram(new EditorState(document(paragraph("abcd")), new DocumentPosition(0, 2)));

        assertTrue(result.changed());
        assertEquals(3, result.document().blocks().size());
        assertText("ab", result.document().blocks().get(0));
        assertTrue(result.document().blocks().get(1) instanceof DiagramBlock);
        var inserted = (DiagramBlock) result.document().blocks().get(1);
        assertText("cd", result.document().blocks().get(2));
        assertEquals(new BlockSelection(1), result.selection());
        assertEquals("Untitled Diagram", inserted.definition().title());
        assertTrue(inserted.definition().elements().isEmpty());
        assertTrue(inserted.definition().connections().isEmpty());
    }

    @Test
    void insertDefaultDiagramAfterSelectedPlotKeepsBothAtomicBlocks() {
        var plot = new DocumentEditor().insertDefaultPlot(new EditorState(document(paragraph("x")), new DocumentPosition(0, 1)))
                .document().blocks().get(1);
        assertTrue(plot instanceof PlotBlock);
        var document = document(paragraph("Before"), plot, paragraph("After"));
        var result = editor.insertDefaultDiagram(new EditorState(document, new BlockSelection(1), java.util.Optional.empty()));

        assertTrue(result.changed());
        assertTrue(result.document().blocks().get(1) instanceof PlotBlock);
        assertTrue(result.document().blocks().get(2) instanceof DiagramBlock);
        assertEquals(new BlockSelection(2), result.selection());
    }

    @Test
    void selectedDiagramDeletesAsWholeAndUndoRedoRestoresIt() {
        var document = document(paragraph("Before"), diagram(), paragraph("After"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), java.util.Optional.empty()));

        assertTrue(session.deleteForward());
        assertEquals(2, session.current().document().blocks().size());
        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertEquals(new BlockSelection(1), session.current().selection());
        assertTrue(session.redo());
        assertEquals(2, session.current().document().blocks().size());
    }

    @Test
    void insertMenuContainsDiagramActionAndSessionExecutesIt() {
        var ids = BuiltInEditorActions.insertMenuActions().stream().map(EditorAction::id).toList();
        var session = new EditorSession(document(paragraph("abc")), 0);

        assertTrue(ids.contains(EditorActionId.INSERT_DIAGRAM));
        assertTrue(session.supportsInsertDiagram());
        assertTrue(session.insertDefaultDiagram());
        assertTrue(session.current().document().blocks().get(1) instanceof DiagramBlock);
    }

    @Test
    void markdownSerializerExplicitlyRejectsDiagramBlock() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> new MarkdownSerializer().serialize(document(diagram())));
        assertTrue(error.getMessage().contains("diagram"));
    }

    @Test
    void selectedDiagramDoesNotSupportTextFormattingOrBlockStyle() {
        var document = document(paragraph("Before"), diagram(), paragraph("After"));
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
        assertTrue(block instanceof Paragraph);
        var paragraph = (Paragraph) block;
        assertTrue(paragraph.content().nodes().get(0) instanceof Text);
        var text = (Text) paragraph.content().nodes().get(0);
        assertEquals(expected, text.content());
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }

    private static DiagramBlock diagram() {
        return new DiagramBlock(new DiagramDefinition(
                "System",
                new DiagramCanvas(100, 50),
                List.of(new DiagramNode(
                        new DiagramElementId("node"),
                        new DiagramBounds(20, 10, 30, 20),
                        "Node",
                        List.of())),
                List.of()));
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            return TextBoundary.characterCount(text) * 8;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return 10;
        }
    }
}
