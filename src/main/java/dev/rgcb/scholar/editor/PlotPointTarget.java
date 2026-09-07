package dev.rgcb.scholar.editor;

public record PlotPointTarget(int seriesIndex, int pointIndex) implements PlotEditTarget {
    public PlotPointTarget {
        if (seriesIndex < 0 || pointIndex < 0) {
            throw new IllegalArgumentException("seriesIndex and pointIndex must not be negative.");
        }
    }
}
