package dev.rgcb.scholar.plot.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.AxisRange;
import dev.rgcb.scholar.plot.AxisScale;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlotRangeResolverTest {
    private final PlotRangeResolver resolver = new PlotRangeResolver();

    @Test
    void explicitRangesWinOverData() {
        var definition = definition(
                new AxisDefinition("x", Optional.of(new AxisRange(-10, 10)), AxisScale.LINEAR),
                new AxisDefinition("y", Optional.of(new AxisRange(100, 200)), AxisScale.LINEAR),
                List.of(new DataPoint(0, 0), new DataPoint(1, 1)));

        var ranges = resolver.resolve(definition);

        assertEquals(new AxisRange(-10, 10), ranges.xRange());
        assertEquals(new AxisRange(100, 200), ranges.yRange());
    }

    @Test
    void automaticRangeUsesAllSeriesWithoutForcingZero() {
        var definition = new PlotDefinition(
                "",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(
                        new PlotSeries("A", PlotSeriesKind.LINE, List.of(new DataPoint(10, 100), new DataPoint(20, 120))),
                        new PlotSeries("B", PlotSeriesKind.SCATTER, List.of(new DataPoint(15, 80), new DataPoint(18, 110)))),
                false,
                true,
                180);

        var ranges = resolver.resolve(definition);

        assertEquals(9.5, ranges.xRange().min(), 1e-9);
        assertEquals(20.5, ranges.xRange().max(), 1e-9);
        assertEquals(78.0, ranges.yRange().min(), 1e-9);
        assertEquals(122.0, ranges.yRange().max(), 1e-9);
        assertTrue(ranges.xRange().min() > 0.0);
        assertTrue(ranges.yRange().min() > 0.0);
    }

    @Test
    void automaticRangePreservesNegativeValuesAndAddsFivePercentPadding() {
        var ranges = resolver.resolve(definition(
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new DataPoint(-5, -10), new DataPoint(5, 10))));

        assertEquals(-5.5, ranges.xRange().min(), 1e-9);
        assertEquals(5.5, ranges.xRange().max(), 1e-9);
        assertEquals(-11.0, ranges.yRange().min(), 1e-9);
        assertEquals(11.0, ranges.yRange().max(), 1e-9);
    }

    @Test
    void zeroConstantRangeExpandsToMinusOnePlusOne() {
        var ranges = resolver.resolve(definition(
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new DataPoint(0, 0))));

        assertEquals(new AxisRange(-1, 1), ranges.xRange());
        assertEquals(new AxisRange(-1, 1), ranges.yRange());
    }

    @Test
    void nonZeroConstantRangeExpandsSymmetrically() {
        var ranges = resolver.resolve(definition(
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new DataPoint(20, -4))));

        assertEquals(19.0, ranges.xRange().min(), 1e-9);
        assertEquals(21.0, ranges.xRange().max(), 1e-9);
        assertEquals(-4.2, ranges.yRange().min(), 1e-9);
        assertEquals(-3.8, ranges.yRange().max(), 1e-9);
    }

    @Test
    void emptyPlotUsesStableZeroToOneDefaults() {
        var definition = new PlotDefinition(
                "",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("empty", PlotSeriesKind.LINE, List.of())),
                false,
                false,
                180);

        var ranges = resolver.resolve(definition);

        assertEquals(new AxisRange(0, 1), ranges.xRange());
        assertEquals(new AxisRange(0, 1), ranges.yRange());
    }

    @Test
    void veryLargeFiniteOppositeValuesRemainAValidRange() {
        var ranges = resolver.resolve(definition(
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new DataPoint(-Double.MAX_VALUE, 0), new DataPoint(Double.MAX_VALUE, 1))));

        assertEquals(-Double.MAX_VALUE, ranges.xRange().min());
        assertEquals(Double.MAX_VALUE, ranges.xRange().max());
    }

    private static PlotDefinition definition(AxisDefinition xAxis, AxisDefinition yAxis, List<DataPoint> points) {
        return new PlotDefinition(
                "",
                xAxis,
                yAxis,
                List.of(new PlotSeries("series", PlotSeriesKind.LINE, points)),
                false,
                true,
                180);
    }
    @Test
    void constantSmallestSubnormalValueStillExpandsToAValidFiniteRange() {
        var ranges = resolver.resolve(definition(
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new DataPoint(Double.MIN_VALUE, Double.MIN_VALUE))));

        assertTrue(Double.isFinite(ranges.xRange().min()));
        assertTrue(Double.isFinite(ranges.xRange().max()));
        assertTrue(ranges.xRange().min() < ranges.xRange().max());
        assertTrue(ranges.yRange().min() < ranges.yRange().max());
    }

}
