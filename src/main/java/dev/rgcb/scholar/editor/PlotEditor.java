package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure immutable editing operations for PlotBlock semantic state. */
public final class PlotEditor {
    public PlotEditTarget firstTarget(PlotBlock plot) {
        Objects.requireNonNull(plot, "plot");
        return new PlotPropertyTarget(PlotProperty.TITLE);
    }

    public void validateSelection(PlotBlock plot, PlotEditTarget target) {
        Objects.requireNonNull(plot, "plot");
        Objects.requireNonNull(target, "target");
        if (target instanceof PlotPropertyTarget) {
            return;
        }
        if (target instanceof PlotSeriesTarget seriesTarget) {
            series(plot, seriesTarget.seriesIndex());
            return;
        }
        if (target instanceof PlotPointTarget pointTarget) {
            var series = series(plot, pointTarget.seriesIndex());
            if (pointTarget.pointIndex() >= series.points().size()) {
                throw new IllegalArgumentException("Plot point selection is outside the selected series.");
            }
            return;
        }
        throw new IllegalArgumentException("Unsupported plot edit target: " + target.getClass().getName());
    }

    public PlotEditTarget nextTarget(PlotBlock plot, PlotEditTarget current) {
        return adjacentTarget(plot, current, 1);
    }

    public PlotEditTarget previousTarget(PlotBlock plot, PlotEditTarget current) {
        return adjacentTarget(plot, current, -1);
    }

    public String textValue(PlotBlock plot, PlotEditTarget target) {
        Objects.requireNonNull(plot, "plot");
        validateSelection(plot, target);
        var definition = plot.definition();
        if (target instanceof PlotPropertyTarget propertyTarget) {
            return switch (propertyTarget.property()) {
                case TITLE -> definition.title();
                case X_AXIS_LABEL -> definition.xAxis().label();
                case Y_AXIS_LABEL -> definition.yAxis().label();
            };
        }
        if (target instanceof PlotSeriesTarget seriesTarget) {
            return definition.series().get(seriesTarget.seriesIndex()).name();
        }
        return "";
    }

    public PlotEditResult setText(PlotBlock plot, PlotEditTarget target, String text) {
        Objects.requireNonNull(text, "text");
        validateSelection(plot, target);
        var definition = plot.definition();
        PlotDefinition updated;
        if (target instanceof PlotPropertyTarget propertyTarget) {
            updated = switch (propertyTarget.property()) {
                case TITLE -> copy(definition, text, definition.xAxis(), definition.yAxis(), definition.series(), definition.legendVisible(), definition.gridVisible());
                case X_AXIS_LABEL -> copy(definition, definition.title(), withLabel(definition.xAxis(), text), definition.yAxis(), definition.series(), definition.legendVisible(), definition.gridVisible());
                case Y_AXIS_LABEL -> copy(definition, definition.title(), definition.xAxis(), withLabel(definition.yAxis(), text), definition.series(), definition.legendVisible(), definition.gridVisible());
            };
        } else if (target instanceof PlotSeriesTarget seriesTarget) {
            var series = new ArrayList<>(definition.series());
            var old = series.get(seriesTarget.seriesIndex());
            series.set(seriesTarget.seriesIndex(), new PlotSeries(text, old.kind(), old.points()));
            updated = copy(definition, definition.title(), definition.xAxis(), definition.yAxis(), series, definition.legendVisible(), definition.gridVisible());
        } else {
            return new PlotEditResult(plot, target, false);
        }
        var changed = !updated.equals(definition);
        return new PlotEditResult(changed ? new PlotBlock(updated) : plot, target, changed);
    }

    public PlotEditResult setPoint(PlotBlock plot, PlotPointTarget target, double x, double y) {
        validateSelection(plot, target);
        var replacement = new DataPoint(x, y);
        var definition = plot.definition();
        var series = new ArrayList<>(definition.series());
        var oldSeries = series.get(target.seriesIndex());
        var points = new ArrayList<>(oldSeries.points());
        points.set(target.pointIndex(), replacement);
        series.set(target.seriesIndex(), new PlotSeries(oldSeries.name(), oldSeries.kind(), points));
        var updated = copy(definition, definition.title(), definition.xAxis(), definition.yAxis(), series, definition.legendVisible(), definition.gridVisible());
        var changed = !updated.equals(definition);
        return new PlotEditResult(changed ? new PlotBlock(updated) : plot, target, changed);
    }

