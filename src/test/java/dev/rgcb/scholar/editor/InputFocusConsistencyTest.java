package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathOperatorRole;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.editor.MathCaretSelection;
import dev.rgcb.scholar.math.editor.MathPath;
import dev.rgcb.scholar.math.editor.MathRangeSelection;
import dev.rgcb.scholar.math.editor.MathSequencePosition;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InputFocusConsistencyTest {
    private final EditorSelectionValidator selectionValidator = new EditorSelectionValidator();

    @Test
    void focusOwnerReflectsExactlyOneActiveSelectionDomain() {
        var document = mixedDocument();
        var session = new EditorSession(document, 0);
        assertEquals(EditorFocusOwner.DOCUMENT_TEXT, session.focusOwner());

        session.setCurrent(state(document, new BlockSelection(1)));
        assertEquals(EditorFocusOwner.BLOCK, session.focusOwner());
        session.enter();
        assertEquals(EditorFocusOwner.EQUATION, session.focusOwner());

        session.setCurrent(state(document, new BlockSelection(2)));
        session.enter();
        assertEquals(EditorFocusOwner.TABLE, session.focusOwner());

        session.setCurrent(state(document, new BlockSelection(3)));
        session.enter();
        assertEquals(EditorFocusOwner.PLOT, session.focusOwner());

        session.setCurrent(state(document, new BlockSelection(4)));
        session.enter();
        assertEquals(EditorFocusOwner.DIAGRAM, session.focusOwner());

        session.setCurrent(state(document, new BlockSelection(5)));
        assertTrue(session.editFigureCaption());
        assertEquals(EditorFocusOwner.FIGURE_CAPTION, session.focusOwner());
        assertValid(session);
    }

    @Test
    void enteringAndLeavingNestedEditorsIsSelectionOnlyAndDoesNotCreateHistory() {
        var document = mixedDocument();
        var session = new EditorSession(document, 0);

        enterBlock(session, document, 1, EditorFocusOwner.EQUATION);
        assertDepth(session, 0, 0);

        enterBlock(session, document, 2, EditorFocusOwner.TABLE);
        session.exitTableEditing();
        assertEquals(EditorFocusOwner.BLOCK, session.focusOwner());
        assertDepth(session, 0, 0);

        enterBlock(session, document, 3, EditorFocusOwner.PLOT);
        session.exitPlotEditing();
        assertEquals(EditorFocusOwner.BLOCK, session.focusOwner());
        assertDepth(session, 0, 0);

        enterBlock(session, document, 4, EditorFocusOwner.DIAGRAM);
        session.exitDiagramEditing();
        assertEquals(EditorFocusOwner.BLOCK, session.focusOwner());
        assertDepth(session, 0, 0);

        session.setCurrent(state(document, new BlockSelection(5)));
        assertTrue(session.editFigureCaption());
        assertEquals(EditorFocusOwner.FIGURE_CAPTION, session.focusOwner());
        assertDepth(session, 0, 0);
        assertValid(session);
    }

    @Test
    void nestedTextInputMutatesOnlyTheFocusedDomain() {
        var document = mixedDocument();
        var session = new EditorSession(document, 0);

        session.setCurrent(state(document, new TableEditingSelection(2,
                TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 1))));
        assertTrue(session.typeText("Z"));
        assertEquals("aZb", tableCellText(session.current().document(), 2, 0, 0));
        assertEquals("alpha", paragraphText(session.current().document(), 0));
        assertEquals(6, session.current().document().blocks().size());
        assertEquals(EditorFocusOwner.TABLE, session.focusOwner());
        assertDepth(session, 1, 0);

        session.undo();
        session.setCurrent(state(document, FigureCaptionSelection.caret(5, 3)));
        assertTrue(session.typeText("!"));
        assertEquals("cap!tion", captionText(session.current().document(), 5));
        assertEquals("alpha", paragraphText(session.current().document(), 0));
        assertEquals(EditorFocusOwner.FIGURE_CAPTION, session.focusOwner());
        assertValid(session);
    }

    @Test
    void selectAllTargetsOnlyTheCurrentInputScope() {
        var document = mixedDocument();
        var session = new EditorSession(document, 0);

        session.selectAll();
        assertEquals(new TextSelection(new DocumentPosition(0, 0), new DocumentPosition(0, 5)), session.current().selection());

        session.setCurrent(state(document, new EquationEditingSelection(1,
                new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1)))));
        session.selectAll();
        var mathSelection = assertInstanceOf(MathRangeSelection.class, session.current().equationEditingSelection().selection());
        assertEquals(new MathSequencePosition(MathPath.ROOT, 0), mathSelection.anchor());
        assertEquals(new MathSequencePosition(MathPath.ROOT, 3), mathSelection.active());

        session.setCurrent(state(document, new TableEditingSelection(2,
                TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 1))));
        session.selectAll();
        assertEquals(new TableEditingSelection(2, new TableCellTextSelection(new TableCellCoordinate(0, 0), 0, 2)),
                session.current().selection());

        session.setCurrent(state(document, FigureCaptionSelection.caret(5, 2)));
        session.selectAll();
        assertEquals(new FigureCaptionSelection(5, 0, 7), session.current().selection());
        assertDepth(session, 0, 0);
        assertValid(session);
    }

    @Test
    void tableTabTraversalIsTransientFocusMovement() {
        var document = mixedDocument();
        var session = new EditorSession(document, 0);
        session.setCurrent(state(document, new TableEditingSelection(2,
                TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 1))));

        session.moveNextTableCell();
        assertEquals(new TableCellCoordinate(0, 1), session.current().tableEditingSelection().selection().cell());
        session.movePreviousTableCell();
        assertEquals(new TableCellCoordinate(0, 0), session.current().tableEditingSelection().selection().cell());
        assertDepth(session, 0, 0);
        assertEquals(document, session.current().document());
    }

    @Test
    void undoRedoRestoreFocusedNestedSelectionsWithoutStaleTransientState() {
        var document = mixedDocument();
        var session = new EditorSession(document, 0);
        session.setCurrent(state(document, new TableEditingSelection(2,
                TableCellTextSelection.caret(new TableCellCoordinate(0, 0), 1))));

        assertTrue(session.typeText("Z"));
        assertEquals(EditorFocusOwner.TABLE, session.focusOwner());
        var edited = session.current();

        assertTrue(session.undo());
        assertEquals(EditorFocusOwner.TABLE, session.focusOwner());
        assertEquals("ab", tableCellText(session.current().document(), 2, 0, 0));
        assertTrue(session.redo());
        assertEquals(edited, session.current());

        session.setCurrent(state(session.current().document(), new DiagramEditingSelection(4,
                elementTarget(session.current().document(), 4, 0))));
        assertTrue(session.beginDiagramElementDrag(11, 11));
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 0)));
        assertFalse(session.isDiagramElementDragging());
        assertEquals(EditorFocusOwner.DOCUMENT_TEXT, session.focusOwner());
        assertValid(session);
    }

    @Test
    void randomizedInputLikeSequenceKeepsSelectionValidAndHistoryDeterministic() {
        var document = mixedDocument();
        var session = new EditorSession(document, 0);

        for (var step = 0; step < 20; step++) {
            switch (step % 8) {
                case 0 -> session.moveRight();
                case 1 -> session.extendRight();
                case 2 -> session.selectAll();
                case 3 -> session.setCurrent(state(session.current().document(), new BlockSelection(2)));
                case 4 -> session.enter();
                case 5 -> session.moveNextTableCell();
                case 6 -> session.exitTableEditing();
                default -> session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 0)));
            }
            assertValid(session);
        }

        var beforeUndoDepth = session.undoDepth();
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 0)));
        assertTrue(session.typeText("x"));
        assertEquals(beforeUndoDepth + 1, session.undoDepth());
        assertTrue(session.undo());
        assertTrue(session.redo());
        assertValid(session);
    }

    private void enterBlock(EditorSession session, Document document, int blockIndex, EditorFocusOwner expectedOwner) {
        session.setCurrent(state(document, new BlockSelection(blockIndex)));
        session.enter();
        assertEquals(expectedOwner, session.focusOwner());
        assertValid(session);
    }

    private void assertValid(EditorSession session) {
        var validation = DocumentValidator.validate(session.current().document());
        assertTrue(validation.errors().isEmpty(), () -> validation.errors().toString());
        selectionValidator.validate(session.current().document(), session.current().selection());
    }

    private static void assertDepth(EditorSession session, int undoDepth, int redoDepth) {
        assertEquals(undoDepth, session.undoDepth());
        assertEquals(redoDepth, session.redoDepth());
    }

    private static EditorState state(Document document, EditorSelection selection) {
        return new EditorState(document, selection, Optional.empty());
    }

    private static Document mixedDocument() {
        return document(
                paragraph("alpha"),
                new EquationBlock(new MathSequence(List.of(
                        new MathIdentifier("x"),
                        new MathOperator("+", MathOperatorRole.BINARY),
                        new MathIdentifier("y")))),
                new TableBlock(List.of(new TableRow(List.of(cell("ab"), cell("cd")))), 0),
                plot(),
                diagram(),
                new FigureBlock("fig-1", plot(), inline("caption")));
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(inline(text));
    }

    private static InlineContent inline(String text) {
        return new InlineContent(List.of((InlineNode) new Text(text, Set.of())));
    }

    private static TableCell cell(String text) {
        return new TableCell(new TableCellContent(inline(text)));
    }

    private static PlotBlock plot() {
        return new PlotBlock(PlotDefinition.of(
                "Plot",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("series", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0))))));
    }

    private static DiagramBlock diagram() {
        var id = new DiagramElementId("node");
        return new DiagramBlock(new DiagramDefinition(
                "Diagram",
                new DiagramCanvas(80, 48),
                List.of(new DiagramNode(id, new DiagramBounds(8, 8, 24, 16), "Node", List.of())),
                List.of()));
    }

    private static DiagramElementTarget elementTarget(Document document, int blockIndex, int elementIndex) {
        var diagram = (DiagramBlock) document.blocks().get(blockIndex);
        return new DiagramElementTarget(elementIndex, diagram.definition().elements().get(elementIndex).id());
    }

    private static String paragraphText(Document document, int blockIndex) {
        return InlineContentEditor.logicalText(((Paragraph) document.blocks().get(blockIndex)).content());
    }

    private static String tableCellText(Document document, int blockIndex, int row, int column) {
        var table = (TableBlock) document.blocks().get(blockIndex);
        return InlineContentEditor.logicalText(table.rows().get(row).cells().get(column).content().content());
    }

    private static String captionText(Document document, int blockIndex) {
        return InlineContentEditor.logicalText(((FigureBlock) document.blocks().get(blockIndex)).caption());
    }
}
