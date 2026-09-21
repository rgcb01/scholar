package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetPlotBinding;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetTableBinding;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;

final class ExtractionFixtures {
    private ExtractionFixtures() {}

    static InlineContent inline(InlineNode... nodes) { return new InlineContent(List.of(nodes)); }

    static ScientificDataset dataset(String id) {
        return new ScientificDataset(id, "Data", List.of(new DatasetColumn("x", "X", DatasetColumnType.NUMBER),
                new DatasetColumn("y", "Y", DatasetColumnType.NUMBER)),
                List.of(new DatasetRow("row", List.of(DatasetValue.number("1"), DatasetValue.number("2")))));
    }

    static TableBlock boundTable(String id) {
        return new TableBlock(new DatasetTableBinding(id, List.of("x"))).withId("table");
    }

    static PlotBlock plot(String... datasetIds) {
        var series = datasetIds.length == 0 ? List.of(new PlotSeries("Static", PlotSeriesKind.SCATTER, List.of(new DataPoint(1, 3))))
                : java.util.Arrays.stream(datasetIds).map(id -> new PlotSeries("Bound", PlotSeriesKind.LINE, new DatasetPlotBinding(id, "x", "y"))).toList();
        return new PlotBlock(PlotDefinition.of("Plot", AxisDefinition.linear("x"), AxisDefinition.linear("y"), series));
    }

    static DiagramBlock diagram() {
        var portId = new DiagramPortId("port");
        var ports = List.of(new DiagramPort(portId, "Port", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)));
        var a = new DiagramNode(new DiagramElementId("a"), new DiagramBounds(10, 10, 20, 20), "A", ports);
        var b = new DiagramNode(new DiagramElementId("b"), new DiagramBounds(60, 10, 20, 20), "B", ports);
        var connection = new DiagramConnection(new DiagramEndpoint(a.id(), portId), new DiagramEndpoint(b.id(), portId), "Wire");
        return new DiagramBlock(new DiagramDefinition("Graph", new DiagramCanvas(100, 100), List.of(a, b), List.of(connection)), 0.6);
    }
}