    public PlotEditResult toggleGrid(PlotBlock plot, PlotEditTarget target) {
        validateSelection(plot, target);
        var d = plot.definition();
        var updated = copy(d, d.title(), d.xAxis(), d.yAxis(), d.series(), d.legendVisible(), !d.gridVisible());
        return new PlotEditResult(new PlotBlock(updated), target, true);
    }

    public PlotEditResult toggleLegend(PlotBlock plot, PlotEditTarget target) {
        validateSelection(plot, target);
        var d = plot.definition();
        var updated = copy(d, d.title(), d.xAxis(), d.yAxis(), d.series(), !d.legendVisible(), d.gridVisible());
        return new PlotEditResult(new PlotBlock(updated), target, true);
    }

    public PlotEditResult addSeries(PlotBlock plot, PlotEditTarget target, PlotSeriesKind kind) {
        Objects.requireNonNull(kind, "kind");
        validateSelection(plot, target);
        var d = plot.definition();
        var series = new ArrayList<>(d.series());
        var index = series.size();
        series.add(new PlotSeries("Series " + (index + 1), kind, List.of()));
        var updated = copy(d, d.title(), d.xAxis(), d.yAxis(), series, d.legendVisible(), d.gridVisible());
        return new PlotEditResult(new PlotBlock(updated), new PlotSeriesTarget(index), true);
    }

    public boolean canDeleteSeries(PlotBlock plot, PlotEditTarget target) {
        validateSelection(plot, target);
        return seriesIndex(target) >= 0;
    }

    public PlotEditResult deleteSeries(PlotBlock plot, PlotEditTarget target) {
        validateSelection(plot, target);
        var index = seriesIndex(target);
        if (index < 0) {
            return new PlotEditResult(plot, target, false);
        }
        var d = plot.definition();
        var series = new ArrayList<>(d.series());
        series.remove(index);
        PlotEditTarget next = series.isEmpty()
                ? new PlotPropertyTarget(PlotProperty.TITLE)
                : new PlotSeriesTarget(Math.min(index, series.size() - 1));
        var updated = copy(d, d.title(), d.xAxis(), d.yAxis(), series, d.legendVisible(), d.gridVisible());
        return new PlotEditResult(new PlotBlock(updated), next, true);
    }

    public boolean canSetSeriesKind(PlotBlock plot, PlotEditTarget target) {
        validateSelection(plot, target);
        return seriesIndex(target) >= 0;
    }

    public PlotEditResult setSeriesKind(PlotBlock plot, PlotEditTarget target, PlotSeriesKind kind) {
        Objects.requireNonNull(kind, "kind");
        validateSelection(plot, target);
        var index = seriesIndex(target);
        if (index < 0) {
            return new PlotEditResult(plot, target, false);
        }
        var d = plot.definition();
        var series = new ArrayList<>(d.series());
        var old = series.get(index);
        if (old.kind() == kind) {
            return new PlotEditResult(plot, target, false);
        }
        series.set(index, new PlotSeries(old.name(), kind, old.points()));
        var updated = copy(d, d.title(), d.xAxis(), d.yAxis(), series, d.legendVisible(), d.gridVisible());
        return new PlotEditResult(new PlotBlock(updated), target, true);
    }

    public boolean canAddPoint(PlotBlock plot, PlotEditTarget target) {
        validateSelection(plot, target);
        return seriesIndex(target) >= 0;
    }

