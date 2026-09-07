package dev.rgcb.scholar.client.ui;

public record ToolbarItemBounds(int itemIndex, ToolbarItemKind kind, int x, int y, int width, int height) {
    public ToolbarItemBounds {
        if (itemIndex < 0) {
            throw new IllegalArgumentException("itemIndex must not be negative.");
        }
        if (width < 0) {
            throw new IllegalArgumentException("width must not be negative.");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative.");
        }
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }
}
