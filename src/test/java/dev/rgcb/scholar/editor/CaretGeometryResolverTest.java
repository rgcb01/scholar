package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.LaidOutText;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CaretGeometryResolverTest {
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();
    private final CaretGeometryResolver resolver = new CaretGeometryResolver();

    @Test
    void layoutTextRunsCarrySourceRanges() {
        var layout = layoutEngine.layout(document("alpha beta gamma"), 60, textMeasurer);

        var ranges = layout.blocks().get(0).lines().stream()
                .flatMap(line -> line.textRuns().stream())
                .map(run -> List.of(run.sourceBlockIndex(), run.sourceStart(), run.sourceEnd()))
                .toList();

        assertTrue(ranges.contains(List.of(0, 0, 5)));
        assertTrue(ranges.stream().anyMatch(range -> range.get(0) == 0 && range.get(1) < range.get(2)));
    }

    @Test
    void resolvesCaretAtParagraphStartMiddleAndEnd() {
        var layout = layoutEngine.layout(document("alpha beta"), 80, textMeasurer);

        var start = resolver.resolve(new DocumentPosition(0, 0), layout, textMeasurer);
        var middle = resolver.resolve(new DocumentPosition(0, 3), layout, textMeasurer);
        var end = resolver.resolve(new DocumentPosition(0, 10), layout, textMeasurer);

        assertEquals(0, start.x());
        assertEquals(15, middle.x());
        assertEquals(50, end.x());
    }

    @Test
    void resolvesCaretAcrossWrappedLines() {
        var layout = layoutEngine.layout(document("alpha beta gamma"), 50, textMeasurer);

        var firstLineEnd = resolver.resolve(new DocumentPosition(0, 10), layout, textMeasurer);
        var secondLineMiddle = resolver.resolve(new DocumentPosition(0, 13), layout, textMeasurer);

        assertEquals(0, firstLineEnd.y());
        assertTrue(secondLineMiddle.y() > firstLineEnd.y());
    }

    @Test
    void relayoutAfterLongerTextChangesCaretGeometry() {
        var shortLayout = layoutEngine.layout(document("short"), 60, textMeasurer);
        var longLayout = layoutEngine.layout(document("short alpha beta gamma"), 60, textMeasurer);

        var shortCaret = resolver.resolve(new DocumentPosition(0, 5), shortLayout, textMeasurer);
        var longCaret = resolver.resolve(new DocumentPosition(0, 22), longLayout, textMeasurer);

        assertTrue(longCaret.y() >= shortCaret.y());
    }

    @Test
    void resolvesCaretAfterTrailingSpace() {
        var layout = layoutEngine.layout(document("abc "), 80, textMeasurer);

        var caret = resolver.resolve(new DocumentPosition(0, 4), layout, textMeasurer);

        assertEquals(20, caret.x());
    }

    @Test
    void resolvesCaretInEmptyParagraphFromEmptyInlineContent() {
        var layout = layoutEngine.layout(
                new Document(List.of(new Paragraph(new InlineContent(List.of())))),
                80,
                textMeasurer);

        var caret = resolver.resolve(new DocumentPosition(0, 0), layout, textMeasurer);

        assertEquals(0, caret.x());
        assertEquals(0, caret.y());
        assertEquals(10, caret.height());
    }

    @Test
    void resolvesCaretInEmptyHeadingUsingHeadingGeometry() {
        var layout = layoutEngine.layout(
                new Document(List.of(new Heading(2, new InlineContent(List.of())))),
                80,
                textMeasurer);

        var caret = resolver.resolve(new DocumentPosition(0, 0), layout, textMeasurer);

        assertEquals(20, caret.x());
        assertEquals(layout.blocks().get(0).y(), caret.y());
        assertEquals(10, caret.height());
    }

    @Test
    void resolvesCaretInsideHeading() {
        var layout = layoutEngine.layout(new Document(List.of(new Heading(2, new InlineContent(List.of((InlineNode) new Text("alpha beta", Set.of())))))), 80, textMeasurer);

        var start = resolver.resolve(new DocumentPosition(0, 0), layout, textMeasurer);
        var middle = resolver.resolve(new DocumentPosition(0, 5), layout, textMeasurer);
        var end = resolver.resolve(new DocumentPosition(0, 10), layout, textMeasurer);

        assertEquals(20, start.x());
        assertEquals(45, middle.x());
        assertEquals(70, end.x());
    }

    private static Document document(String text) {
        return new Document(List.of(new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))))));
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override
        public int measureWidth(String text, TextStyle style) {
            return TextBoundary.characterCount(text) * 5;
        }

        @Override
        public int lineHeight(TextStyle style) {
            return 10;
        }
    }
}
