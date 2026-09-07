package dev.rgcb.scholar.math.layout;

public record MathLineSegment(int x1, int y1, int x2, int y2, int thickness) implements MathPrimitive {
    public MathLineSegment {
        if (thickness <= 0) {
            throw new IllegalArgumentException("thickness must be positive.");
        }
    }
}
