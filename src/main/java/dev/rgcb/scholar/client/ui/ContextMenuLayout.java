package dev.rgcb.scholar.client.ui;

public final class ContextMenuLayout {
    public static final int ROW_HEIGHT = 18;
    public static final int DEFAULT_WIDTH = 190;
    public static final int MAX_VISIBLE_ROWS = 12;

    private ContextMenuLayout() {
    }

    public static ShellRect compute(int pointerX, int pointerY, int viewportWidth, int viewportHeight, int rowCount) {
        var visibleRows = visibleRowCount(viewportHeight, rowCount);
        var width = Math.min(DEFAULT_WIDTH, Math.max(1, viewportWidth));
        var height = Math.min(Math.max(ROW_HEIGHT, viewportHeight), visibleRows * ROW_HEIGHT);
        var x = pointerX;
        var y = pointerY;
        if (x + width > viewportWidth) {
            x = viewportWidth - width;
        }
        if (y + height > viewportHeight) {
            y = viewportHeight - height;
        }
        return new ShellRect(Math.max(0, x), Math.max(0, y), width, height);
    }

    public static int visibleRowCount(int viewportHeight, int rowCount) {
        if (rowCount <= 0) {
            return 0;
        }
        var rowsByHeight = Math.max(1, viewportHeight / ROW_HEIGHT);
        return Math.max(1, Math.min(rowCount, Math.min(MAX_VISIBLE_ROWS, rowsByHeight)));
    }
}
