package dev.rgcb.scholar.math.editor;

public record MathSelectionRect(int x, int y, int width, int height) {
    public MathSelectionRect {
        if (width < 0) {
            throw new IllegalArgumentException("width must not be negative.");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative.");
        }
    }
}
