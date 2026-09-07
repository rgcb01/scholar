package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.editor.TextBoundary;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.plot.layout.PlotLayoutEngine;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlotHitTesterTest {
    private final TextMeasurer measurer = new FixedTextMeasurer();
    private final PlotHitTester hitTester = new PlotHitTester();

    @Test
    void titleAndAxisLabelsMapToPropertyTargets() {
        var plot = layout();
        var title = plot.title().orElseThrow();
        var x = plot.xAxisLabel().orElseThrow();
        var y = plot.yAxisLabel().orElseThrow();

        assertEquals(new PlotPropertyTarget(PlotProperty.TITLE), hitTester.hit(plot, title.x() + 1, title.y() + 1));
        assertEquals(new PlotPropertyTarget(PlotProperty.X_AXIS_LABEL), hitTester.hit(plot, x.x() + 1, x.y() + 1));
        assertEquals(new PlotPropertyTarget(PlotProperty.Y_AXIS_LABEL), hitTester.hit(plot, y.x() + 1, y.y() + 1));
    }

    @Test
    void visiblePointMapsToExactAuthoredPointTarget() {
        var plot = layout();
        var point = plot.series().get(1).points().get(0);
        assertEquals(new PlotPointTarget(1, point.sourcePointIndex()), hitTester.hit(plot, point.x(), point.y()));
    }

    @Test
    void legendMapsToSeriesTarget() {
        var plot = layout();
        var item = plot.legend().orElseThrow().items().get(0);
        assertEquals(new PlotSeriesTarget(0), hitTester.hit(plot, item.label().x() + 1, item.label().y() + 1));
    }

    @Test
    void lineSegmentMapsToSeriesWhenNotNearPoint() {
        var plot = layout();
        var segment = plot.series().get(0).lineSegments().get(0);
        var x = (segment.x1() + segment.x2()) / 2;
        var y = (segment.y1() + segment.y2()) / 2;
        assertInstanceOf(PlotSeriesTarget.class, hitTester.hit(plot, x, y));
    }

    @Test
    void emptyPlotAreaFallsBackToTitleTarget() {
        var plot = layout();
        assertEquals(new PlotPropertyTarget(PlotProperty.TITLE),
                hitTester.hit(plot, plot.plotAreaX() + plot.plotAreaWidth() / 2, plot.plotAreaY() + 4));
    }

    private dev.rgcb.scholar.plot.layout.LaidOutPlot layout() {
        var block = new PlotBlock(new PlotDefinition(
                "Motion Plot",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(
                        new PlotSeries("Motion", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(2, 4))),
                        new PlotSeries("Samples", PlotSeriesKind.SCATTER, List.of(new DataPoint(1, 1)))),
                true,
                true,
                PlotDefinition.DEFAULT_HEIGHT));
        return new PlotLayoutEngine().layout(block, 0, 0, 0, 320, measurer);
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            return TextBoundary.characterCount(text) * 7;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return 10;
        }
    }
}
