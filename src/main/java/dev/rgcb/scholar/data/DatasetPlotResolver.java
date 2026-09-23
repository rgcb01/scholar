package dev.rgcb.scholar.data;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.analysis.DatasetAnalysisEngine;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.quantity.Quantity;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitExpression;
import dev.rgcb.scholar.quantity.UnitRegistry;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.MathContext;

public final class DatasetPlotResolver {
    private final DatasetRegistry registry = new DatasetRegistry();
    private final DatasetAnalysisEngine analyses = new DatasetAnalysisEngine();

    public PlotBlock resolve(Document document, PlotBlock plot) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(plot, "plot");
        var updatedSeries = new ArrayList<PlotSeries>();
        for (var series : plot.definition().series()) {
            updatedSeries.add(resolveSeries(document, series, plot.definition().xAxis(), plot.definition().yAxis()));
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
        return resolveSeries(document, series, AxisDefinition.linear("X"), AxisDefinition.linear("Y"));
    }

    private PlotSeries resolveSeries(Document document, PlotSeries series,
                                     AxisDefinition xAxis, AxisDefinition yAxis) {
        if (series.fitAnalysisId().isPresent()) {
            return resolveFit(document, series, xAxis, yAxis);
        }
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
            if (x.isPresent() && y.isPresent() && Double.isFinite(x.orElseThrow()) && Double.isFinite(y.orElseThrow())) {
                points.add(new DataPoint(convert(x.orElseThrow(), resolved.columns().get(xIndex), xAxis),
                        convert(y.orElseThrow(), resolved.columns().get(yIndex), yAxis)));
            }
        }
        return series.withPoints(points);
    }

    private PlotSeries resolveFit(Document document, PlotSeries series,
                                  AxisDefinition xAxis, AxisDefinition yAxis) {
        var analysis = document.blocks().stream().filter(DatasetAnalysisBlock.class::isInstance)
                .map(DatasetAnalysisBlock.class::cast)
                .filter(block -> block.id().equals(series.fitAnalysisId().orElseThrow()))
                .findFirst();
        if (analysis.isEmpty() || !analysis.orElseThrow().kind().isFit()) return series.withPoints(java.util.List.of());
        var definition = analysis.orElseThrow();
        var outcome = analyses.evaluate(document, definition);
        if (outcome.result().isEmpty()) return series.withPoints(java.util.List.of());
        var result = outcome.result().orElseThrow();
        var dataset = registry.find(document, definition.datasetId()).orElseThrow();
        var xColumn = dataset.column(definition.xColumnId().orElseThrow()).orElseThrow();
        var yColumn = dataset.column(definition.yColumnId()).orElseThrow();
        var sourceYUnit = definition.displayUnit().or(() -> yColumn.unit());
        var effectiveYAxis = yAxis.displayUnit().isEmpty() && yColumn.unit().isPresent()
                ? yAxis.withDisplayUnit(yColumn.unit().orElseThrow(), yColumn.quantitySemantics()) : yAxis;
        var points = new ArrayList<DataPoint>();
        var minimum = result.xMinimum().orElseThrow();
        var range = result.xMaximum().orElseThrow().subtract(minimum);
        for (var index = 0; index <= 64; index++) {
            var x = minimum.add(range.multiply(BigDecimal.valueOf(index))
                    .divide(BigDecimal.valueOf(64), MathContext.DECIMAL128));
            var y = result.predict(x);
            try {
                var displayX = fitCoordinate(x, xColumn.unit(), xColumn.quantitySemantics(), xAxis);
                var displayY = fitCoordinate(y, sourceYUnit, yColumn.quantitySemantics(), effectiveYAxis);
                if (Double.isFinite(displayX) && Double.isFinite(displayY)) points.add(new DataPoint(displayX, displayY));
            } catch (IllegalArgumentException ignored) {
                return series.withPoints(java.util.List.of());
            }
        }
        return series.withPoints(points);
    }

    private static double fitCoordinate(BigDecimal value, Optional<UnitExpression> unit,
                                        QuantitySemantics semantics, AxisDefinition axis) {
        if (axis.displayUnit().isEmpty()) return value.doubleValue();
        if (unit.isEmpty() || axis.displayUnitSemantics().orElse(semantics) != semantics) {
            throw new IllegalArgumentException("Incompatible fit axis unit.");
        }
        return new Quantity(value, unit.orElseThrow(), semantics).convertTo(axis.displayUnit().orElseThrow())
                .value().doubleValue();
    }

    private AxisDefinition resolvedAxis(Document document, AxisDefinition axis, java.util.List<PlotSeries> series, boolean x) {
        var sourceColumn = series.stream().map(value -> {
                    if (value.datasetBinding().isPresent()) {
                        var binding = value.datasetBinding().orElseThrow();
                        return registry.find(document, binding.datasetId()).flatMap(dataset ->
                                dataset.column(x ? binding.xColumnId() : binding.yColumnId()));
                    }
                    return value.fitAnalysisId().flatMap(id -> document.blocks().stream()
                            .filter(block -> block instanceof DatasetAnalysisBlock analysis && analysis.id().equals(id))
                            .map(block -> (DatasetAnalysisBlock) block).findFirst())
                            .flatMap(analysis -> registry.find(document, analysis.datasetId()).flatMap(dataset ->
                                    dataset.column(x ? analysis.xColumnId().orElse("") : analysis.yColumnId())));
                }).flatMap(Optional::stream).filter(column -> column.unit().isPresent()).findFirst();
        var unit = axis.displayUnit().or(() -> sourceColumn.flatMap(DatasetColumn::unit));
        var semantics = axis.displayUnitSemantics().or(() -> sourceColumn.map(DatasetColumn::quantitySemantics));
        var label = axis.label() + unit.map(value -> " (" + semanticUnit(semantics.orElse(QuantitySemantics.LINEAR), value) + ")").orElse("");
        return new AxisDefinition(label, axis.explicitRange(), axis.scale(), axis.displayUnit(), axis.displayUnitSemantics());
    }

    private static double convert(double value, DatasetColumn source, AxisDefinition target) {
        if (source.unit().isEmpty() || target.displayUnit().isEmpty()) return value;
        if (target.displayUnitSemantics().orElseThrow() != source.quantitySemantics()) return value;
        if (!source.unit().orElseThrow().compatibleWith(target.displayUnit().orElseThrow(), UnitRegistry.builtIn())) return value;
        return new Quantity(java.math.BigDecimal.valueOf(value), source.unit().orElseThrow(), source.quantitySemantics())
                .convertTo(target.displayUnit().orElseThrow()).value().doubleValue();
    }

    private static String semanticUnit(QuantitySemantics semantics, UnitExpression unit) {
        var symbol = unit.displaySymbol(UnitRegistry.builtIn());
        return semantics == QuantitySemantics.TEMPERATURE_DIFFERENCE ? "Δ" + symbol : symbol;
    }
}