    public PlotEditResult addPoint(PlotBlock plot, PlotEditTarget target) {
        validateSelection(plot, target);
        var index = seriesIndex(target);
        if (index < 0) {
            return new PlotEditResult(plot, target, false);
        }
        var d = plot.definition();
        var series = new ArrayList<>(d.series());
        var old = series.get(index);
        var points = new ArrayList<>(old.points());
        points.add(new DataPoint(0.0, 0.0));
        series.set(index, new PlotSeries(old.name(), old.kind(), points));
        var pointIndex = points.size() - 1;
        var updated = copy(d, d.title(), d.xAxis(), d.yAxis(), series, d.legendVisible(), d.gridVisible());
        return new PlotEditResult(new PlotBlock(updated), new PlotPointTarget(index, pointIndex), true);
    }

    public boolean canDeletePoint(PlotBlock plot, PlotEditTarget target) {
        validateSelection(plot, target);
        return target instanceof PlotPointTarget;
    }

    public PlotEditResult deletePoint(PlotBlock plot, PlotEditTarget target) {
        validateSelection(plot, target);
        if (!(target instanceof PlotPointTarget pointTarget)) {
            return new PlotEditResult(plot, target, false);
        }
        var d = plot.definition();
        var series = new ArrayList<>(d.series());
        var old = series.get(pointTarget.seriesIndex());
        var points = new ArrayList<>(old.points());
        points.remove(pointTarget.pointIndex());
        series.set(pointTarget.seriesIndex(), new PlotSeries(old.name(), old.kind(), points));
        PlotEditTarget next = points.isEmpty()
                ? new PlotSeriesTarget(pointTarget.seriesIndex())
                : new PlotPointTarget(pointTarget.seriesIndex(), Math.min(pointTarget.pointIndex(), points.size() - 1));
        var updated = copy(d, d.title(), d.xAxis(), d.yAxis(), series, d.legendVisible(), d.gridVisible());
        return new PlotEditResult(new PlotBlock(updated), next, true);
    }

    public int selectedSeriesIndex(PlotBlock plot, PlotEditTarget target) {
        validateSelection(plot, target);
        return seriesIndex(target);
    }

    private PlotEditTarget adjacentTarget(PlotBlock plot, PlotEditTarget current, int direction) {
        validateSelection(plot, current);
        var targets = targets(plot);
        var index = targets.indexOf(current);
        if (index < 0) {
            return firstTarget(plot);
        }
        var next = Math.max(0, Math.min(targets.size() - 1, index + direction));
        return targets.get(next);
    }

    private List<PlotEditTarget> targets(PlotBlock plot) {
        var result = new ArrayList<PlotEditTarget>();
        result.add(new PlotPropertyTarget(PlotProperty.TITLE));
        result.add(new PlotPropertyTarget(PlotProperty.X_AXIS_LABEL));
        result.add(new PlotPropertyTarget(PlotProperty.Y_AXIS_LABEL));
        for (var seriesIndex = 0; seriesIndex < plot.definition().series().size(); seriesIndex++) {
            result.add(new PlotSeriesTarget(seriesIndex));
            var points = plot.definition().series().get(seriesIndex).points();
            for (var pointIndex = 0; pointIndex < points.size(); pointIndex++) {
                result.add(new PlotPointTarget(seriesIndex, pointIndex));
            }
        }
        return List.copyOf(result);
    }

    private static int seriesIndex(PlotEditTarget target) {
        if (target instanceof PlotSeriesTarget seriesTarget) {
            return seriesTarget.seriesIndex();
        }
        if (target instanceof PlotPointTarget pointTarget) {
            return pointTarget.seriesIndex();
        }
        return -1;
    }

    private static PlotSeries series(PlotBlock plot, int seriesIndex) {
        if (seriesIndex < 0 || seriesIndex >= plot.definition().series().size()) {
            throw new IllegalArgumentException("Plot series selection is outside the plot.");
        }
        return plot.definition().series().get(seriesIndex);
    }

    private static AxisDefinition withLabel(AxisDefinition axis, String label) {
        return new AxisDefinition(label, axis.explicitRange(), axis.scale());
    }

    private static PlotDefinition copy(
            PlotDefinition original,
            String title,
            AxisDefinition xAxis,
            AxisDefinition yAxis,
            List<PlotSeries> series,
            boolean legendVisible,
            boolean gridVisible
    ) {
        return new PlotDefinition(title, xAxis, yAxis, series, legendVisible, gridVisible, original.height());
    }
}
