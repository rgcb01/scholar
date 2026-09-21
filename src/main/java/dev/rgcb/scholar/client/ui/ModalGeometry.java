package dev.rgcb.scholar.client.ui;

/** Shared viewport-safe geometry for Scholar's small in-editor modal panels. */
public final class ModalGeometry {
    private static final int EDGE_MARGIN = 8;

    private ModalGeometry() {
    }

    public static ShellRect centered(
            int viewportWidth,
            int viewportHeight,
            int preferredWidth,
            int preferredHeight,
            int topInset
    ) {
        var availableWidth = Math.max(1, viewportWidth - EDGE_MARGIN * 2);
        var availableHeight = Math.max(1, viewportHeight - topInset - EDGE_MARGIN);
        var width = Math.min(preferredWidth, availableWidth);
        var height = Math.min(preferredHeight, availableHeight);
        var x = Math.max(0, (viewportWidth - width) / 2);
        var centeredY = (viewportHeight - height) / 2;
        var y = Math.max(topInset, Math.min(centeredY, viewportHeight - EDGE_MARGIN - height));
        return new ShellRect(x, Math.max(0, y), width, height);
    }
}
