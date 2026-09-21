package dev.rgcb.scholar.layout;

public record LaidOutColumn(int index, int x, int y, int width, int height) {
    public LaidOutColumn {
        if (index < 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Column geometry must be positive and its index non-negative.");
        }
    }
}
