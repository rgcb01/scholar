package dev.rgcb.scholar.editor;

public record SelectionRect(int x, int y, int width, int height) {
    public SelectionRect {
        if (width < 0) {
            throw new IllegalArgumentException("width must not be negative.");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative.");
        }
    }
}
