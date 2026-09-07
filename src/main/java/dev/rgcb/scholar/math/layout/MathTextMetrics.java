package dev.rgcb.scholar.math.layout;

public record MathTextMetrics(int width, int ascent, int descent) {
    public MathTextMetrics {
        if (width < 0) {
            throw new IllegalArgumentException("width must not be negative.");
        }
        if (ascent < 0) {
            throw new IllegalArgumentException("ascent must not be negative.");
        }
        if (descent < 0) {
            throw new IllegalArgumentException("descent must not be negative.");
        }
    }
}
