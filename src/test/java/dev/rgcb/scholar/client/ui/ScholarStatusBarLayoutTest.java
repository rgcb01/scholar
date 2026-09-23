package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScholarStatusBarLayoutTest {
    @Test void wideBarShowsAllControlsWithoutOverlaps() {
        var bar = new ShellRect(0, 336, 640, 24);
        var layout = ScholarStatusBarLayout.compute(bar);
        assertTrue(layout.page().width() > 0);
        assertTrue(layout.words().width() > 0);
        assertTrue(layout.fitPage().width() > 0);
        assertTrue(layout.fitWidth().width() > 0);
        assertTrue(layout.words().right() < layout.fitPage().x());
        assertTrue(layout.fitPage().right() < layout.fitWidth().x());
        assertTrue(layout.fitWidth().right() < layout.zoomOut().x());
        assertEquals(bar.bottom(), ScholarShellLayout.compute(640, 360, true).statusBarBounds().bottom());
    }

    @Test void narrowBarDropsLowerPriorityItemsBeforeZoom() {
        var layout = ScholarStatusBarLayout.compute(new ShellRect(0, 216, 250, 24));
        assertTrue(layout.page().width() > 0);
        assertEquals(0, layout.words().width());
        assertEquals(0, layout.fitPage().width());
        assertEquals(0, layout.fitWidth().width());
        assertTrue(layout.slider().width() > 0);
        assertTrue(layout.percentage().width() > 0);
        assertFalse(layout.slider().contains(layout.zoomOut().x(), layout.zoomOut().y()));
    }

    @Test void sliderCoversSupportedRangeAndTracksFitZoom() {
        var slider = ScholarStatusBarLayout.compute(new ShellRect(0, 0, 640, 24)).slider();
        assertEquals(0.4f, ScholarStatusBarLayout.zoomAt(slider, slider.x()));
        assertEquals(2.0f, ScholarStatusBarLayout.zoomAt(slider, slider.right()));
        for (var zoom : new float[]{0.75f, 1.0f, 1.25f, 1.5f, 2.0f}) {
            var thumb = ScholarStatusBarLayout.thumbX(slider, zoom);
            assertEquals(zoom, ScholarStatusBarLayout.zoomAt(slider, thumb), 0.02f);
        }
    }
}
