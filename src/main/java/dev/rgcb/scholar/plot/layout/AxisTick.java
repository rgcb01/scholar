package dev.rgcb.scholar.plot.layout;

/** Semantic resolved tick before document-space label placement. */
public record AxisTick(double value, String label) {
    public AxisTick {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Tick value must be finite.");
        }
        if (label == null) {
            throw new NullPointerException("label");
        }
    }
}
