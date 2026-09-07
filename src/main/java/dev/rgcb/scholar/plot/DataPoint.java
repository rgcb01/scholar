package dev.rgcb.scholar.plot;

public record DataPoint(double x, double y) {
    public DataPoint {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Plot data points must use finite coordinates.");
        }
    }
}
