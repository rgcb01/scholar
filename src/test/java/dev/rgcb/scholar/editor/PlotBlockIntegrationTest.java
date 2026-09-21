package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.LaidOutBlockKind;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.markdown.MarkdownSerializer;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlotBlockIntegrationTest {
    private final DocumentEditor editor = new DocumentEditor();
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void documentLayoutPlacesPlotBetweenEditableBlocks() {
        var layout = layoutEngine.layout(document(paragraph("Before"), plot(), paragraph("After")), 180, textMeasurer);

        assertEquals(List.of(LaidOutBlockKind.PARAGRAPH, LaidOutBlockKind.PLOT, LaidOutBlockKind.PARAGRAPH),
                layout.blocks().stream().map(block -> block.kind()).toList());
        assertTrue(layout.blocks().get(1).plot().isPresent());
        assertEquals(180, layout.blocks().get(1).width());
        assertTrue(layout.blocks().get(0).y() < layout.blocks().get(1).y());
        assertTrue(layout.blocks().get(1).y() < layout.blocks().get(2).y());
    }

    @Test
    void hitTestingSelectsPlotAsAtomicBlock() {
        var layout = layoutEngine.layout(document(paragraph("Before"), plot(), paragraph("After")), 180, textMeasurer);
        var plotBlock = layout.blocks().get(1);
        var hitTester = new DocumentHitTester();

        assertEquals(DocumentHit.block(1), hitTester.hit(layout, plotBlock.x() + 2, plotBlock.y() + 2, textMeasurer));
        assertTrue(hitTester.hitTest(layout, plotBlock.x() + 2, plotBlock.y() + 2, textMeasurer).isEmpty());
    }

    @Test
    void verticalNavigationTreatsPlotAsAtomicBlock() {
        var document = document(paragraph("abc"), plot(), paragraph("def"));
        var layout = layoutEngine.layout(document, 180, textMeasurer);
        var navigator = new VisualLineNavigator();

        var plotSelection = navigator.moveDown(new EditorState(document, new DocumentPosition(0, 2)), layout, textMeasurer, 20, true).orElseThrow();
        var below = navigator.moveDown(new EditorState(document, plotSelection, java.util.Optional.empty()), layout, textMeasurer, 20, true).orElseThrow();
        var plotAgain = navigator.moveUp(new EditorState(document, below, java.util.Optional.empty()), layout, textMeasurer, 20, true).orElseThrow();

        assertEquals(new BlockSelection(1), plotSelection);
        assertEquals(caret(2, 3), below);
        assertEquals(new BlockSelection(1), plotAgain);
    }

    @Test
    void leftRightNavigationTreatsPlotAsStructuralObject() {
        var document = document(paragraph("a"), plot(), paragraph("b"));

        var fromLeft = editor.moveRight(new EditorState(document, new DocumentPosition(0, 1))).editorState();
        var afterPlot = editor.moveRight(fromLeft).editorState();
        var fromRight = editor.moveLeft(new EditorState(document, new DocumentPosition(2, 0))).editorState();
        var beforePlot = editor.moveLeft(fromRight).editorState();

        assertEquals(new BlockSelection(1), fromLeft.selection());
        assertEquals(caret(2, 0), afterPlot.selection());
        assertEquals(new BlockSelection(1), fromRight.selection());
        assertEquals(caret(0, 1), beforePlot.selection());
    }

    @Test
    void insertDefaultPlotSplitsParagraphAndSelectsPlot() {
        var result = editor.insertDefaultPlot(new EditorState(document(paragraph("abcd")), new DocumentPosition(0, 2)));

        assertTrue(result.changed());
        assertEquals(3, result.document().blocks().size());
        assertText("ab", result.document().blocks().get(0));
        var inserted = assertInstanceOf(PlotBlock.class, result.document().blocks().get(1));
        assertText("cd", result.document().blocks().get(2));
        assertEquals(new BlockSelection(1), result.selection());
        assertEquals("Untitled Plot", inserted.definition().title());
        assertEquals("Series 1", inserted.definition().series().get(0).name());
        assertTrue(inserted.definition().series().get(0).points().isEmpty());
    }

    @Test
    void insertDefaultPlotAfterSelectedTableKeepsBothAtomicBlocks() {
        var document = document(paragraph("Before"), TableBlock.empty(1, 1), paragraph("After"));
        var result = editor.insertDefaultPlot(new EditorState(document, new BlockSelection(1), java.util.Optional.empty()));

        assertTrue(result.changed());
        assertInstanceOf(TableBlock.class, result.document().blocks().get(1));
        assertInstanceOf(PlotBlock.class, result.document().blocks().get(2));
        assertEquals(new BlockSelection(2), result.selection());
    }

    @Test
    void selectedPlotDeletesAsWholeAndUndoRedoRestoresIt() {
        var document = document(paragraph("Before"), plot(), paragraph("After"));
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
    void insertMenuContainsPlotActionAndSessionExecutesIt() {
        var ids = BuiltInEditorActions.insertMenuActions().stream().map(EditorAction::id).toList();
        var session = new EditorSession(document(paragraph("abc")), 0);

        assertTrue(ids.contains(EditorActionId.INSERT_PLOT));
        assertTrue(session.supportsInsertPlot());
        assertTrue(session.insertDefaultPlot());
        assertInstanceOf(PlotBlock.class, session.current().document().blocks().get(1));
    }

    @Test
    void markdownSerializerExplicitlyRejectsPlotBlock() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> new MarkdownSerializer().serialize(document(plot())));
        assertTrue(error.getMessage().contains("plot"));
    }

    @Test
    void selectedPlotDoesNotSupportTextFormattingOrBlockStyle() {
        var document = document(paragraph("Before"), plot(), paragraph("After"));
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

    private static PlotBlock plot() {
        return new PlotBlock(PlotDefinition.of(
                "Motion Ω",
                AxisDefinition.linear("Time (s)"),
                AxisDefinition.linear("Position (m)"),
                List.of(new PlotSeries("Series Δ", PlotSeriesKind.LINE, List.of(
                        new DataPoint(-1, 1), new DataPoint(0, 0), new DataPoint(1, 1))))));
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
