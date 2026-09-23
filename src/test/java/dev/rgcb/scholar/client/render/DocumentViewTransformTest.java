package dev.rgcb.scholar.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.rgcb.scholar.editor.CaretGeometry;

import org.junit.jupiter.api.Test;

class DocumentViewTransformTest {
    @Test
    void finalCaretRectangleIsScreenSpaceAndScaledOnceAfterScroll() {
        for (var zoom : new double[]{0.75, 1.0, 1.25, 1.5, 2.0}) {
            var view = new DocumentViewTransform(40, 60, zoom);
            for (var logicalHeight : new int[]{11, 12}) {
                var logical = new CaretGeometry(27, 100, logicalHeight);
                var rect = view.caretRect(logical, 40, 60, 37);
                assertEquals(1, rect.right() - rect.left());
                assertEquals(Math.round(view.screenX(67)), rect.left());
                assertEquals(logicalHeight * zoom, rect.bottom() - rect.top(), 1.0);
                assertTrue(rect.bottom() - rect.top() < logicalHeight * zoom * zoom + 2 || zoom < 1);
            }
        }
    }
    @Test
    void screenAndLayoutRoundTripAcrossZoomAndGuiScales() {
        for (var guiScale : new int[]{1, 2, 3, 4}) {
            for (var zoom : new double[]{0.75, 1.0, 1.25, 1.5, 2.0}) {
                var transform = new DocumentViewTransform(63, 91, zoom);
                for (var x : new double[]{63, 64.25, 232.5, 1001}) {
                    var physicalX = transform.screenX(x) * guiScale;
                    assertEquals(x, transform.logicalX(physicalX / guiScale), 1.0e-9);
                }
                for (var y : new double[]{91, 92.5, 353.25, 800}) {
                    var physicalY = transform.screenY(y) * guiScale;
                    assertEquals(y, transform.logicalY(physicalY / guiScale), 1.0e-9);
                }
            }
        }
    }

    @Test
    void scissorExpandsToContainFractionalScreenBounds() {
        var transform = new DocumentViewTransform(31, 57, 1.25);
        var clip = transform.clip(40, 62, 160, 270);
        assertTrue(clip.left() <= transform.screenX(40));
        assertTrue(clip.top() <= transform.screenY(62));
        assertTrue(clip.right() >= transform.screenX(160));
        assertTrue(clip.bottom() >= transform.screenY(270));
        assertEquals(31, transform.clip(31, 57, 100, 100).left());
    }

    @Test
    void logicalViewportExtentsCoverPhysicalViewportWhenZoomedOut() {
        for (var zoom : new double[]{0.75, 0.8, 0.9, 1.0, 1.25, 2.0}) {
            var transform = new DocumentViewTransform(31, 57, zoom);
            var logicalWidth = (int) Math.ceil(481 / zoom);
            var logicalHeight = (int) Math.ceil(307 / zoom);
            var clip = transform.clip(31, 57, 31 + logicalWidth, 57 + logicalHeight);
            assertTrue(clip.right() >= 31 + 481);
            assertTrue(clip.bottom() >= 57 + 307);
        }
    }
}
