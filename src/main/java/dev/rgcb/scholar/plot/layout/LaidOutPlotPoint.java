package dev.rgcb.scholar.plot.layout;

/** Visible authored data point positioned in document-space plot coordinates. */
public record LaidOutPlotPoint(
        int sourcePointIndex,
        double dataX,
        double dataY,
        int x,
        int y
) {
    public LaidOutPlotPoint {
        if (sourcePointIndex < 0) {
            throw new IllegalArgumentException("sourcePointIndex must not be negative.");
        }
        if (!Double.isFinite(dataX) || !Double.isFinite(dataY)) {
            throw new IllegalArgumentException("Plot point data must remain finite.");
        }
    }
}
