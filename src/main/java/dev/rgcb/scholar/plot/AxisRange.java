package dev.rgcb.scholar.plot;

public record AxisRange(double min, double max) {
    public AxisRange {
        if (!Double.isFinite(min) || !Double.isFinite(max)) {
            throw new IllegalArgumentException("Axis range bounds must be finite.");
        }
        if (!(min < max)) {
            throw new IllegalArgumentException("Axis range min must be less than max.");
        }
    }
}
