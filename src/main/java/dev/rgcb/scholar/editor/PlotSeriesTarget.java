package dev.rgcb.scholar.editor;

public record PlotSeriesTarget(int seriesIndex) implements PlotEditTarget {
    public PlotSeriesTarget {
        if (seriesIndex < 0) {
            throw new IllegalArgumentException("seriesIndex must not be negative.");
        }
    }
}
