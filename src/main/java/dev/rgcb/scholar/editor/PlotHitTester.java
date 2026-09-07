package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.plot.layout.LaidOutPlot;
import dev.rgcb.scholar.plot.layout.LaidOutPlotLabel;
import java.util.Objects;

/** Pure-Java mapping from laid-out plot geometry to semantic plot edit targets. */
public final class PlotHitTester {
    private static final int POINT_RADIUS = 6;
    private static final double SERIES_DISTANCE = 4.0;

    public PlotEditTarget hit(LaidOutPlot plot, int x, int y) {
        Objects.requireNonNull(plot, "plot");

        for (var series : plot.series()) {
            for (var point : series.points()) {
                if (Math.abs(x - point.x()) <= POINT_RADIUS && Math.abs(y - point.y()) <= POINT_RADIUS) {
                    return new PlotPointTarget(series.seriesIndex(), point.sourcePointIndex());
                }
            }
        }

        if (plot.legend().isPresent()) {
            var legend = plot.legend().orElseThrow();
            for (var item : legend.items()) {
                var label = item.label();
                var left = Math.min(item.sampleX1(), label.x());
                var right = Math.max(item.sampleX2(), label.x() + label.width());
                var top = Math.min(item.sampleY() - 5, label.y());
                var bottom = Math.max(item.sampleY() + 6, label.y() + label.height());
                if (contains(left, top, right - left, bottom - top, x, y)) {
                    return new PlotSeriesTarget(item.seriesIndex());
                }
            }
        }

        if (plot.title().filter(label -> contains(label, x, y)).isPresent()) {
            return new PlotPropertyTarget(PlotProperty.TITLE);
        }
        if (plot.xAxisLabel().filter(label -> contains(label, x, y)).isPresent()) {
            return new PlotPropertyTarget(PlotProperty.X_AXIS_LABEL);
        }
        if (plot.yAxisLabel().filter(label -> contains(label, x, y)).isPresent()) {
            return new PlotPropertyTarget(PlotProperty.Y_AXIS_LABEL);
        }

        for (var series : plot.series()) {
            for (var segment : series.lineSegments()) {
                if (distanceToSegment(x, y, segment.x1(), segment.y1(), segment.x2(), segment.y2()) <= SERIES_DISTANCE) {
                    return new PlotSeriesTarget(series.seriesIndex());
                }
            }
        }

        return new PlotPropertyTarget(PlotProperty.TITLE);
    }

    private static boolean contains(LaidOutPlotLabel label, int x, int y) {
        return contains(label.x(), label.y(), Math.max(1, label.width()), Math.max(1, label.height()), x, y);
    }

    private static boolean contains(int left, int top, int width, int height, int x, int y) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }

    private static double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        var dx = x2 - x1;
        var dy = y2 - y1;
        if (dx == 0.0 && dy == 0.0) {
            return Math.hypot(px - x1, py - y1);
        }
        var t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0.0, Math.min(1.0, t));
        var cx = x1 + t * dx;
        var cy = y1 + t * dy;
        return Math.hypot(px - cx, py - cy);
    }
}
