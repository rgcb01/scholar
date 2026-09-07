package dev.rgcb.scholar.plot.layout;

import dev.rgcb.scholar.plot.AxisRange;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record LaidOutPlot(
        int sourceBlockIndex,
        int x,
        int y,
        int width,
        int height,
        int plotAreaX,
        int plotAreaY,
        int plotAreaWidth,
        int plotAreaHeight,
        Optional<LaidOutPlotLabel> title,
        Optional<LaidOutPlotLabel> xAxisLabel,
        Optional<LaidOutPlotLabel> yAxisLabel,
        AxisRange xRange,
        AxisRange yRange,
        List<LaidOutPlotTick> xTicks,
        List<LaidOutPlotTick> yTicks,
        PlotCoordinateTransform transform,
        boolean gridVisible,
        List<LaidOutPlotSeries> series,
        Optional<LaidOutPlotLegend> legend
) {
    public LaidOutPlot {
        if (sourceBlockIndex < 0) {
            throw new IllegalArgumentException("sourceBlockIndex must not be negative.");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Plot dimensions must be positive.");
        }
        if (plotAreaWidth <= 0 || plotAreaHeight <= 0) {
            throw new IllegalArgumentException("Plot area dimensions must be positive.");
        }
        title = Objects.requireNonNull(title, "title");
        xAxisLabel = Objects.requireNonNull(xAxisLabel, "xAxisLabel");
        yAxisLabel = Objects.requireNonNull(yAxisLabel, "yAxisLabel");
        xRange = Objects.requireNonNull(xRange, "xRange");
        yRange = Objects.requireNonNull(yRange, "yRange");
        xTicks = List.copyOf(Objects.requireNonNull(xTicks, "xTicks"));
        yTicks = List.copyOf(Objects.requireNonNull(yTicks, "yTicks"));
        transform = Objects.requireNonNull(transform, "transform");
        series = List.copyOf(Objects.requireNonNull(series, "series"));
        legend = Objects.requireNonNull(legend, "legend");
    }
}
