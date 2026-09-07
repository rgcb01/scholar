package dev.rgcb.scholar.client.ui;

public record ShellRect(int x, int y, int width, int height) {
    public ShellRect {
        if (width < 0) {
            throw new IllegalArgumentException("width must not be negative.");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative.");
        }
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }
}
