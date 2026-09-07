package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DocumentRangeTest {
    @Test
    void ordersPositionsByBlockThenCharacterOffset() {
        assertTrue(DocumentPositions.compare(new DocumentPosition(0, 1), new DocumentPosition(0, 2)) < 0);
        assertTrue(DocumentPositions.compare(new DocumentPosition(1, 100), new DocumentPosition(2, 0)) < 0);
        assertEquals(0, DocumentPositions.compare(new DocumentPosition(1, 2), new DocumentPosition(1, 2)));
    }

    @Test
    void normalizesForwardAndBackwardMultiBlockRanges() {
        var start = new DocumentPosition(1, 2);
        var end = new DocumentPosition(3, 5);

        assertEquals(new DocumentRange(start, end), DocumentRange.between(start, end));
        assertEquals(new DocumentRange(start, end), DocumentRange.between(end, start));
    }

    @Test
    void supportsBoundaryOnlyAndSeveralBlockRanges() {
        var boundaryOnly = new DocumentRange(new DocumentPosition(0, 3), new DocumentPosition(1, 0));
        var severalBlocks = new DocumentRange(new DocumentPosition(0, 1), new DocumentPosition(3, 2));

        assertFalse(boundaryOnly.isEmpty());
        assertFalse(boundaryOnly.isSingleBlock());
        assertFalse(severalBlocks.isSingleBlock());
    }

    @Test
    void emptyOnlyMeansEqualPositions() {
        assertTrue(new DocumentRange(new DocumentPosition(2, 0), new DocumentPosition(2, 0)).isEmpty());
        assertFalse(new DocumentRange(new DocumentPosition(2, 0), new DocumentPosition(3, 0)).isEmpty());
    }

    @Test
    void rejectsUnnormalizedDirectConstruction() {
        assertThrows(IllegalArgumentException.class, () ->
                new DocumentRange(new DocumentPosition(3, 0), new DocumentPosition(2, 10)));
        assertThrows(IllegalArgumentException.class, () ->
                new DocumentRange(new DocumentPosition(1, 3), new DocumentPosition(1, 2)));
    }
}
