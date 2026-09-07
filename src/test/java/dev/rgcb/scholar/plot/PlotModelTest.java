package dev.rgcb.scholar.plot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.PlotBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlotModelTest {
    @Test
    void dataPointRequiresFiniteCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new DataPoint(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new DataPoint(0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new DataPoint(Double.POSITIVE_INFINITY, 0));
        assertThrows(IllegalArgumentException.class, () -> new DataPoint(0, Double.NEGATIVE_INFINITY));
    }

    @Test
    void dataPointAllowsNegativeAndVerySmallFiniteValues() {
        assertEquals(new DataPoint(-12.5, Double.MIN_VALUE), new DataPoint(-12.5, Double.MIN_VALUE));
    }

    @Test
    void axisRangeRequiresFiniteStrictlyIncreasingBounds() {
        assertEquals(new AxisRange(-5, 5), new AxisRange(-5, 5));
        assertThrows(IllegalArgumentException.class, () -> new AxisRange(1, 1));
        assertThrows(IllegalArgumentException.class, () -> new AxisRange(2, 1));
        assertThrows(IllegalArgumentException.class, () -> new AxisRange(Double.NEGATIVE_INFINITY, 1));
    }

    @Test
    void linearAxisMayUseAutomaticOrExplicitRange() {
        assertEquals(Optional.empty(), AxisDefinition.linear("Time (s)").explicitRange());
        var explicit = new AxisDefinition("x", Optional.of(new AxisRange(-1, 1)), AxisScale.LINEAR);
        assertEquals(new AxisRange(-1, 1), explicit.explicitRange().orElseThrow());
    }

    @Test
    void plotSeriesPreservesAuthoredPointOrderAndDefensivelyCopies() {
        var points = new ArrayList<>(List.of(
                new DataPoint(2, 4),
                new DataPoint(0, 0),
                new DataPoint(2, 9)));
        var series = new PlotSeries("Samples", PlotSeriesKind.SCATTER, points);
        points.clear();

        assertEquals(List.of(
                new DataPoint(2, 4),
                new DataPoint(0, 0),
                new DataPoint(2, 9)), series.points());
        assertThrows(UnsupportedOperationException.class, () -> series.points().add(new DataPoint(3, 3)));
    }

    @Test
    void emptyAndOnePointSeriesAreValid() {
        assertTrue(new PlotSeries("empty", PlotSeriesKind.LINE, List.of()).points().isEmpty());
        assertEquals(1, new PlotSeries("one", PlotSeriesKind.SCATTER, List.of(new DataPoint(1, 2))).points().size());
    }

    @Test
    void plotDefinitionDefensivelyCopiesSeriesAndUsesDefaultHeight() {
        var source = new ArrayList<PlotSeries>();
        source.add(new PlotSeries("A", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0))));
        var definition = PlotDefinition.of("Motion", AxisDefinition.linear("t"), AxisDefinition.linear("x"), source);
        source.clear();

        assertEquals(PlotDefinition.DEFAULT_HEIGHT, definition.height());
        assertEquals(1, definition.series().size());
        assertThrows(UnsupportedOperationException.class, () -> definition.series().clear());
    }

    @Test
    void plotDefinitionRejectsTooSmallHeight() {
        assertThrows(IllegalArgumentException.class, () -> new PlotDefinition(
                "Plot",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(),
                true,
                true,
                PlotDefinition.MIN_HEIGHT - 1));
    }

    @Test
    void plotBlockStoresSemanticDefinition() {
        var definition = PlotDefinition.of(
                "Δx vs θ",
                AxisDefinition.linear("θ"),
                AxisDefinition.linear("Δx (m)"),
                List.of(new PlotSeries("café λ", PlotSeriesKind.LINE, List.of(new DataPoint(-1, 2)))));
        assertEquals(definition, new PlotBlock(definition).definition());
    }

    @Test
    void plotBlockRejectsNullDefinition() {
        assertThrows(NullPointerException.class, () -> new PlotBlock(null));
    }
}
