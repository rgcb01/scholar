package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathOperatorRole;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.editor.MathRangeSelection;
import dev.rgcb.scholar.math.editor.MathSequencePosition;
import dev.rgcb.scholar.math.editor.MathPath;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EditorSelectionCaretContractTest {
    private final EditorSelectionValidator validator = new EditorSelectionValidator();

    @Test
    void selectAllTargetsContiguousEditableTextScopeOnly() {
        var document = document(
                paragraph("abc"),
                heading("def"),
                equation(),
                paragraph("ghi"));
        var session = new EditorSession(document, 0);

        session.selectAll();
        assertEquals(new TextSelection(new DocumentPosition(0, 0), new DocumentPosition(1, 3)), session.current().selection());

        session.setCurrent(new EditorState(document, new DocumentPosition(3, 1)));
        session.selectAll();
        assertEquals(new TextSelection(new DocumentPosition(3, 0), new DocumentPosition(3, 3)), session.current().selection());
    }

    @Test
    void selectAllTargetsCurrentEquationRoot() {
        var expression = new MathSequence(List.of(
                new MathIdentifier("x"),
                new MathOperator("+", MathOperatorRole.BINARY),
                new MathNumber("1")));
        var document = document(paragraph("before"), new EquationBlock(expression), paragraph("after"));
        var session = new EditorSession(document, 0);
        session.moveRight();
        session.enter();

        session.selectAll();

        var selection = assertInstanceOf(MathRangeSelection.class, session.current().equationEditingSelection().selection());
        assertEquals(new MathSequencePosition(MathPath.ROOT, 0), selection.anchor());
        assertEquals(new MathSequencePosition(MathPath.ROOT, 3), selection.active());
    }

    @Test
    void selectAllTargetsCurrentTableCellOnly() {
        var document = document(paragraph("before"), table("abc", "def"), paragraph("after"));
        var session = new EditorSession(document, 0);
        session.moveRight();
        session.enter();

        session.selectAll();

        assertEquals(new TableEditingSelection(
                1,
                new TableCellTextSelection(new TableCellCoordinate(0, 0), 0, 3)),
                session.current().selection());
    }

    @Test
    void selectAllTargetsFigureCaptionOnly() {
        var document = document(paragraph("before"), figure("caption"), paragraph("after"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, FigureCaptionSelection.caret(1, 2), java.util.Optional.empty()));

        session.selectAll();

        assertEquals(new FigureCaptionSelection(1, 0, 7), session.current().selection());
    }

    @Test
    void selectAllIsNoOpForAtomicBlockSelection() {
        var document = document(paragraph("before"), new TableOfContentsBlock(), paragraph("after"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), java.util.Optional.empty()));

        session.selectAll();

        assertEquals(new BlockSelection(1), session.current().selection());
    }

    @Test
    void goldenHorizontalNavigationTraversesMixedDocumentWithoutInvalidSelections() {
        var document = mixedNavigationDocument();
        var session = new EditorSession(document, 0);
        var visitedBlocks = new HashSet<Integer>();

        for (var guard = 0; guard < 100; guard++) {
            session.moveRight();
            assertTrue(validator.isValid(session.current()), "invalid selection after moveRight: " + session.current().selection());
            if (session.current().selection() instanceof BlockSelection blockSelection) {
                visitedBlocks.add(blockSelection.blockIndex());
            }
            if (session.current().isTextSelection() && session.current().active().blockIndex() == document.blocks().size() - 1) {
                break;
            }
        }

        assertEquals(Set.of(1, 2, 4, 5, 6, 7), visitedBlocks);

        for (var guard = 0; guard < 100; guard++) {
            session.moveLeft();
            assertTrue(validator.isValid(session.current()), "invalid selection after moveLeft: " + session.current().selection());
            if (session.current().isTextSelection() && session.current().active().equals(new DocumentPosition(0, 0))) {
                return;
            }
        }
        throw new AssertionError("navigation did not return to the document start");
    }

    @Test
    void randomizedHorizontalNavigationPreservesValidityAndDocument() {
        var document = mixedNavigationDocument();
        var session = new EditorSession(document, 0);
        var random = new Random(24);

        for (var step = 0; step < 200; step++) {
            switch (random.nextInt(4)) {
                case 0 -> session.moveLeft();
                case 1 -> session.moveRight();
                case 2 -> session.extendLeft();
                default -> session.extendRight();
            }
            assertEquals(document, session.current().document());
            assertTrue(validator.isValid(session.current()), "invalid selection at step " + step + ": " + session.current().selection());
        }
    }

    @Test
    void crossReferenceContributesOneLogicalCaretUnit() {
        var document = document(paragraph(
                text("A"),
                new CrossReference(CrossReferenceTargetKind.FIGURE, "fig-1"),
                text("B")));
        var session = new EditorSession(document, 0);

        session.setCurrent(new EditorState(document, new DocumentPosition(0, 1)));
        session.moveRight();

        assertEquals(new TextSelection(new DocumentPosition(0, 2), new DocumentPosition(0, 2)), session.current().selection());
    }

    private static Document mixedNavigationDocument() {
        return document(
                paragraph("alpha"),
                new TableOfContentsBlock(),
                equation(),
                heading("Editable heading"),
                table("h", "v"),
                plot(),
                diagram(),
                figure("caption"),
                paragraph("omega"));
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(inline(text));
    }

    private static Paragraph paragraph(InlineNode... nodes) {
        return new Paragraph(new InlineContent(List.of(nodes)));
    }

    private static Heading heading(String text) {
        return new Heading(2, inline(text));
    }

    private static InlineNode text(String text) {
        return new Text(text, Set.of());
    }

    private static InlineContent inline(String text) {
        return new InlineContent(List.of(text(text)));
    }

    private static EquationBlock equation() {
        return new EquationBlock(new MathIdentifier("x"));
    }

    private static TableBlock table(String first, String second) {
        return new TableBlock(List.of(new TableRow(List.of(cell(first), cell(second)))), 0);
    }

    private static TableCell cell(String text) {
        return new TableCell(new TableCellContent(inline(text)));
    }

    private static PlotBlock plot() {
        return new PlotBlock(PlotDefinition.of(
                "Plot",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("s", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0))))));
    }

    private static DiagramBlock diagram() {
        return new DiagramBlock(new DiagramDefinition("Diagram", new DiagramCanvas(40, 30), List.of(), List.of()));
    }

    private static FigureBlock figure(String caption) {
        return new FigureBlock("fig-1", plot(), inline(caption));
    }
}
