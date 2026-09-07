package dev.rgcb.scholar.diagram;

public record DiagramBounds(double x, double y, double width, double height) {
    public DiagramBounds {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Diagram bounds position must be finite.");
        }
        if (!Double.isFinite(width) || width <= 0.0 || !Double.isFinite(height) || height <= 0.0) {
            throw new IllegalArgumentException("Diagram bounds dimensions must be finite and positive.");
        }
    }

    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }
}
