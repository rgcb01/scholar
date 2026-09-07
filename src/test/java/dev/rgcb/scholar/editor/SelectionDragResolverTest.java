package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.math.MathIdentifier;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SelectionDragResolverTest {
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();
    private final SelectionDragResolver resolver = new SelectionDragResolver();

    @Test
    void dragsDownAcrossAdjacentEditableBlocks() {
        var layout = layoutEngine.layout(document(paragraph(text("abc")), heading(2, text("def")), paragraph(text("ghi"))), 100, textMeasurer);

        var position = resolver.resolve(layout, new DocumentPosition(0, 1), 15, layout.blocks().get(2).y() + 5, textMeasurer);

        assertEquals(new DocumentPosition(2, 2), position.orElseThrow());
    }

    @Test
    void dragsUpAcrossAdjacentEditableBlocks() {
        var layout = layoutEngine.layout(document(paragraph(text("abc")), paragraph(text("def"))), 100, textMeasurer);

        var position = resolver.resolve(layout, new DocumentPosition(1, 2), 5, 5, textMeasurer);

        assertEquals(new DocumentPosition(0, 1), position.orElseThrow());
    }

    @Test
    void gapMidpointChoosesPreviousEndThenNextStart() {
        var layout = layoutEngine.layout(document(paragraph(text("abc")), paragraph(text("def"))), 100, textMeasurer);
        var first = layout.blocks().get(0);
        var second = layout.blocks().get(1);
        var gapStart = first.y() + first.height();
        var gapEnd = second.y();
        var midpoint = gapStart + (gapEnd - gapStart) / 2;

        var upper = resolver.resolve(layout, new DocumentPosition(0, 1), 0, midpoint - 1, textMeasurer);
        var lower = resolver.resolve(layout, new DocumentPosition(0, 1), 0, midpoint, textMeasurer);

        assertEquals(new DocumentPosition(0, 3), upper.orElseThrow());
        assertEquals(new DocumentPosition(1, 0), lower.orElseThrow());
    }

    @Test
    void equationBarrierClampsDragFromAboveAndBelow() {
        var layout = layoutEngine.layout(
                document(paragraph(text("abc")), new EquationBlock(new MathIdentifier("x")), paragraph(text("def"))),
                100,
                textMeasurer,
                (content, kind) -> new dev.rgcb.scholar.math.layout.MathTextMetrics(10, 7, 3));

        var fromAbove = resolver.resolve(layout, new DocumentPosition(0, 1), 10, layout.blocks().get(2).y() + 5, textMeasurer);
        var fromBelow = resolver.resolve(layout, new DocumentPosition(2, 1), 10, layout.blocks().get(0).y() + 5, textMeasurer);

        assertEquals(new DocumentPosition(0, 3), fromAbove.orElseThrow());
        assertEquals(new DocumentPosition(2, 0), fromBelow.orElseThrow());
    }

    @Test
    void emptyBlockParticipatesInDragRegion() {
        var layout = layoutEngine.layout(document(
                paragraph(text("A")),
                new Paragraph(new InlineContent(List.of())),
                paragraph(text("B"))), 100, textMeasurer);

        var empty = resolver.resolve(layout, new DocumentPosition(0, 1), 0, layout.blocks().get(1).y() + 5, textMeasurer);
        var beyond = resolver.resolve(layout, new DocumentPosition(0, 1), 0, layout.blocks().get(2).y(), textMeasurer);

        assertEquals(new DocumentPosition(1, 0), empty.orElseThrow());
        assertEquals(new DocumentPosition(2, 0), beyond.orElseThrow());
    }

    @Test
    void unsupportedAnchorCannotResolveDragSelection() {
        var layout = layoutEngine.layout(
                document(new EquationBlock(new MathIdentifier("x"))),
                100,
                textMeasurer,
                (content, kind) -> new dev.rgcb.scholar.math.layout.MathTextMetrics(10, 7, 3));

        assertTrue(resolver.resolve(layout, new DocumentPosition(0, 0), 0, 0, textMeasurer).isEmpty());
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(Text... text) {
        return new Paragraph(new InlineContent(List.of(text).stream().map(InlineNode.class::cast).toList()));
    }

    private static Heading heading(int level, Text... text) {
        return new Heading(level, new InlineContent(List.of(text).stream().map(InlineNode.class::cast).toList()));
    }

    private static Text text(String content) {
        return new Text(content, Set.of());
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            return TextBoundary.characterCount(text) * 10;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return 10;
        }
    }
}
