package dev.rgcb.scholar.editor;

public record CaretGeometry(int x, int y, int height) {
    public CaretGeometry {
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative.");
        }
    }


    public int bottom() { return y + height; }
}
