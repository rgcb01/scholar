package dev.rgcb.scholar.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.markdown.MarkdownSerializer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FigureModelTest {
    @Test
    void figureStoresStableIdContentAndCaptionWithoutDisplayNumber() {
        var plot = plot("Motion");
        var figure = new FigureBlock(" velocity-graph ", plot, caption("Velocity graph"));

        assertEquals("velocity-graph", figure.id());
        assertEquals(plot, figure.content());
        assertEquals("Velocity graph", ((Text) figure.caption().nodes().get(0)).content());
    }

    @Test
    void figureAcceptsOnlySupportedScientificVisualBlocksForThisSlice() {
        assertTrue(FigureBlock.supportsContent(plot("Plot")));
        assertFalse(FigureBlock.supportsContent(paragraph("Text")));

        assertThrows(IllegalArgumentException.class, () -> new FigureBlock("text", paragraph("Text"), caption("caption")));
        assertThrows(IllegalArgumentException.class, () -> new FigureBlock(" ", plot("Plot"), caption("caption")));
        assertThrows(NullPointerException.class, () -> new FigureBlock("id", null, caption("caption")));
        assertThrows(NullPointerException.class, () -> new FigureBlock("id", plot("Plot"), null));
    }

    @Test
    void figureNumberingIsDerivedFromDocumentOrderAndRenumbersAfterInsertion() {
        var first = new FigureBlock("first", plot("First"), caption("First"));
        var second = new FigureBlock("second", plot("Second"), caption("Second"));
        var inserted = new FigureBlock("inserted", plot("Inserted"), caption("Inserted"));
        var document = new Document(List.of(paragraph("Before"), first, paragraph("Middle"), second));
        var updated = new Document(List.of(paragraph("Before"), first, inserted, paragraph("Middle"), second));

        assertEquals(List.of(1, 2), FigureNumbering.numbers(document));
        assertEquals(1, FigureNumbering.numberFor(document, 1).orElseThrow());
        assertTrue(FigureNumbering.numberFor(document, 0).isEmpty());

        assertEquals(List.of(1, 2, 3), FigureNumbering.numbers(updated));
        assertEquals(3, FigureNumbering.numberFor(updated, 4).orElseThrow());
    }

    @Test
    void markdownSerializerExplicitlyRejectsFigureBlock() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> new MarkdownSerializer().serialize(new Document(List.of(
                        new FigureBlock("figure", plot("Plot"), caption("Caption"))))));

        assertTrue(error.getMessage().contains("figure"));
    }

    private static PlotBlock plot(String title) {
        return new PlotBlock(PlotDefinition.of(
                title,
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("Series", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(1, 1))))));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(caption(text));
    }

    private static InlineContent caption(String text) {
        return new InlineContent(List.of(new Text(text, Set.of())));
    }
}
