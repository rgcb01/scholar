package dev.rgcb.scholar.plot.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.plot.AxisRange;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlotSeriesLayoutEngineTest {
    private final PlotSeriesLayoutEngine engine = new PlotSeriesLayoutEngine();
    private final PlotCoordinateTransform transform = new PlotCoordinateTransform(
            new AxisRange(0, 10),
            new AxisRange(0, 10),
            10,
            20,
            101,
            101);

    @Test
    void lineSeriesFollowsAuthoredPointOrderAndCreatesConsecutiveSegments() {
        var series = new PlotSeries("line", PlotSeriesKind.LINE, List.of(
                new DataPoint(0, 0),
                new DataPoint(5, 5),
                new DataPoint(10, 10)));

        var layout = engine.layout(List.of(series), transform).getFirst();

        assertEquals(3, layout.points().size());
        assertEquals(2, layout.lineSegments().size());
        assertEquals(new LaidOutPlotLineSegment(0, 1, 10, 120, 60, 70), layout.lineSegments().get(0));
        assertEquals(new LaidOutPlotLineSegment(1, 2, 60, 70, 110, 20), layout.lineSegments().get(1));
    }

    @Test
    void scatterSeriesProducesVisibleMarkersWithoutLineSegments() {
        var series = new PlotSeries("samples", PlotSeriesKind.SCATTER, List.of(
                new DataPoint(0, 0),
                new DataPoint(5, 5),
                new DataPoint(10, 10)));

        var layout = engine.layout(List.of(series), transform).getFirst();

        assertEquals(3, layout.points().size());
        assertTrue(layout.lineSegments().isEmpty());
    }

    @Test
    void scatterOmitsPointsOutsideExplicitResolvedRange() {
        var series = new PlotSeries("samples", PlotSeriesKind.SCATTER, List.of(
                new DataPoint(-1, 5),
                new DataPoint(5, 5),
                new DataPoint(11, 5)));

        var layout = engine.layout(List.of(series), transform).getFirst();

        assertEquals(1, layout.points().size());
        assertEquals(1, layout.points().getFirst().sourcePointIndex());
        assertEquals(60, layout.points().getFirst().x());
        assertEquals(70, layout.points().getFirst().y());
    }

    @Test
    void lineCrossingRangeIsClippedToPlotArea() {
        var series = new PlotSeries("crossing", PlotSeriesKind.LINE, List.of(
                new DataPoint(-5, 5),
                new DataPoint(15, 5)));

        var layout = engine.layout(List.of(series), transform).getFirst();

        assertTrue(layout.points().isEmpty());
        assertEquals(List.of(new LaidOutPlotLineSegment(0, 1, 10, 70, 110, 70)), layout.lineSegments());
    }

    @Test
    void completelyOutsideLineSegmentIsOmitted() {
        var series = new PlotSeries("outside", PlotSeriesKind.LINE, List.of(
                new DataPoint(-5, 20),
                new DataPoint(15, 20)));

        var layout = engine.layout(List.of(series), transform).getFirst();

        assertTrue(layout.points().isEmpty());
        assertTrue(layout.lineSegments().isEmpty());
    }

    @Test
    void unsortedLineRetainsAuthoredDirection() {
        var series = new PlotSeries("reverse", PlotSeriesKind.LINE, List.of(
                new DataPoint(10, 0),
                new DataPoint(0, 10)));

        var segment = engine.layout(List.of(series), transform).getFirst().lineSegments().getFirst();

        assertEquals(110, segment.x1());
        assertEquals(120, segment.y1());
        assertEquals(10, segment.x2());
        assertEquals(20, segment.y2());
    }

    @Test
    void emptySeriesProducesEmptyGeometry() {
        var layout = engine.layout(
                List.of(new PlotSeries("empty", PlotSeriesKind.LINE, List.of())),
                transform).getFirst();

        assertTrue(layout.points().isEmpty());
        assertTrue(layout.lineSegments().isEmpty());
    }

    @Test
    void duplicateConsecutivePointsRemainAZeroLengthAuthoredSegment() {
        var series = new PlotSeries("duplicate", PlotSeriesKind.LINE, List.of(
                new DataPoint(5, 5),
                new DataPoint(5, 5)));

        var segment = engine.layout(List.of(series), transform).getFirst().lineSegments().getFirst();

        assertEquals(segment.x1(), segment.x2());
        assertEquals(segment.y1(), segment.y2());
    }

    @Test
    void automaticStyleAssignmentIsDeterministicAndUsesNonColorVisualRoles() {
        var input = new ArrayList<PlotSeries>();
        for (var i = 0; i < 6; i++) {
            input.add(new PlotSeries("S" + i, PlotSeriesKind.LINE, List.of()));
        }

        var laidOut = engine.layout(input, transform);

        assertEquals(PlotLinePattern.SOLID, laidOut.get(0).style().linePattern());
        assertEquals(PlotLinePattern.DASHED, laidOut.get(1).style().linePattern());
        assertEquals(PlotLinePattern.DOTTED, laidOut.get(2).style().linePattern());
        assertEquals(PlotLinePattern.DASH_DOT, laidOut.get(3).style().linePattern());
        assertEquals(PlotLinePattern.LONG_DASH, laidOut.get(4).style().linePattern());
        assertEquals(PlotLinePattern.DENSE_DOT, laidOut.get(5).style().linePattern());
        assertEquals(PlotMarkerShape.SQUARE, laidOut.get(0).style().markerShape());
        assertEquals(PlotMarkerShape.DIAMOND, laidOut.get(1).style().markerShape());
        assertEquals(PlotMarkerShape.CROSS, laidOut.get(2).style().markerShape());
        assertEquals(PlotMarkerShape.X, laidOut.get(3).style().markerShape());
        assertEquals(PlotMarkerShape.CIRCLE, laidOut.get(4).style().markerShape());
        assertEquals(PlotMarkerShape.TRIANGLE, laidOut.get(5).style().markerShape());
    }

    @Test
    void veryLargeFiniteValuesCanBeClippedWithoutOverflowingGeometry() {
        var hugeTransform = new PlotCoordinateTransform(
                new AxisRange(-Double.MAX_VALUE, Double.MAX_VALUE),
                new AxisRange(-1, 1),
                0,
                0,
                101,
                101);
        var series = new PlotSeries("huge", PlotSeriesKind.LINE, List.of(
                new DataPoint(-Double.MAX_VALUE, 0),
                new DataPoint(Double.MAX_VALUE, 0)));

        var layout = engine.layout(List.of(series), hugeTransform).getFirst();

        assertEquals(1, layout.lineSegments().size());
        assertEquals(0, layout.lineSegments().getFirst().x1());
        assertEquals(100, layout.lineSegments().getFirst().x2());
    }
}
