package dev.rgcb.scholar.plot.layout;

import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import java.util.Objects;

/** Positioned geometry for one semantic PlotSeries. */
public record LaidOutPlotSeries(
        int seriesIndex,
        String name,
        PlotSeriesKind kind,
        LaidOutPlotSeriesStyle style,
        List<LaidOutPlotPoint> points,
        List<LaidOutPlotLineSegment> lineSegments
) {
    public LaidOutPlotSeries {
        if (seriesIndex < 0) {
            throw new IllegalArgumentException("seriesIndex must not be negative.");
        }
        name = Objects.requireNonNull(name, "name");
        kind = Objects.requireNonNull(kind, "kind");
        style = Objects.requireNonNull(style, "style");
        points = List.copyOf(Objects.requireNonNull(points, "points"));
        lineSegments = List.copyOf(Objects.requireNonNull(lineSegments, "lineSegments"));
    }
}
