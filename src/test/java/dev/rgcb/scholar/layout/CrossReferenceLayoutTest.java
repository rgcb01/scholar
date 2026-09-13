package dev.rgcb.scholar.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CrossReferenceLayoutTest {
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();

    @Test
    void inlineCrossReferenceLaysOutAsResolvedTextWithAtomicSourceRange() {
        var document = document(
                paragraph(new Text("See ", Set.of()), ref("velocity"), new Text(".", Set.of())),
                figure("velocity", "Velocity graph"));

        var block = layoutEngine.layout(document, 200, new FixedTextMeasurer()).blocks().get(0);

        assertEquals("See Figure 1.", lineText(block.lines().get(0)));
        var referenceRun = block.lines().get(0).textRuns().get(1);
        assertEquals(" Figure", referenceRun.text());
        assertEquals(3, referenceRun.sourceStart());
        assertEquals(5, referenceRun.sourceEnd());
    }

    @Test
    void figureCaptionCanLayoutCrossReferenceText() {
        var document = document(
                figure("setup", "Setup graph"),
                new FigureBlock("velocity", plot(), inline(new Text("See ", Set.of()), ref("setup"))));

        var caption = layoutEngine.layout(document, 200, new FixedTextMeasurer()).blocks().get(1).figure().orElseThrow().captionLines();

        assertEquals("Figure 2. See Figure 1", lineText(caption.get(0)));
    }

    private static String lineText(LaidOutLine line) {
        return line.textRuns().stream().map(LaidOutText::text).reduce("", String::concat);
    }

    private static CrossReference ref(String id) {
        return new CrossReference(CrossReferenceTargetKind.FIGURE, id);
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(InlineNode... nodes) {
        return new Paragraph(new InlineContent(List.of(nodes)));
    }

    private static FigureBlock figure(String id, String caption) {
        return new FigureBlock(id, plot(), inline(new Text(caption, Set.of())));
    }

    private static InlineContent inline(InlineNode... nodes) {
        return new InlineContent(List.of(nodes));
    }

    private static PlotBlock plot() {
        return new PlotBlock(PlotDefinition.of(
                "Plot",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("Series", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(1, 1))))));
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            return text.length() * 5;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return style.isHeading() ? 12 : 10;
        }
    }
}
