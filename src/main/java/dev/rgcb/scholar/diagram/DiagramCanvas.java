package dev.rgcb.scholar.diagram;

public record DiagramCanvas(double width, double height) {
    public DiagramCanvas {
        requireFinitePositive(width, "width");
        requireFinitePositive(height, "height");
    }

    private static void requireFinitePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException("Diagram canvas " + name + " must be finite and positive.");
        }
    }
}
