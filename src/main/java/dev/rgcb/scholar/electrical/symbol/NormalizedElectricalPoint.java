package dev.rgcb.scholar.electrical.symbol;

/** Unit-space point used only by derived schematic symbol geometry. */
public record NormalizedElectricalPoint(double x, double y) {
    public NormalizedElectricalPoint {
        if (!Double.isFinite(x) || !Double.isFinite(y)
                || x < 0.0 || x > 1.0 || y < 0.0 || y > 1.0) {
            throw new IllegalArgumentException("Normalized electrical coordinates must be finite and in [0, 1].");
        }
    }
}
