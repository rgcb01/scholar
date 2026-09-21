package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.data.*;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.math.*;
import dev.rgcb.scholar.plot.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class M28ProductizationTest {
    @Test
    void equationBlockFallbackUsesReadableNestedMath() {
        var equation = new EquationBlock("eq", new MathSequence(List.of(
                new MathIdentifier("y"),
                new MathOperator("=", MathOperatorRole.RELATION),
                new MathFraction(new MathNumber("1"),
                        new MathRoot(new MathIdentifier("x"), Optional.of(new MathNumber("3")))))));
        var document = new Document(List.of(emptyParagraph(), equation));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));

        assertEquals("y = 1 / root(3, x)", session.copyForClipboard().orElseThrow().plainText());
    }

    @Test
    void datasetBackedPlotAndFigureFallbackContainResolvedPoints() {
        var dataset = new ScientificDataset("d", "Measurements",
                List.of(new DatasetColumn("x", "x", DatasetColumnType.NUMBER),
                        new DatasetColumn("y", "y", DatasetColumnType.NUMBER)),
                List.of(new DatasetRow(List.of(DatasetValue.number("1"), DatasetValue.number("2"))),
                        new DatasetRow(List.of(DatasetValue.number("3"), DatasetValue.number("4")))));
        var plot = new PlotBlock(PlotDefinition.of("Bound", AxisDefinition.linear("x"), AxisDefinition.linear("y"),
                List.of(new PlotSeries("observed", PlotSeriesKind.LINE, new DatasetPlotBinding("d", "x", "y")))));
        var figure = new FigureBlock("f", plot, new InlineContent(List.of(new Text("caption", java.util.Set.of()))));
        var document = new Document(List.of(emptyParagraph(), plot, figure), List.of(dataset));

        var plotCopy = copy(document, 1);
        var figureCopy = copy(document, 2);

        assertTrue(plotCopy.contains("1.0\t2.0"));
        assertTrue(plotCopy.contains("3.0\t4.0"));
        assertTrue(figureCopy.contains("Figure 1: caption"));
        assertTrue(figureCopy.contains("1.0\t2.0"));
    }

    @Test
    void productionInsertionDefaultsAreNeutralAndEmpty() {
        var base = new EditorState(new Document(List.of(new Paragraph(new InlineContent(List.of())))),
                new DocumentPosition(0, 0));
        var editor = new DocumentEditor();
        var plot = (PlotBlock) editor.insertDefaultPlot(base).editorState().document().blocks().get(0);
        var diagram = (DiagramBlock) editor.insertDefaultDiagram(base).editorState().document().blocks().get(0);
        var session = new EditorSession(base.document(), 0);

        assertEquals("Untitled Plot", plot.definition().title());
        assertTrue(plot.definition().series().stream().allMatch(series -> series.points().isEmpty()));
        assertEquals("Untitled Diagram", diagram.definition().title());
        assertTrue(diagram.definition().elements().isEmpty());
        assertTrue(diagram.definition().connections().isEmpty());
        assertTrue(session.createDefaultDataset());
        var dataset = session.current().document().datasets().getFirst();
        assertEquals("Untitled Dataset", dataset.displayLabel());
        assertTrue(dataset.rows().isEmpty());
    }

    private static String copy(Document document, int blockIndex) {
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(blockIndex), Optional.empty()));
        return session.copyForClipboard().orElseThrow().plainText();
    }

    private static Paragraph emptyParagraph() {
        return new Paragraph(new InlineContent(List.of()));
    }
}
