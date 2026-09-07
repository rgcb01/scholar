package dev.rgcb.scholar.math.layout;

public record MathHorizontalRule(int x, int y, int width, int thickness) implements MathPrimitive {
    public MathHorizontalRule {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive.");
        }
        if (thickness <= 0) {
            throw new IllegalArgumentException("thickness must be positive.");
        }
    }
}
