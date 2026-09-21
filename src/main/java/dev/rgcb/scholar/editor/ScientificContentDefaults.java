package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;

/** Minimal valid production defaults; rich examples belong to development fixtures. */
public final class ScientificContentDefaults {
    private ScientificContentDefaults() {
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
                new DiagramCanvas(100, 50),
                List.of(),
                List.of()));
    }

    public static ScientificDataset dataset(String id) {
        return new ScientificDataset(
                id,
                "Untitled Dataset",
                List.of(
                        new DatasetColumn("x", "x", DatasetColumnType.NUMBER),
                        new DatasetColumn("y", "y", DatasetColumnType.NUMBER)),
                List.of());
    }
}
