package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.FigureBlock;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScientificContentCreationPolicyTest {
    @Test
    void tablePlotDiagramAndDatasetDefaultsRemainValidAndDeterministic() {
        var table = ScientificContentCreationPolicy.table();
        assertEquals(ScientificContentCreationPolicy.DEFAULT_TABLE_ROWS, table.rows().size());
        assertEquals(ScientificContentCreationPolicy.DEFAULT_TABLE_COLUMNS, table.columnCount());

        var firstPlot = ScientificContentCreationPolicy.plot();
        var secondPlot = ScientificContentCreationPolicy.plot();
        assertNotSame(firstPlot, secondPlot);
        assertEquals(firstPlot, secondPlot);
        assertEquals(1, firstPlot.definition().series().size());

        var diagram = ScientificContentCreationPolicy.diagram();
        assertTrue(diagram.definition().canvas().width() > 0);
        assertTrue(diagram.definition().canvas().height() > 0);
        assertTrue(diagram.definition().elements().isEmpty());

        var dataset = ScientificContentCreationPolicy.dataset("dataset-1");
        assertEquals("dataset-1", dataset.id());
        assertEquals(List.of("x", "y"), dataset.columns().stream().map(column -> column.id()).toList());
        assertTrue(dataset.rows().isEmpty());
    }

    @Test
    void figurePolicyCreatesAnEmptyCaptionWithoutChangingContent() {
        var content = ScientificContentCreationPolicy.plot();

        FigureBlock figure = ScientificContentCreationPolicy.figure("figure-1", content);

        assertEquals("figure-1", figure.id());
        assertEquals(content, figure.content());
        assertTrue(figure.caption().nodes().isEmpty());
    }
}
