package dev.rgcb.scholar.plot;

import java.util.List;
import java.util.Objects;

public record PlotDefinition(
        String title,
        AxisDefinition xAxis,
        AxisDefinition yAxis,
        List<PlotSeries> series,
        boolean legendVisible,
        boolean gridVisible,
        int height
) {
    public static final int DEFAULT_HEIGHT = 180;
    public static final int MIN_HEIGHT = 96;

    public PlotDefinition {
        title = Objects.requireNonNull(title, "title");
        xAxis = Objects.requireNonNull(xAxis, "xAxis");
        yAxis = Objects.requireNonNull(yAxis, "yAxis");
        series = List.copyOf(Objects.requireNonNull(series, "series"));
        if (height < MIN_HEIGHT) {
            throw new IllegalArgumentException("Plot height must be at least " + MIN_HEIGHT + ".");
        }
    }

    public static PlotDefinition of(String title, AxisDefinition xAxis, AxisDefinition yAxis, List<PlotSeries> series) {
        return new PlotDefinition(title, xAxis, yAxis, series, true, true, DEFAULT_HEIGHT);
    }
}
