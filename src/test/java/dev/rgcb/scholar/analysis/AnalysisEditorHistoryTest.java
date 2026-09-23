package dev.rgcb.scholar.analysis;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.data.*;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.editor.BlockSelection;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.editor.EditorState;
import dev.rgcb.scholar.plot.*;
import dev.rgcb.scholar.quantity.NumberNotation;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnalysisEditorHistoryTest {
    private static ScientificDataset dataset() {
        return new ScientificDataset("d", "Data", List.of(
                new DatasetColumn("x", "X", DatasetColumnType.NUMBER),
                new DatasetColumn("y", "Y", DatasetColumnType.NUMBER)), List.of(
                new DatasetRow(List.of(DatasetValue.number("0"), DatasetValue.number("1"))),
                new DatasetRow(List.of(DatasetValue.number("1"), DatasetValue.number("3"))),
                new DatasetRow(List.of(DatasetValue.number("2"), DatasetValue.number("5")))));
    }

    @Test void insertEditOverlayAndUndoRedoKeepAuthoredIdentity() {
        var plot = new PlotBlock(PlotDefinition.of("Data", AxisDefinition.linear("X"), AxisDefinition.linear("Y"),
                List.of(new PlotSeries("Data", PlotSeriesKind.SCATTER, new DatasetPlotBinding("d", "x", "y")))));
        var session = new EditorSession(new Document(List.of(new Paragraph(new InlineContent(List.of(new Text("", java.util.Set.of())))), plot), List.of(dataset())), 0);
        assertTrue(session.insertAnalysis("d", AnalysisKind.LINEAR_REGRESSION, Optional.of("x"), "y", Optional.empty(), NumberNotation.DECIMAL));
        assertEquals(1, session.undoDepth());
        var analysisIndex = java.util.stream.IntStream.range(0, session.current().document().blocks().size())
                .filter(i -> session.current().document().blocks().get(i) instanceof DatasetAnalysisBlock).findFirst().orElseThrow();
        var plotIndex = java.util.stream.IntStream.range(0, session.current().document().blocks().size())
                .filter(i -> session.current().document().blocks().get(i) instanceof PlotBlock).findFirst().orElseThrow();
        var analysis = (DatasetAnalysisBlock) session.current().document().blocks().get(analysisIndex);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(analysisIndex), Optional.empty()));
        assertTrue(session.editAnalysis("d", AnalysisKind.LINEAR_REGRESSION, Optional.of("x"), "y",
                Optional.empty(), NumberNotation.SCIENTIFIC));
        assertEquals(2, session.undoDepth());
        assertEquals(analysis.id(), ((DatasetAnalysisBlock) session.current().document().blocks().get(analysisIndex)).id());
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(plotIndex), Optional.empty()));
        assertTrue(session.addFitOverlay(analysis.id()));
        assertEquals(3, session.undoDepth());
        var withFit = session.current().document();
        assertEquals(0, new DatasetAnalysisEngine().evaluate(withFit, (DatasetAnalysisBlock) withFit.blocks().get(analysisIndex))
                .result().orElseThrow().coefficients().get(1).value().compareTo(new java.math.BigDecimal("2")));
        assertEquals(3, session.undoDepth());
        assertTrue(session.undo());
        assertEquals(1, ((PlotBlock) session.current().document().blocks().get(plotIndex)).definition().series().size());
        assertTrue(session.redo());
        assertEquals(withFit, session.current().document());
        assertEquals(analysis.id(), ((PlotBlock) session.current().document().blocks().get(plotIndex))
                .definition().series().get(1).fitAnalysisId().orElseThrow());
    }
}
