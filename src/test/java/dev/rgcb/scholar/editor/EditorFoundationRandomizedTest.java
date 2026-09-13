package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.math.editor.MathCaretSelection;
import dev.rgcb.scholar.math.editor.MathPath;
import dev.rgcb.scholar.math.editor.MathSequencePosition;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class EditorFoundationRandomizedTest {
    private final EditorSelectionValidator selectionValidator = new EditorSelectionValidator();

    @Test
    void seededCrossSubsystemEditingReplayIsDeterministicAndValid() {
        var first = replay(2407L);
        var second = replay(2407L);

        assertEquals(first.current(), second.current());
        assertEquals(first.undoDepth(), second.undoDepth());
        assertEquals(first.redoDepth(), second.redoDepth());
    }

    private EditorSession replay(long seed) {
        var session = new EditorSession(EditorFoundationFixture.canonicalDocument(), 0);
        var random = new Random(seed);

        for (var step = 0; step < 80; step++) {
            switch (random.nextInt(10)) {
                case 0 -> typeInHeading(session, random);
                case 1 -> toggleParagraphMark(session);
                case 2 -> typeInEquation(session, random);
                case 3 -> insertEquationStructure(session, random);
                case 4 -> typeInTable(session, random);
                case 5 -> mutatePlot(session, random);
                case 6 -> mutateDiagram(session);
                case 7 -> mutateDataset(session, random);
                case 8 -> maybeUndo(session);
                case 9 -> maybeRedo(session);
                default -> throw new AssertionError("Unexpected random branch");
            }
            assertValid(session);
        }

        return session;
    }

    private void typeInHeading(EditorSession session, Random random) {
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 4)));
        assertTrue(session.typeText(String.valueOf((char) ('a' + random.nextInt(5)))));
    }

    private void toggleParagraphMark(EditorSession session) {
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(4, 0), new DocumentPosition(4, 8)));
        assertTrue(session.toggleMark(TextMark.BOLD));
    }

    private void typeInEquation(EditorSession session, Random random) {
        session.setCurrent(state(session.current().document(), new BlockSelection(indexOf(session.current().document(), EquationBlock.class))));
        session.enter();
        session.setEquationEditingSelection(new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0)));
        assertTrue(session.typeText(String.valueOf((char) ('x' + random.nextInt(3)))));
    }

    private void insertEquationStructure(EditorSession session, Random random) {
        session.setCurrent(state(session.current().document(), new BlockSelection(indexOf(session.current().document(), EquationBlock.class))));
        session.enter();
        session.setEquationEditingSelection(new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0)));
        assertTrue(random.nextBoolean() ? session.insertFraction() : session.insertRoot());
    }

    private void typeInTable(EditorSession session, Random random) {
        session.setCurrent(state(session.current().document(), new TableEditingSelection(indexOf(session.current().document(), TableBlock.class),
                TableCellTextSelection.caret(new TableCellCoordinate(1, 1), 0))));
        assertTrue(session.typeText(Integer.toString(random.nextInt(10))));
    }

    private void mutatePlot(EditorSession session, Random random) {
        session.setCurrent(state(session.current().document(), new PlotEditingSelection(indexOf(session.current().document(), PlotBlock.class),
                new PlotSeriesTarget(0))));
        assertTrue(random.nextBoolean() ? session.addPlotPoint() : session.addPlotSeries(PlotSeriesKind.SCATTER));
    }

    private void mutateDiagram(EditorSession session) {
        session.setCurrent(state(session.current().document(), new DiagramEditingSelection(indexOf(session.current().document(), DiagramBlock.class),
                new DiagramPropertyTarget(DiagramProperty.CANVAS))));
        assertTrue(session.addDiagramNode());
    }

    private void mutateDataset(EditorSession session, Random random) {
        assertTrue(session.editDatasetCell("projectile", 1, "height", DatasetValue.number(Integer.toString(1 + random.nextInt(9)))));
    }

    private void maybeUndo(EditorSession session) {
        if (session.canUndo()) {
            assertTrue(session.undo());
        }
    }

    private void maybeRedo(EditorSession session) {
        if (session.canRedo()) {
            assertTrue(session.redo());
        }
    }

    private void assertValid(EditorSession session) {
        var result = DocumentValidator.validate(session.current().document());
        assertTrue(result.errors().isEmpty(), () -> result.errors().toString());
        selectionValidator.validate(session.current().document(), session.current().selection());
    }

    private static EditorState state(Document document, EditorSelection selection) {
        return new EditorState(document, selection, Optional.empty());
    }

    private static int indexOf(Document document, Class<?> type) {
        for (var index = 0; index < document.blocks().size(); index++) {
            if (type.isInstance(document.blocks().get(index))) {
                return index;
            }
        }
        throw new AssertionError("Missing block type " + type.getSimpleName());
    }
}
