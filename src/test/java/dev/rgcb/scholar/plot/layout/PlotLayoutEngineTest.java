package dev.rgcb.scholar.plot.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.editor.TextBoundary;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlotLayoutEngineTest {
    private final PlotLayoutEngine engine = new PlotLayoutEngine();
    private final TextMeasurer measurer = new FixedTextMeasurer();

    @Test
    void staticLayoutUsesAvailableWidthAndAuthoredHeight() {
        var plot = plot(140);
        var layout = engine.layout(plot, 3, 0, 12, 240, measurer);

        assertEquals(3, layout.sourceBlockIndex());
        assertEquals(0, layout.x());
        assertEquals(12, layout.y());
        assertEquals(240, layout.width());
        assertEquals(140, layout.height());
        assertTrue(layout.plotAreaWidth() > 0);
        assertTrue(layout.plotAreaHeight() > 0);
    }

    @Test
    void layoutWidthRespondsToDocumentWidthWhileHeightStaysSemantic() {
        var plot = plot(150);
        var narrow = engine.layout(plot, 0, 0, 0, 120, measurer);
        var wide = engine.layout(plot, 0, 0, 0, 300, measurer);

        assertEquals(120, narrow.width());
        assertEquals(300, wide.width());
        assertEquals(150, narrow.height());
        assertEquals(150, wide.height());
        assertTrue(wide.plotAreaWidth() > narrow.plotAreaWidth());
    }

    @Test
    void titleAndAxisLabelsArePremeasuredInPureLayout() {
        var layout = engine.layout(plot(180), 0, 0, 0, 240, measurer);

        assertEquals("Motion Ω", layout.title().orElseThrow().text());
        assertEquals("Time (s)", layout.xAxisLabel().orElseThrow().text());
        assertEquals("Position (m)", layout.yAxisLabel().orElseThrow().text());
        assertTrue(layout.title().orElseThrow().width() > 0);
    }

    @Test
    void blankLabelsRemainAbsentWithoutChangingPlotBounds() {
        var definition = new PlotDefinition(
                "",
                AxisDefinition.linear(""),
                AxisDefinition.linear(""),
                List.of(),
                false,
                false,
                120);
        var layout = engine.layout(new PlotBlock(definition), 0, 0, 0, 180, measurer);

        assertTrue(layout.title().isEmpty());
        assertTrue(layout.xAxisLabel().isEmpty());
        assertTrue(layout.yAxisLabel().isEmpty());
        assertEquals(120, layout.height());
    }

    @Test
    void veryNarrowResponsiveWidthStillProducesPositivePlotArea() {
        var layout = engine.layout(plot(120), 0, 0, 0, 1, measurer);
        assertEquals(1, layout.width());
        assertEquals(1, layout.plotAreaWidth());
        assertTrue(layout.plotAreaHeight() > 0);
    }


    @Test
    void layoutResolvesAutomaticRangesTicksAndTransform() {
        var layout = engine.layout(plot(180), 0, 0, 0, 240, measurer);

        assertEquals(-0.05, layout.xRange().min(), 1e-9);
        assertEquals(1.05, layout.xRange().max(), 1e-9);
        assertEquals(-0.05, layout.yRange().min(), 1e-9);
        assertEquals(1.05, layout.yRange().max(), 1e-9);
        assertEquals(
                List.of("0", "0.5", "1"),
                layout.xTicks().stream().map(tick -> tick.label().text()).toList());
        assertEquals(
                List.of("0", "0.5", "1"),
                layout.yTicks().stream().map(tick -> tick.label().text()).toList());
        assertEquals(layout.plotAreaX(), Math.round(layout.transform().mapX(layout.xRange().min())));
        assertEquals(layout.plotAreaY(), Math.round(layout.transform().mapY(layout.yRange().max())));
    }

    @Test
    void explicitRangesDriveTicksAndCoordinateTransform() {
        var definition = new PlotDefinition(
                "Explicit",
                new AxisDefinition("x", java.util.Optional.of(new dev.rgcb.scholar.plot.AxisRange(-10, 10)), dev.rgcb.scholar.plot.AxisScale.LINEAR),
                new AxisDefinition("y", java.util.Optional.of(new dev.rgcb.scholar.plot.AxisRange(0, 100)), dev.rgcb.scholar.plot.AxisScale.LINEAR),
                List.of(new PlotSeries("Series A", PlotSeriesKind.LINE, List.of(new DataPoint(999, -999)))),
                true,
                true,
                180);
        var layout = engine.layout(new PlotBlock(definition), 0, 0, 0, 240, measurer);

        assertEquals(new dev.rgcb.scholar.plot.AxisRange(-10, 10), layout.xRange());
        assertEquals(new dev.rgcb.scholar.plot.AxisRange(0, 100), layout.yRange());
        assertEquals(layout.plotAreaX() + (layout.plotAreaWidth() - 1) / 2.0, layout.transform().mapX(0), 1e-9);
        assertEquals(layout.plotAreaY(), layout.transform().mapY(100), 1e-9);
    }

    @Test
    void tickLabelsReserveLeftAndBottomGuttersOutsidePlotArea() {
        var layout = engine.layout(plot(180), 0, 0, 0, 240, measurer);

        assertTrue(layout.plotAreaX() > layout.x());
        assertTrue(layout.xTicks().stream().allMatch(tick -> tick.label().y() >= layout.plotAreaY() + layout.plotAreaHeight()));
        assertTrue(layout.yTicks().stream().allMatch(tick -> tick.label().x() < layout.plotAreaX()));
    }

    @Test
    void yAxisLabelIsPlacedAbovePlotAreaInsteadOfOverlappingFrame() {
        var layout = engine.layout(plot(180), 0, 0, 0, 240, measurer);
        var label = layout.yAxisLabel().orElseThrow();

        assertTrue(label.y() + label.height() <= layout.plotAreaY());
    }

    @Test
    void gridVisibilityIsCarriedIntoLaidOutPlot() {
        var visible = engine.layout(plot(180), 0, 0, 0, 240, measurer);
        var definition = new PlotDefinition(
                "",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(),
                false,
                false,
                180);
        var hidden = engine.layout(new PlotBlock(definition), 0, 0, 0, 240, measurer);

        assertTrue(visible.gridVisible());
        org.junit.jupiter.api.Assertions.assertFalse(hidden.gridVisible());
    }


    @Test
    void layoutIncludesLineAndScatterSeriesGeometry() {
        var definition = new PlotDefinition(
                "Mixed",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(
                        new PlotSeries("line", PlotSeriesKind.LINE, List.of(
                                new DataPoint(0, 0), new DataPoint(1, 1))),
                        new PlotSeries("scatter", PlotSeriesKind.SCATTER, List.of(
                                new DataPoint(0.25, 0.75), new DataPoint(0.75, 0.25)))),
                true,
                true,
                180);

        var layout = engine.layout(new PlotBlock(definition), 0, 0, 0, 240, measurer);

        assertEquals(2, layout.series().size());
        assertEquals(1, layout.series().get(0).lineSegments().size());
        assertEquals(2, layout.series().get(1).points().size());
        assertTrue(layout.series().get(1).lineSegments().isEmpty());
    }

    @Test
    void visibleNamedSeriesProduceAPlacedLegend() {
        var definition = new PlotDefinition(
                "Mixed",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(
                        new PlotSeries("Motion", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(1, 1))),
                        new PlotSeries("Samples", PlotSeriesKind.SCATTER, List.of(new DataPoint(0.5, 0.5)))),
                true,
                true,
                180);

        var layout = engine.layout(new PlotBlock(definition), 0, 0, 0, 240, measurer);
        var legend = layout.legend().orElseThrow();

        assertEquals(2, legend.items().size());
        assertEquals("Motion", legend.items().get(0).label().text());
        assertEquals("Samples", legend.items().get(1).label().text());
        assertTrue(legend.x() >= layout.plotAreaX());
        assertTrue(legend.y() >= layout.plotAreaY());
    }

    @Test
    void legendCanBeDisabledWithoutRemovingSeriesGeometry() {
        var definition = new PlotDefinition(
                "",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("A", PlotSeriesKind.LINE, List.of(
                        new DataPoint(0, 0), new DataPoint(1, 1)))),
                false,
                true,
                180);

        var layout = engine.layout(new PlotBlock(definition), 0, 0, 0, 240, measurer);

        assertTrue(layout.legend().isEmpty());
        assertEquals(1, layout.series().getFirst().lineSegments().size());
    }

    @Test
    void narrowPlotThinsXAxisTicksSoRenderedLabelsDoNotOverlap() {
        var definition = new PlotDefinition(
                "Narrow",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("S", PlotSeriesKind.LINE, List.of(
                        new DataPoint(0, 0),
                        new DataPoint(1_000_000, 1)))),
                false,
                true,
                180);
        var layout = engine.layout(new PlotBlock(definition), 0, 0, 0, 80, measurer);

        for (var i = 1; i < layout.xTicks().size(); i++) {
            var previous = layout.xTicks().get(i - 1).label();
            var current = layout.xTicks().get(i).label();
            assertTrue(previous.x() + previous.width() + PlotLayoutEngine.TICK_LABEL_MIN_GAP <= current.x());
        }
    }

    @Test
    void normalWidthKeepsUsefulTickDensityWhileRespectingGlobalBound() {
        var layout = engine.layout(plot(180), 0, 0, 0, 320, measurer);

        assertTrue(layout.xTicks().size() >= 2);
        assertTrue(layout.yTicks().size() >= 2);
        assertTrue(layout.xTicks().size() <= NiceTickGenerator.DEFAULT_TARGET_TICKS);
        assertTrue(layout.yTicks().size() <= NiceTickGenerator.DEFAULT_TARGET_TICKS);
    }

    private static PlotBlock plot(int height) {
        return new PlotBlock(new PlotDefinition(
                "Motion Ω",
                AxisDefinition.linear("Time (s)"),
                AxisDefinition.linear("Position (m)"),
                List.of(new PlotSeries("Series A", PlotSeriesKind.LINE, List.of(
                        new DataPoint(0, 0), new DataPoint(1, 1)))),
                true,
                true,
                height));
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            return TextBoundary.characterCount(text) * 6;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return 10;
        }
    }
}
