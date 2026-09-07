package dev.rgcb.scholar.plot.layout;

import dev.rgcb.scholar.plot.AxisRange;
import java.util.Objects;

/** Resolved linear X/Y ranges used by plot layout. */
public record ResolvedPlotRanges(AxisRange xRange, AxisRange yRange) {
    public ResolvedPlotRanges {
        xRange = Objects.requireNonNull(xRange, "xRange");
        yRange = Objects.requireNonNull(yRange, "yRange");
    }
}
