package dev.rgcb.scholar.plot;

import java.util.List;
import java.util.Objects;

public record PlotSeries(String name, PlotSeriesKind kind, List<DataPoint> points) {
    public PlotSeries {
        name = Objects.requireNonNull(name, "name");
        kind = Objects.requireNonNull(kind, "kind");
        points = List.copyOf(Objects.requireNonNull(points, "points"));
    }
}
