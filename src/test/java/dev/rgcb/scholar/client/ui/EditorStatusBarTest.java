package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.editor.EditorActionId;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class EditorStatusBarTest {
    @Test
    void iconClicksDispatchExistingViewportActions() {
        var actions = new ArrayList<EditorActionId>();
        var status = new EditorStatusBar(actions::add, zoom -> {}, () -> 1.0f);
        var bar = new ShellRect(0, 0, 640, 24);
        var fitPage = ScholarStatusBarLayout.compute(bar).fitPage();

        assertTrue(status.mouseClicked(bar, fitPage.x() + 1, fitPage.y() + 1, 0));
        assertEquals(java.util.List.of(EditorActionId.VIEW_FIT_PAGE), actions);
        assertTrue(status.mouseClicked(bar, fitPage.x() + 1, fitPage.y() + 1, 1));
        assertFalse(status.mouseClicked(bar, fitPage.x() + 1, bar.bottom() + 1, 0));
    }

    @Test
    void sliderOwnsDragUntilLeftButtonRelease() {
        var zooms = new ArrayList<Float>();
        var status = new EditorStatusBar(action -> {}, zooms::add, () -> 1.0f);
        var bar = new ShellRect(0, 0, 640, 24);
        var slider = ScholarStatusBarLayout.compute(bar).slider();

        assertTrue(status.mouseClicked(bar, slider.x(), slider.y() + 1, 0));
        assertTrue(status.mouseDragged(bar, slider.right(), 0));
        assertTrue(status.mouseReleased(0));
        assertFalse(status.mouseDragged(bar, slider.x(), 0));
        assertEquals(0.4f, zooms.get(0));
        assertEquals(2.0f, zooms.get(1));
    }
}
