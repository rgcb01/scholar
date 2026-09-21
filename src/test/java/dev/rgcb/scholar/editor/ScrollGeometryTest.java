package dev.rgcb.scholar.editor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScrollGeometryTest {
    @Test void clampsAfterDeletionAndResize() {
        assertEquals(0, ScrollGeometry.clamp(-1, 1000, 200));
        assertEquals(800, ScrollGeometry.clamp(9999, 1000, 200));
        assertEquals(100, ScrollGeometry.clamp(800, 300, 200));
        assertEquals(0, ScrollGeometry.clamp(800, 300, 400));
    }
    @Test void revealsCaretMinimallyWithoutRecentering() {
        assertEquals(100, ScrollGeometry.reveal(100, 1000, 200, 150, 10));
        assertEquals(90, ScrollGeometry.reveal(100, 1000, 200, 90, 10));
        assertEquals(110, ScrollGeometry.reveal(100, 1000, 200, 300, 10));
        assertEquals(800, ScrollGeometry.reveal(100, 1000, 200, 990, 10));
    }
    @Test void tallAtomicObjectDoesNotOscillateOnRepeatedVisibilityRequests() {
        assertEquals(100, ScrollGeometry.reveal(100, 1000, 200, 50, 500));
        assertEquals(50, ScrollGeometry.reveal(700, 1000, 200, 50, 500));
        var offset = ScrollGeometry.reveal(0, 2000, 200, 400, 500);
        assertEquals(offset, ScrollGeometry.reveal(offset, 2000, 200, 400, 500));
    }
}
