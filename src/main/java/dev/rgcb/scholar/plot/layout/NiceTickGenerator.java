package dev.rgcb.scholar.plot.layout;

import dev.rgcb.scholar.plot.AxisRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Generates deterministic human-readable linear ticks using 1/2/5 decades. */
public final class NiceTickGenerator {
    public static final int DEFAULT_TARGET_TICKS = 6;
    private static final int MAX_TICKS = 128;

    public List<AxisTick> generate(AxisRange range) {
        return generate(range, DEFAULT_TARGET_TICKS);
    }

    public List<AxisTick> generate(AxisRange range, int targetTickCount) {
        Objects.requireNonNull(range, "range");
        if (targetTickCount < 2) {
            throw new IllegalArgumentException("targetTickCount must be at least 2.");
        }

        var span = range.max() - range.min();
        if (!Double.isFinite(span) || span <= 0.0) {
            return endpointFallback(range);
        }
        var rawStep = span / (targetTickCount - 1.0);
        if (!Double.isFinite(rawStep) || rawStep <= 0.0) {
            return endpointFallback(range);
        }
        var step = niceStep(rawStep);
        if (!Double.isFinite(step) || step <= 0.0) {
            return endpointFallback(range);
        }

        var startIndex = (long) Math.ceil(range.min() / step - 1.0e-12);
        var endIndex = (long) Math.floor(range.max() / step + 1.0e-12);
        if (endIndex < startIndex || endIndex - startIndex + 1 > MAX_TICKS) {
            return endpointFallback(range);
        }

        var ticks = new ArrayList<AxisTick>();
        for (long index = startIndex; index <= endIndex; index++) {
            var value = index * step;
            if (!Double.isFinite(value)) {
                return endpointFallback(range);
            }
            if (Math.abs(value) < step * 1.0e-12) {
                value = 0.0;
            }
            ticks.add(new AxisTick(value, PlotNumberFormatter.formatGeneratedTick(index, step)));
        }
        if (ticks.size() < 2) {
            return endpointFallback(range);
        }
        return List.copyOf(ticks);
    }

    static double niceStep(double rawStep) {
        if (!Double.isFinite(rawStep) || rawStep <= 0.0) {
            throw new IllegalArgumentException("rawStep must be finite and positive.");
        }
        var exponent = Math.floor(Math.log10(rawStep));
        var power = Math.pow(10.0, exponent);
        var fraction = rawStep / power;
        var niceFraction = fraction <= 1.0 ? 1.0
                : fraction <= 2.0 ? 2.0
                : fraction <= 5.0 ? 5.0
                : 10.0;
        return niceFraction * power;
    }

    private static List<AxisTick> endpointFallback(AxisRange range) {
        var step = Math.max(Math.abs(range.max() - range.min()), Math.ulp(Math.max(Math.abs(range.min()), Math.abs(range.max()))));
        if (!Double.isFinite(step) || step <= 0.0) {
            step = 1.0;
        }
        return List.of(
                new AxisTick(range.min(), PlotNumberFormatter.format(range.min(), step)),
                new AxisTick(range.max(), PlotNumberFormatter.format(range.max(), step)));
    }
}
