package dev.rgcb.scholar.plot.layout;

import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.AxisRange;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

/** Resolves explicit or deterministic automatic linear ranges for an XY plot. */
public final class PlotRangeResolver {
    public static final AxisRange EMPTY_RANGE = new AxisRange(0.0, 1.0);
    public static final double AUTO_PADDING_FRACTION = 0.05;

    public ResolvedPlotRanges resolve(PlotDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return new ResolvedPlotRanges(
                resolveAxis(definition, definition.xAxis(), DataPoint::x),
                resolveAxis(definition, definition.yAxis(), DataPoint::y));
    }

    private AxisRange resolveAxis(
            PlotDefinition definition,
            AxisDefinition axis,
            ToDoubleFunction<DataPoint> coordinate
    ) {
        if (axis.explicitRange().isPresent()) {
            return axis.explicitRange().orElseThrow();
        }

        var found = false;
        var min = Double.POSITIVE_INFINITY;
        var max = Double.NEGATIVE_INFINITY;
        for (var series : definition.series()) {
            for (var point : series.points()) {
                var value = coordinate.applyAsDouble(point);
                if (!found) {
                    min = value;
                    max = value;
                    found = true;
                } else {
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }
        }

        if (!found) {
            return EMPTY_RANGE;
        }
        if (Double.compare(min, max) == 0) {
            return expandConstant(min);
        }
        return addPadding(min, max);
    }

    private static AxisRange expandConstant(double value) {
        if (value == 0.0) {
            return new AxisRange(-1.0, 1.0);
        }
        var halfSpan = Math.abs(value) * AUTO_PADDING_FRACTION;
        if (!Double.isFinite(halfSpan) || halfSpan == 0.0) {
            halfSpan = Math.ulp(value) * 4.0;
        }
        var min = value - halfSpan;
        var max = value + halfSpan;
        if (!Double.isFinite(min) || !Double.isFinite(max) || !(min < max)) {
            min = Math.nextDown(value);
            max = Math.nextUp(value);
        }
        if (!Double.isFinite(min) || !Double.isFinite(max) || !(min < max)) {
            // Finite extreme values can have only one finite adjacent value. Use a
            // conservative finite range on the representable side.
            if (value > 0.0) {
                min = value * 0.9;
                max = value;
            } else {
                min = value;
                max = value * 0.9;
            }
        }
        return new AxisRange(min, max);
    }

    private static AxisRange addPadding(double min, double max) {
        var span = max - min;
        if (!Double.isFinite(span) || span <= 0.0) {
            return new AxisRange(min, max);
        }
        var padding = span * AUTO_PADDING_FRACTION;
        var paddedMin = min - padding;
        var paddedMax = max + padding;
        if (!Double.isFinite(padding)
                || !Double.isFinite(paddedMin)
                || !Double.isFinite(paddedMax)
                || !(paddedMin < paddedMax)) {
            return new AxisRange(min, max);
        }
        return new AxisRange(paddedMin, paddedMax);
    }
}
