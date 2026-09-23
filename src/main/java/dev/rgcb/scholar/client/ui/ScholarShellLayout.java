package dev.rgcb.scholar.client.ui;

public record ScholarShellLayout(ShellRect menuBarBounds, ShellRect toolbarBounds, ShellRect applicationHeaderBounds,
                                 ShellRect documentWorkspaceBounds, ShellRect statusBarBounds) {
    public static final int DOCUMENT_SCROLL_STEP = 24;
    public static final int STATUS_BAR_HEIGHT = 24;
    public static ScholarShellLayout compute(int screenWidth, int screenHeight) {
        return compute(screenWidth, screenHeight, false);
    }

    public static ScholarShellLayout compute(int screenWidth, int screenHeight, boolean productionRibbon) {
        if (screenWidth < 0 || screenHeight < 0) {
            throw new IllegalArgumentException("screen dimensions must not be negative.");
        }
        var header = productionRibbon
                ? new ShellRect(0, 0, screenWidth, ApplicationHeaderWidget.HEIGHT)
                : new ShellRect(0, MenuBarWidget.HEIGHT + ToolbarWidget.HEIGHT,
                        screenWidth, ApplicationHeaderWidget.HEIGHT);
        var menu = productionRibbon
                ? new ShellRect(0, header.bottom(), screenWidth, RibbonWidget.TAB_HEIGHT)
                : new ShellRect(0, 0, screenWidth, MenuBarWidget.HEIGHT);
        var toolbar = productionRibbon
                ? new ShellRect(0, menu.bottom(), screenWidth, RibbonWidget.COMMAND_HEIGHT)
                : new ShellRect(0, menu.bottom(), screenWidth, ToolbarWidget.HEIGHT);
        var workspaceY = productionRibbon ? toolbar.bottom() : header.bottom();
        var statusY = Math.min(screenHeight, Math.max(workspaceY, screenHeight - STATUS_BAR_HEIGHT));
        var workspaceHeight = Math.max(0, statusY - workspaceY);
        return new ScholarShellLayout(menu, toolbar, header,
                new ShellRect(0, workspaceY, screenWidth, workspaceHeight),
                new ShellRect(0, statusY, screenWidth, Math.max(0, screenHeight - statusY)));
    }

    public int chromeHeight() {
        return documentWorkspaceBounds.y();
    }
}
