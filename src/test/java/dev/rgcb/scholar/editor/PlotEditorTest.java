package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlotEditorTest {
    private final PlotEditor editor = new PlotEditor();

    @Test
    void navigationTraversesPropertiesSeriesAndPointsInAuthoredOrder() {
        var plot = plot();
        PlotEditTarget target = editor.firstTarget(plot);
        assertEquals(new PlotPropertyTarget(PlotProperty.TITLE), target);
        target = editor.nextTarget(plot, target);
        assertEquals(new PlotPropertyTarget(PlotProperty.X_AXIS_LABEL), target);
        target = editor.nextTarget(plot, target);
        assertEquals(new PlotPropertyTarget(PlotProperty.Y_AXIS_LABEL), target);
        target = editor.nextTarget(plot, target);
        assertEquals(new PlotSeriesTarget(0), target);
        target = editor.nextTarget(plot, target);
        assertEquals(new PlotPointTarget(0, 0), target);
        target = editor.nextTarget(plot, target);
        assertEquals(new PlotPointTarget(0, 1), target);
        target = editor.nextTarget(plot, target);
        assertEquals(new PlotSeriesTarget(1), target);
        assertEquals(target, editor.nextTarget(plot, target));
        assertEquals(new PlotPointTarget(0, 1), editor.previousTarget(plot, target));
    }

    @Test
    void editsTitleAxisLabelsAndSeriesNameWithoutChangingOtherState() {
        var plot = plot();
        var title = editor.setText(plot, new PlotPropertyTarget(PlotProperty.TITLE), "Edited").plot();
        var x = editor.setText(title, new PlotPropertyTarget(PlotProperty.X_AXIS_LABEL), "Time (ms)").plot();
        var y = editor.setText(x, new PlotPropertyTarget(PlotProperty.Y_AXIS_LABEL), "Distance (m)").plot();
        var series = editor.setText(y, new PlotSeriesTarget(0), "Trajectory").plot();

        assertEquals("Edited", series.definition().title());
        assertEquals("Time (ms)", series.definition().xAxis().label());
        assertEquals("Distance (m)", series.definition().yAxis().label());
        assertEquals("Trajectory", series.definition().series().get(0).name());
        assertEquals(plot.definition().series().get(0).points(), series.definition().series().get(0).points());
    }

    @Test
    void editsFinitePointAndRejectsNonFiniteValues() {
        var plot = plot();
        var target = new PlotPointTarget(0, 1);
        var result = editor.setPoint(plot, target, -2.5, 7.25);

        assertTrue(result.changed());
        assertEquals(new DataPoint(-2.5, 7.25), result.plot().definition().series().get(0).points().get(1));
        assertThrows(IllegalArgumentException.class, () -> editor.setPoint(plot, target, Double.NaN, 1));
    }

    @Test
    void togglesGridAndLegendAsSemanticPlotProperties() {
        var plot = plot();
        var target = editor.firstTarget(plot);
        var grid = editor.toggleGrid(plot, target).plot();
        var legend = editor.toggleLegend(grid, target).plot();

        assertFalse(grid.definition().gridVisible());
        assertFalse(legend.definition().legendVisible());
    }

    @Test
    void addsLineAndScatterSeriesAndSelectsNewSeries() {
        var plot = plot();
        var line = editor.addSeries(plot, editor.firstTarget(plot), PlotSeriesKind.LINE);
        var scatter = editor.addSeries(line.plot(), line.target(), PlotSeriesKind.SCATTER);

        assertEquals(4, scatter.plot().definition().series().size());
        assertEquals(PlotSeriesKind.LINE, scatter.plot().definition().series().get(2).kind());
        assertEquals(PlotSeriesKind.SCATTER, scatter.plot().definition().series().get(3).kind());
        assertEquals(new PlotSeriesTarget(3), scatter.target());
        assertEquals("Series 4", scatter.plot().definition().series().get(3).name());
    }

    @Test
    void seriesKindCanChangeFromSeriesOrPointTarget() {
        var plot = plot();
        var changed = editor.setSeriesKind(plot, new PlotPointTarget(0, 0), PlotSeriesKind.SCATTER);
        assertTrue(changed.changed());
        assertEquals(PlotSeriesKind.SCATTER, changed.plot().definition().series().get(0).kind());
        assertFalse(editor.setSeriesKind(changed.plot(), changed.target(), PlotSeriesKind.SCATTER).changed());
    }

    @Test
    void addAndDeletePointPreserveSeriesAndReturnDeterministicTarget() {
        var plot = plot();
        var added = editor.addPoint(plot, new PlotSeriesTarget(1));
        assertEquals(new PlotPointTarget(1, 0), added.target());
        assertEquals(List.of(new DataPoint(0, 0)), added.plot().definition().series().get(1).points());

        var deleted = editor.deletePoint(added.plot(), added.target());
        assertEquals(new PlotSeriesTarget(1), deleted.target());
        assertTrue(deleted.plot().definition().series().get(1).points().isEmpty());
    }

    @Test
    void deleteSeriesPreservesOtherSeriesAndFallsBackToTitleWhenEmpty() {
        var plot = plot();
        var firstDeleted = editor.deleteSeries(plot, new PlotPointTarget(0, 0));
        assertEquals(1, firstDeleted.plot().definition().series().size());
        assertEquals("Samples", firstDeleted.plot().definition().series().get(0).name());
        assertEquals(new PlotSeriesTarget(0), firstDeleted.target());

        var secondDeleted = editor.deleteSeries(firstDeleted.plot(), firstDeleted.target());
        assertTrue(secondDeleted.plot().definition().series().isEmpty());
        assertEquals(new PlotPropertyTarget(PlotProperty.TITLE), secondDeleted.target());
    }

    @Test
    void validationRejectsStaleSeriesAndPointTargets() {
        var plot = plot();
        assertThrows(IllegalArgumentException.class, () -> editor.validateSelection(plot, new PlotSeriesTarget(9)));
        assertThrows(IllegalArgumentException.class, () -> editor.validateSelection(plot, new PlotPointTarget(0, 9)));
    }

    private static PlotBlock plot() {
        return new PlotBlock(new PlotDefinition(
                "Position vs Time",
                AxisDefinition.linear("Time (s)"),
                AxisDefinition.linear("Position (m)"),
                List.of(
                        new PlotSeries("Motion", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(1, 1))),
                        new PlotSeries("Samples", PlotSeriesKind.SCATTER, List.of())),
                true,
                true,
                PlotDefinition.DEFAULT_HEIGHT));
    }
}
