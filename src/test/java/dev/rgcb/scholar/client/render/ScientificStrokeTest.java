package dev.rgcb.scholar.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScientificStrokeTest {
    @Test
    void coverageIsSymmetricAndFallsOffAtDiagonalEdges() {
        var onLine = ScientificStroke.coverage(5.5, 5.5, 0.5, 0.5, 10.5, 10.5, 1);
        assertEquals(1, onLine);
        assertEquals(onLine, ScientificStroke.coverage(5.5, 5.5, 10.5, 10.5, 0.5, 0.5, 1));
        var edge = ScientificStroke.coverage(5.5, 6.5, 0.5, 0.5, 10.5, 10.5, 1);
        assertTrue(edge > 0 && edge < onLine);
        assertEquals(0, ScientificStroke.coverage(5.5, 9.5, 0.5, 0.5, 10.5, 10.5, 1));
    }

    @Test
    void thickerStrokeCoversMoreAreaWithoutMovingCenter() {
        var thin = ScientificStroke.coverage(5.5, 7.5, 0.5, 5.5, 10.5, 5.5, 1);
        var thick = ScientificStroke.coverage(5.5, 7.5, 0.5, 5.5, 10.5, 5.5, 3);
        assertEquals(0, thin);
        assertTrue(thick > thin);
    }
}
