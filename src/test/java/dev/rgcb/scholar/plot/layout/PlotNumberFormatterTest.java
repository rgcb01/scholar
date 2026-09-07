package dev.rgcb.scholar.plot.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlotNumberFormatterTest {
    @Test
    void formatsSmallestPositiveSubnormalWithoutThrowing() {
        assertEquals("4.9E-324", PlotNumberFormatter.format(Double.MIN_VALUE, Double.MIN_VALUE));
    }

    @Test
    void formatsVeryLargeFiniteValueDeterministically() {
        var formatted = PlotNumberFormatter.format(Double.MAX_VALUE, 1.0e307);
        assertTrue(formatted.endsWith("E308"));
        assertFalse(formatted.contains("E+"));
    }

    @Test
    void ordinaryDecimalFormattingRemainsCompact() {
        assertEquals("0.5", PlotNumberFormatter.format(0.5, 0.5));
        assertEquals("0", PlotNumberFormatter.format(-0.0, 0.5));
    }
}
