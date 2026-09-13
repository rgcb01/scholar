package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContextMenuLayoutTest {
    @Test
    void positionsMenuAtPointerWhenItFits() {
        var rect = ContextMenuLayout.compute(20, 30, 400, 300, 3);

        assertEquals(20, rect.x());
        assertEquals(30, rect.y());
        assertEquals(ContextMenuLayout.DEFAULT_WIDTH, rect.width());
        assertEquals(3 * ContextMenuLayout.ROW_HEIGHT, rect.height());
    }

    @Test
    void clampsMenuInsideRightAndBottomViewportEdges() {
        var rect = ContextMenuLayout.compute(390, 290, 400, 300, 4);

        assertEquals(400 - ContextMenuLayout.DEFAULT_WIDTH, rect.x());
        assertEquals(300 - 4 * ContextMenuLayout.ROW_HEIGHT, rect.y());
        assertTrue(rect.right() <= 400);
        assertTrue(rect.bottom() <= 300);
    }

    @Test
    void adaptsWidthAndPositionForTinyViewports() {
        var rect = ContextMenuLayout.compute(50, 50, 80, 40, 10);

        assertEquals(0, rect.x());
        assertEquals(80, rect.width());
        assertTrue(rect.height() <= 40);
        assertTrue(rect.bottom() <= 40);
    }

    @Test
    void capsVisibleRowsForLongMenus() {
        assertEquals(ContextMenuLayout.MAX_VISIBLE_ROWS, ContextMenuLayout.visibleRowCount(800, 50));
    }

    @Test
    void keepsAtLeastOneVisibleRowWhenViewportIsShort() {
        assertEquals(1, ContextMenuLayout.visibleRowCount(8, 5));
    }

    @Test
    void reportsZeroVisibleRowsForEmptyMenus() {
        assertEquals(0, ContextMenuLayout.visibleRowCount(200, 0));
    }
}
