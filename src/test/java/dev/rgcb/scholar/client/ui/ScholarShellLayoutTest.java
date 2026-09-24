package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void productionChromeHasNoNegativeOrOverlappingGeometryAcrossWindowSizes() {
        for (var size : new int[][] {{80, 10}, {320, 240}, {640, 360}, {1280, 720}, {2560, 1440}}) {
            var layout = ScholarShellLayout.compute(size[0], size[1], true);
            assertEquals(layout.toolbarBounds().bottom(), layout.documentWorkspaceBounds().y());
            if (size[1] >= layout.chromeHeight()) {
                assertEquals(layout.documentWorkspaceBounds().bottom(), layout.statusBarBounds().y());
            } else {
                assertEquals(0, layout.documentWorkspaceBounds().height());
            }
            assertEquals(size[1], layout.statusBarBounds().bottom());
            assertTrue(layout.documentWorkspaceBounds().height() >= 0);
            assertTrue(layout.statusBarBounds().height() >= 0);
        }
    }
}
