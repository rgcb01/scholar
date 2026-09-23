package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScholarShellLayoutTest {
    @Test
    void preservesDevelopmentMenuAndToolbarGeometry() {
        var layout = ScholarShellLayout.compute(320, 240);

        assertEquals(new ShellRect(0, 0, 320, MenuBarWidget.HEIGHT), layout.menuBarBounds());
        assertEquals(new ShellRect(0, MenuBarWidget.HEIGHT, 320, ToolbarWidget.HEIGHT), layout.toolbarBounds());
        assertEquals(new ShellRect(0, MenuBarWidget.HEIGHT + ToolbarWidget.HEIGHT, 320, ApplicationHeaderWidget.HEIGHT), layout.applicationHeaderBounds());
        assertEquals(MenuBarWidget.HEIGHT + ToolbarWidget.HEIGHT + ApplicationHeaderWidget.HEIGHT, layout.documentWorkspaceBounds().y());
        assertEquals(240 - MenuBarWidget.HEIGHT - ToolbarWidget.HEIGHT - ApplicationHeaderWidget.HEIGHT
                - ScholarShellLayout.STATUS_BAR_HEIGHT, layout.documentWorkspaceBounds().height());
        assertEquals(layout.documentWorkspaceBounds().bottom(), layout.statusBarBounds().y());
    }

    @Test
    void productionReservesARealRibbonWithoutOverlappingDocument() {
        var layout = ScholarShellLayout.compute(640, 360, true);

        assertEquals(0, layout.applicationHeaderBounds().y());
        assertEquals(ApplicationHeaderWidget.HEIGHT, layout.menuBarBounds().y());
        assertEquals(RibbonWidget.TAB_HEIGHT, layout.menuBarBounds().height());
        assertEquals(RibbonWidget.COMMAND_HEIGHT, layout.toolbarBounds().height());
        assertEquals(layout.toolbarBounds().bottom(), layout.documentWorkspaceBounds().y());
        assertEquals(360, layout.statusBarBounds().bottom());
        assertEquals(layout.documentWorkspaceBounds().bottom(), layout.statusBarBounds().y());
    }

    @Test
    void clampsWorkspaceHeightForTinyScreens() {
        var layout = ScholarShellLayout.compute(80, 10);

        assertEquals(0, layout.documentWorkspaceBounds().height());
    }
}
