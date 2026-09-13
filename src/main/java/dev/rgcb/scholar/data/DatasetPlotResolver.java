package dev.rgcb.scholar.data;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import java.util.ArrayList;
import java.util.Objects;

public final class DatasetPlotResolver {
    private final DatasetRegistry registry = new DatasetRegistry();

    public PlotBlock resolve(Document document, PlotBlock plot) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(plot, "plot");
        var updatedSeries = new ArrayList<PlotSeries>();
        for (var series : plot.definition().series()) {
            updatedSeries.add(resolveSeries(document, series));
        }
        var definition = plot.definition();
        return new PlotBlock(new PlotDefinition(
                definition.title(),
                definition.xAxis(),
                definition.yAxis(),
                updatedSeries,
                definition.legendVisible(),
                definition.gridVisible(),
                definition.height()));
    }

    public PlotSeries resolveSeries(Document document, PlotSeries series) {
        if (series.datasetBinding().isEmpty()) {
            return series;
        }
        var binding = series.datasetBinding().orElseThrow();
        var dataset = registry.find(document, binding.datasetId());
        if (dataset.isEmpty()) {
            return series.withPoints(java.util.List.of());
        }
        var resolved = dataset.orElseThrow();
        var xIndex = resolved.columnIndex(binding.xColumnId());
        var yIndex = resolved.columnIndex(binding.yColumnId());
        if (xIndex < 0 || yIndex < 0) {
            return series.withPoints(java.util.List.of());
        }
        var points = new ArrayList<DataPoint>();
        for (var row : resolved.rows()) {
            var x = row.values().get(xIndex).asDouble();
            var y = row.values().get(yIndex).asDouble();
            if (x.isPresent() && y.isPresent()) {
                points.add(new DataPoint(x.orElseThrow(), y.orElseThrow()));
            }
        }
        return series.withPoints(points);
    }
}
