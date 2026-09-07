package dev.rgcb.scholar.plot.layout;

import dev.rgcb.scholar.plot.AxisRange;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure-Java conversion from semantic XY series to visible plot geometry.
 * Scatter points outside explicit ranges are omitted. Line segments are clipped
 * against the resolved XY rectangle while preserving authored point order.
 */
public final class PlotSeriesLayoutEngine {
    public List<LaidOutPlotSeries> layout(List<PlotSeries> series, PlotCoordinateTransform transform) {
        Objects.requireNonNull(series, "series");
        Objects.requireNonNull(transform, "transform");
        var result = new ArrayList<LaidOutPlotSeries>(series.size());
        for (var seriesIndex = 0; seriesIndex < series.size(); seriesIndex++) {
            result.add(layoutSeries(series.get(seriesIndex), seriesIndex, transform));
        }
        return List.copyOf(result);
    }

    private static LaidOutPlotSeries layoutSeries(
            PlotSeries series,
            int seriesIndex,
            PlotCoordinateTransform transform
    ) {
        var style = LaidOutPlotSeriesStyle.forSeriesIndex(seriesIndex);
        var visiblePoints = new ArrayList<LaidOutPlotPoint>();
        for (var pointIndex = 0; pointIndex < series.points().size(); pointIndex++) {
            var point = series.points().get(pointIndex);
            if (contains(transform.xRange(), point.x()) && contains(transform.yRange(), point.y())) {
                visiblePoints.add(new LaidOutPlotPoint(
                        pointIndex,
                        point.x(),
                        point.y(),
                        roundAndClamp(transform.mapX(point.x()), transform.plotAreaX(), right(transform)),
                        roundAndClamp(transform.mapY(point.y()), transform.plotAreaY(), bottom(transform))));
            }
        }

        var segments = new ArrayList<LaidOutPlotLineSegment>();
        if (series.kind() == PlotSeriesKind.LINE) {
            for (var pointIndex = 0; pointIndex + 1 < series.points().size(); pointIndex++) {
                var clipped = clipSegment(series.points().get(pointIndex), series.points().get(pointIndex + 1), transform);
                if (clipped.isPresent()) {
                    var segment = clipped.orElseThrow();
                    segments.add(new LaidOutPlotLineSegment(
                            pointIndex,
                            pointIndex + 1,
                            segment.x1(),
                            segment.y1(),
                            segment.x2(),
                            segment.y2()));
                }
            }
        }

        return new LaidOutPlotSeries(
                seriesIndex,
                series.name(),
                series.kind(),
                style,
                visiblePoints,
                segments);
    }

    private static Optional<ScreenSegment> clipSegment(
            DataPoint from,
            DataPoint to,
            PlotCoordinateTransform transform
    ) {
        // Scale each data axis before Liang-Barsky so subtracting very large
        // finite authored values cannot overflow merely during clipping.
        var sx = scaleFor(from.x(), to.x(), transform.xRange().min(), transform.xRange().max());
        var sy = scaleFor(from.y(), to.y(), transform.yRange().min(), transform.yRange().max());
        var x1 = from.x() / sx;
        var x2 = to.x() / sx;
        var y1 = from.y() / sy;
        var y2 = to.y() / sy;
        var xmin = transform.xRange().min() / sx;
        var xmax = transform.xRange().max() / sx;
        var ymin = transform.yRange().min() / sy;
        var ymax = transform.yRange().max() / sy;

        var dx = x2 - x1;
        var dy = y2 - y1;
        var t = new double[]{0.0, 1.0};
        if (!clip(-dx, x1 - xmin, t)
                || !clip(dx, xmax - x1, t)
                || !clip(-dy, y1 - ymin, t)
                || !clip(dy, ymax - y1, t)) {
            return Optional.empty();
        }

        var cx1 = x1 + t[0] * dx;
        var cy1 = y1 + t[0] * dy;
        var cx2 = x1 + t[1] * dx;
        var cy2 = y1 + t[1] * dy;
        var fx1 = fraction(cx1, xmin, xmax);
        var fy1 = fraction(cy1, ymin, ymax);
        var fx2 = fraction(cx2, xmin, xmax);
        var fy2 = fraction(cy2, ymin, ymax);
        if (!Double.isFinite(fx1) || !Double.isFinite(fy1) || !Double.isFinite(fx2) || !Double.isFinite(fy2)) {
            return Optional.empty();
        }

        var left = transform.plotAreaX();
        var top = transform.plotAreaY();
        var right = right(transform);
        var bottom = bottom(transform);
        return Optional.of(new ScreenSegment(
                roundAndClamp(left + fx1 * Math.max(0, transform.plotAreaWidth() - 1), left, right),
                roundAndClamp(top + (1.0 - fy1) * Math.max(0, transform.plotAreaHeight() - 1), top, bottom),
                roundAndClamp(left + fx2 * Math.max(0, transform.plotAreaWidth() - 1), left, right),
                roundAndClamp(top + (1.0 - fy2) * Math.max(0, transform.plotAreaHeight() - 1), top, bottom)));
    }

    private static boolean clip(double p, double q, double[] t) {
        if (p == 0.0) {
            return q >= 0.0;
        }
        var ratio = q / p;
        if (p < 0.0) {
            if (ratio > t[1]) {
                return false;
            }
            if (ratio > t[0]) {
                t[0] = ratio;
            }
        } else {
            if (ratio < t[0]) {
                return false;
            }
            if (ratio < t[1]) {
                t[1] = ratio;
            }
        }
        return true;
    }

    private static double scaleFor(double a, double b, double min, double max) {
        return Math.max(1.0, Math.max(Math.max(Math.abs(a), Math.abs(b)), Math.max(Math.abs(min), Math.abs(max))));
    }

    private static double fraction(double value, double min, double max) {
        var span = max - min;
        if (!(span > 0.0) || !Double.isFinite(span)) {
            return Double.NaN;
        }
        return (value - min) / span;
    }

    private static boolean contains(AxisRange range, double value) {
        return value >= range.min() && value <= range.max();
    }

    private static int roundAndClamp(double value, int min, int max) {
        if (!Double.isFinite(value)) {
            return value < 0.0 ? min : max;
        }
        var rounded = Math.round(value);
        if (rounded <= min) {
            return min;
        }
        if (rounded >= max) {
            return max;
        }
        return (int) rounded;
    }

    private static int right(PlotCoordinateTransform transform) {
        return transform.plotAreaX() + Math.max(0, transform.plotAreaWidth() - 1);
    }

    private static int bottom(PlotCoordinateTransform transform) {
        return transform.plotAreaY() + Math.max(0, transform.plotAreaHeight() - 1);
    }

    private record ScreenSegment(int x1, int y1, int x2, int y2) {
    }
}
