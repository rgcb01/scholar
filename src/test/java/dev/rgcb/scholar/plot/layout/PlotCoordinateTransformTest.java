package dev.rgcb.scholar.plot.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.plot.AxisRange;
import org.junit.jupiter.api.Test;

class PlotCoordinateTransformTest {
    @Test
    void mapsXMinMidMaxAcrossPlotArea() {
        var transform = new PlotCoordinateTransform(new AxisRange(0, 10), new AxisRange(0, 10), 20, 30, 101, 201);

        assertEquals(20.0, transform.mapX(0), 1e-9);
        assertEquals(70.0, transform.mapX(5), 1e-9);
        assertEquals(120.0, transform.mapX(10), 1e-9);
    }

    @Test
    void mapsYWithScientificUpwardDirection() {
        var transform = new PlotCoordinateTransform(new AxisRange(0, 10), new AxisRange(-10, 10), 20, 30, 101, 201);

        assertEquals(230.0, transform.mapY(-10), 1e-9);
        assertEquals(130.0, transform.mapY(0), 1e-9);
        assertEquals(30.0, transform.mapY(10), 1e-9);
    }

    @Test
    void doesNotClampValuesOutsideRange() {
        var transform = new PlotCoordinateTransform(new AxisRange(0, 10), new AxisRange(0, 10), 0, 0, 101, 101);
        assertTrue(transform.mapX(20) > 100.0);
        assertTrue(transform.mapY(20) < 0.0);
    }

    @Test
    void handlesExtremeFiniteRangesWithoutOverflowingToNan() {
        var transform = new PlotCoordinateTransform(
                new AxisRange(-Double.MAX_VALUE, Double.MAX_VALUE),
                new AxisRange(-Double.MAX_VALUE, Double.MAX_VALUE),
                0,
                0,
                101,
                101);

        assertEquals(50.0, transform.mapX(0), 1e-9);
        assertEquals(50.0, transform.mapY(0), 1e-9);
    }

    @Test
    void rejectsNonFiniteInputCoordinates() {
        var transform = new PlotCoordinateTransform(new AxisRange(0, 1), new AxisRange(0, 1), 0, 0, 10, 10);
        assertThrows(IllegalArgumentException.class, () -> transform.mapX(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> transform.mapY(Double.POSITIVE_INFINITY));
    }
}
