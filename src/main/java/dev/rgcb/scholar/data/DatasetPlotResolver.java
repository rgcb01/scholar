package dev.rgcb.scholar.data;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.quantity.Quantity;
import dev.rgcb.scholar.quantity.UnitExpression;
import dev.rgcb.scholar.quantity.UnitRegistry;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public final class DatasetPlotResolver {
    private final DatasetRegistry registry = new DatasetRegistry();

    public PlotBlock resolve(Document document, PlotBlock plot) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(plot, "plot");
        var updatedSeries = new ArrayList<PlotSeries>();
        for (var series : plot.definition().series()) {
            updatedSeries.add(resolveSeries(document, series, plot.definition().xAxis().displayUnit(),
                    plot.definition().yAxis().displayUnit()));
        }
        var definition = plot.definition();
        return new PlotBlock(new PlotDefinition(
                definition.title(),
                resolvedAxis(document, definition.xAxis(), definition.series(), true),
                resolvedAxis(document, definition.yAxis(), definition.series(), false),
                updatedSeries,
                definition.legendVisible(),
                definition.gridVisible(),
                definition.height()));
    }

    public PlotSeries resolveSeries(Document document, PlotSeries series) {
        return resolveSeries(document, series, Optional.empty(), Optional.empty());
    }

    private PlotSeries resolveSeries(Document document, PlotSeries series,
                                     Optional<UnitExpression> xDisplayUnit, Optional<UnitExpression> yDisplayUnit) {
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
        var xUnit = resolved.columns().get(xIndex).unit();
        var yUnit = resolved.columns().get(yIndex).unit();
        for (var row : resolved.rows()) {
            var x = row.values().get(xIndex).asDouble();
            var y = row.values().get(yIndex).asDouble();
            if (x.isPresent() && y.isPresent() && Double.isFinite(x.orElseThrow()) && Double.isFinite(y.orElseThrow())) {
                points.add(new DataPoint(convert(x.orElseThrow(), xUnit, xDisplayUnit),
                        convert(y.orElseThrow(), yUnit, yDisplayUnit)));
            }
        }
        return series.withPoints(points);
    }

    private AxisDefinition resolvedAxis(Document document, AxisDefinition axis, java.util.List<PlotSeries> series, boolean x) {
        var unit = axis.displayUnit().or(() -> series.stream().map(PlotSeries::datasetBinding)
                .flatMap(Optional::stream)
                .map(binding -> registry.find(document, binding.datasetId())
                        .flatMap(dataset -> dataset.column(x ? binding.xColumnId() : binding.yColumnId()))
                        .flatMap(DatasetColumn::unit))
                .flatMap(Optional::stream).findFirst());
        var label = axis.label() + unit.map(value -> " (" + value.displaySymbol(UnitRegistry.builtIn()) + ")").orElse("");
        return new AxisDefinition(label, axis.explicitRange(), axis.scale(), axis.displayUnit());
    }

    private static double convert(double value, Optional<UnitExpression> source, Optional<UnitExpression> target) {
        if (source.isEmpty() || target.isEmpty()) return value;
        if (!source.orElseThrow().compatibleWith(target.orElseThrow(), UnitRegistry.builtIn())) return value;
        return new Quantity(java.math.BigDecimal.valueOf(value), source.orElseThrow())
                .convertTo(target.orElseThrow()).value().doubleValue();
    }
}
