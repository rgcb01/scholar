package dev.rgcb.scholar.client.ui;

public record ScholarShellLayout(ShellRect menuBarBounds, ShellRect toolbarBounds, ShellRect documentWorkspaceBounds) {
    public static ScholarShellLayout compute(int screenWidth, int screenHeight) {
        if (screenWidth < 0 || screenHeight < 0) {
            throw new IllegalArgumentException("screen dimensions must not be negative.");
        }
        var menu = new ShellRect(0, 0, screenWidth, MenuBarWidget.HEIGHT);
        var toolbar = new ShellRect(0, menu.bottom(), screenWidth, ToolbarWidget.HEIGHT);
        var workspaceY = toolbar.bottom();
        var workspaceHeight = Math.max(0, screenHeight - workspaceY);
        return new ScholarShellLayout(menu, toolbar, new ShellRect(0, workspaceY, screenWidth, workspaceHeight));
    }

    public int chromeHeight() {
        return documentWorkspaceBounds.y();
    }
}
