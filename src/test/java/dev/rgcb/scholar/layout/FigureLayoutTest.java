package dev.rgcb.scholar.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.editor.DocumentHit;
import dev.rgcb.scholar.editor.DocumentHitTester;
import dev.rgcb.scholar.editor.TextBoundary;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FigureLayoutTest {
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void figureLayoutPlacesScientificContentAboveGeneratedCaptionAndBoundsBoth() {
        var document = new Document(List.of(
                figure("graph-a", "Velocity graph"),
                figure("graph-b", "Acceleration graph")));

        var layout = layoutEngine.layout(document, 360, textMeasurer);
        var firstBlock = layout.blocks().get(0);
        var secondBlock = layout.blocks().get(1);
        var firstFigure = firstBlock.figure().orElseThrow();

        assertEquals(LaidOutBlockKind.FIGURE, firstBlock.kind());
        assertEquals(1, firstFigure.number());
        assertTrue(firstFigure.content().plot().isPresent());
        assertTrue(firstFigure.content().y() < firstFigure.captionY());
        assertTrue(firstFigure.height() >= firstFigure.content().height() + firstFigure.captionLines().get(0).height());
        assertEquals("Figure 1. Velocity graph", lineText(firstFigure.captionLines().get(0)));

        assertEquals(LaidOutBlockKind.FIGURE, secondBlock.kind());
        assertEquals(2, secondBlock.figure().orElseThrow().number());
        assertTrue(firstBlock.y() < secondBlock.y());
    }

    @Test
    void figureCaptionHitTestingSelectsWholeFigureBlockNotEditableText() {
        var layout = layoutEngine.layout(new Document(List.of(figure("graph", "Velocity graph"))), 360, textMeasurer);
        var figureBlock = layout.blocks().get(0);
        var figure = figureBlock.figure().orElseThrow();
        var hitTester = new DocumentHitTester();

        assertEquals(DocumentHit.block(0), hitTester.hit(layout, figure.x() + 4, figure.captionY() + 1, textMeasurer));
        assertTrue(hitTester.hitTest(layout, figure.x() + 4, figure.captionY() + 1, textMeasurer).isEmpty());
    }

    private static FigureBlock figure(String id, String caption) {
        return new FigureBlock(
                id,
                new PlotBlock(PlotDefinition.of(
                        "Plot",
                        AxisDefinition.linear("x"),
                        AxisDefinition.linear("y"),
                        List.of(new PlotSeries("Series", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(1, 1)))))),
                new InlineContent(List.of(new Text(caption, Set.of()))));
    }

    private static String lineText(LaidOutLine line) {
        return line.textRuns().stream()
                .map(LaidOutText::text)
                .reduce("", String::concat);
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            return TextBoundary.characterCount(text) * 8;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return 10;
        }
    }
}
