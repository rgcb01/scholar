package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import java.util.Objects;

/** Deterministic product policy for newly inserted scientific content. */
public final class ScientificContentCreationPolicy {
    static final int DEFAULT_TABLE_ROWS = 2;
    static final int DEFAULT_TABLE_COLUMNS = 2;
    static final double DEFAULT_DIAGRAM_WIDTH = 100.0;
    static final double DEFAULT_DIAGRAM_HEIGHT = 50.0;

    private ScientificContentCreationPolicy() {
    }

    public static TableBlock table() {
        return TableBlock.empty(DEFAULT_TABLE_ROWS, DEFAULT_TABLE_COLUMNS);
    }

    public static PlotBlock plot() {
        return new PlotBlock(PlotDefinition.of(
                "Untitled Plot",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("Series 1", PlotSeriesKind.LINE, List.of()))));
    }

    public static DiagramBlock diagram() {
        return new DiagramBlock(new DiagramDefinition(
                "Untitled Diagram",
                new DiagramCanvas(DEFAULT_DIAGRAM_WIDTH, DEFAULT_DIAGRAM_HEIGHT),
                List.of(),
                List.of()));
    }

    public static ScientificDataset dataset(String id) {
        return new ScientificDataset(
                Objects.requireNonNull(id, "id"),
                "Untitled Dataset",
                List.of(
                        new DatasetColumn("x", "x", DatasetColumnType.NUMBER),
                        new DatasetColumn("y", "y", DatasetColumnType.NUMBER)),
                List.of());
    }

    public static FigureBlock figure(String id, BlockNode content) {
        return new FigureBlock(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(content, "content"),
                new InlineContent(List.of()));
    }
}
