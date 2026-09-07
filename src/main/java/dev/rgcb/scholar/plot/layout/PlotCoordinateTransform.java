package dev.rgcb.scholar.plot.layout;

import dev.rgcb.scholar.plot.AxisRange;
import java.util.Objects;

/** Pure data-space to plot-area coordinate transform for linear XY plots. */
public record PlotCoordinateTransform(
        AxisRange xRange,
        AxisRange yRange,
        int plotAreaX,
        int plotAreaY,
        int plotAreaWidth,
        int plotAreaHeight
) {
    public PlotCoordinateTransform {
        xRange = Objects.requireNonNull(xRange, "xRange");
        yRange = Objects.requireNonNull(yRange, "yRange");
        if (plotAreaWidth <= 0 || plotAreaHeight <= 0) {
            throw new IllegalArgumentException("Plot area dimensions must be positive.");
        }
    }

    public double mapX(double x) {
        requireFinite(x);
        var fraction = fractionWithin(x, xRange);
        return plotAreaX + fraction * Math.max(0, plotAreaWidth - 1);
    }

    public double mapY(double y) {
        requireFinite(y);
        var fraction = fractionWithin(y, yRange);
        return plotAreaY + (1.0 - fraction) * Math.max(0, plotAreaHeight - 1);
    }

    private static double fractionWithin(double value, AxisRange range) {
        if (value == range.min()) {
            return 0.0;
        }
        if (value == range.max()) {
            return 1.0;
        }
        var scale = Math.max(Math.abs(range.min()), Math.abs(range.max()));
        if (scale == 0.0 || !Double.isFinite(scale)) {
            return (value - range.min()) / (range.max() - range.min());
        }
        var normalizedMin = range.min() / scale;
        var normalizedMax = range.max() / scale;
        return (value / scale - normalizedMin) / (normalizedMax - normalizedMin);
    }

    private static void requireFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Plot transform coordinates must be finite.");
        }
    }
}
