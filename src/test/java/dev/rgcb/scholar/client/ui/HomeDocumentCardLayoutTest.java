package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HomeDocumentCardLayoutTest {
    @Test void cardsAreResponsiveBoundedAndReserveAVisualNewDocumentCard() {
        var layout = HomeDocumentCardLayout.compute(640, 360, 7, 0);

        assertTrue(layout.columns() > 1);
        assertFalse(layout.documentCards().isEmpty());
        assertTrue(layout.newDocumentCard().x() >= 0);
        assertTrue(layout.documentCards().stream().allMatch(card -> card.right() <= 640 && card.bottom() <= 360));
    }

    @Test void paginationClampsAndNeverDropsDocumentAccess() {
        var first = HomeDocumentCardLayout.compute(320, 240, 5, 0);
        var last = HomeDocumentCardLayout.compute(320, 240, 5, 99);

        assertTrue(first.documentsPerPage() >= 1);
        assertEquals(last.maxPage(), last.page());
        assertFalse(last.documentCards().isEmpty());
    }
}
