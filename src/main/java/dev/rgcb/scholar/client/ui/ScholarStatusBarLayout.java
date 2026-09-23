package dev.rgcb.scholar.client.ui;

/** Screen-space hit regions for the fixed application status bar. */
public record ScholarStatusBarLayout(ShellRect page, ShellRect words, ShellRect fitPage, ShellRect fitWidth,
                                     ShellRect zoomOut, ShellRect slider, ShellRect zoomIn, ShellRect percentage) {
    private static final ShellRect HIDDEN = new ShellRect(0, 0, 0, 0);

    public static ScholarStatusBarLayout compute(ShellRect bar) {
        int y = bar.y() + Math.max(0, (bar.height() - 18) / 2);
        int right = bar.right() - 6;
        ShellRect percentage = rect(right - 40, y, 40, bar);
        ShellRect zoomIn = rect(percentage.x() - 20, y, 18, bar);
        ShellRect slider = rect(zoomIn.x() - 82, y, 78, bar);
        ShellRect zoomOut = rect(slider.x() - 22, y, 18, bar);
        int available = zoomOut.x() - bar.x();
        ShellRect page = available >= 66 ? rect(bar.x() + 6, y, 66, bar) : HIDDEN;
        ShellRect words = available >= 148 ? rect(page.right() + 8, y, 74, bar) : HIDDEN;
        ShellRect fitWidth = available >= 202 ? rect(zoomOut.x() - 24, y, 18, bar) : HIDDEN;
        ShellRect fitPage = available >= 226 ? rect(fitWidth.x() - 22, y, 18, bar) : HIDDEN;
        return new ScholarStatusBarLayout(page, words, fitPage, fitWidth,
                zoomOut, slider, zoomIn, percentage);
    }

    private static ShellRect rect(int x, int y, int width, ShellRect bar) {
        return x >= bar.x() && x + width <= bar.right() && bar.height() >= 18
                ? new ShellRect(x, y, width, 18) : HIDDEN;
    }

    public static float zoomAt(ShellRect slider, double mouseX) {
        if (slider.width() <= 0) return 1.0f;
        var fraction = Math.max(0, Math.min(1, (mouseX - slider.x()) / slider.width()));
        return (float) (0.4 + fraction * 1.6);
    }

    public static int thumbX(ShellRect slider, float zoom) {
        return slider.x() + (int) Math.round(slider.width() * Math.max(0, Math.min(1, (zoom - 0.4) / 1.6)));
    }
}
