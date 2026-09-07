package dev.rgcb.scholar.math.editor;

public record MathCaretGeometry(int x, int baselineY, int ascent, int descent) {
    public MathCaretGeometry {
        if (ascent < 0) {
            throw new IllegalArgumentException("ascent must not be negative.");
        }
        if (descent < 0) {
            throw new IllegalArgumentException("descent must not be negative.");
        }
    }

    public int y() {
        return baselineY - ascent;
    }

    public int height() {
        return Math.max(1, ascent + descent);
    }
}
