package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScholarShellLayoutTest {
    @Test
    void reservesMenuAndToolbarBeforeDocumentWorkspace() {
        var layout = ScholarShellLayout.compute(320, 240);

        assertEquals(new ShellRect(0, 0, 320, MenuBarWidget.HEIGHT), layout.menuBarBounds());
        assertEquals(new ShellRect(0, MenuBarWidget.HEIGHT, 320, ToolbarWidget.HEIGHT), layout.toolbarBounds());
        assertEquals(MenuBarWidget.HEIGHT + ToolbarWidget.HEIGHT, layout.documentWorkspaceBounds().y());
        assertEquals(240 - MenuBarWidget.HEIGHT - ToolbarWidget.HEIGHT, layout.documentWorkspaceBounds().height());
    }

    @Test
    void clampsWorkspaceHeightForTinyScreens() {
        var layout = ScholarShellLayout.compute(80, 10);

        assertEquals(0, layout.documentWorkspaceBounds().height());
    }
}
