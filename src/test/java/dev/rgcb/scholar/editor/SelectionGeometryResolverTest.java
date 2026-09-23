package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.DocumentSettings;
import dev.rgcb.scholar.document.LayoutSectionBreak;
import dev.rgcb.scholar.document.ColumnLayout;
import dev.rgcb.scholar.client.render.DocumentViewTransform;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.math.MathIdentifier;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SelectionGeometryResolverTest {
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();
    private final SelectionGeometryResolver resolver = new SelectionGeometryResolver();

    @Test
    void resolvesPartialRunSelection() {
        var layout = layoutEngine.layout(document(paragraph(text("abcdef"))), 100, textMeasurer);

        var rects = resolver.resolve(range(1, 4), layout, textMeasurer);

        assertEquals(List.of(new SelectionRect(10, 0, 30, 10)), rects);
    }

    @Test
    void paginatedPartialSelectionUsesPlacedRunOriginOnlyOnce() {
        var source = new Document(List.of(paragraph(text("abcdef"))), List.of(), DocumentSettings.blank());
        var layout = layoutEngine.layoutPaginated(source, textMeasurer, null);
        var run = layout.blocks().getFirst().lines().getFirst().textRuns().getFirst();
        assertTrue(run.x() > 0);
        var rect = resolver.resolve(range(1, 4), layout, textMeasurer).getFirst();
        assertEquals(new SelectionRect(run.x() + 10, run.y(), 30, layout.blocks().getFirst().lines().getFirst().height()), rect);
        for (var zoom : new double[]{0.75, 1.0, 1.25, 1.5, 2.0}) {
            var view = new DocumentViewTransform(35, 50, zoom);
            assertEquals(view.screenX(35 + run.x() + 10), view.screenX(35 + rect.x()), 1.0e-9);
            assertEquals(30 * zoom, view.screenX(35 + rect.x() + rect.width()) - view.screenX(35 + rect.x()), 1.0e-9);
        }
    }

    @Test
    void paginatedTwoColumnSelectionUsesActualColumnOrigin() {
        var source = new Document(List.of(new LayoutSectionBreak(ColumnLayout.two()), paragraph(text("abcdef"))),
                List.of(), DocumentSettings.blank());
        var layout = layoutEngine.layoutPaginated(source, textMeasurer, null);
        var run = layout.blocks().get(1).lines().getFirst().textRuns().getFirst();
        var rect = resolver.resolve(new DocumentRange(new DocumentPosition(1, 2), new DocumentPosition(1, 5)),
                layout, textMeasurer).getFirst();
        assertEquals(run.x() + 20, rect.x());
        assertEquals(30, rect.width());
    }

    @Test
    void selectedWidthUsesDifferenceOfPrefixAdvances() {
        TextMeasurer kerned = new TextMeasurer() {
            @Override public int measureWidth(String text, TextStyle style) {
                return text.length() * 10 - (text.startsWith("AV") ? 2 : 0);
            }
            @Override public int lineHeight(TextStyle style) { return 12; }
        };
        var layout = layoutEngine.layout(document(paragraph(text("AVA"))), 100, kerned);
        var rect = resolver.resolve(range(1, 2), layout, kerned).getFirst();
        assertEquals(10, rect.x());
        assertEquals(8, rect.width());
    }

    @Test
    void paginatedWrappedSelectionStartsAndEndsAtActualCarets() {
        var content = "alpha beta gamma delta epsilon ".repeat(35);
        var source = new Document(List.of(paragraph(text(content))), List.of(), DocumentSettings.blank());
        var layout = layoutEngine.layoutPaginated(source, textMeasurer, null);
        var start = new DocumentPosition(0, 3);
        var end = new DocumentPosition(0, content.length() - 3);
        var rects = resolver.resolve(new DocumentRange(start, end), layout, textMeasurer);
        var caret = new CaretGeometryResolver();
        assertTrue(layout.blocks().getFirst().lines().size() > 1);
        assertEquals(caret.resolve(start, layout, textMeasurer).x(), rects.getFirst().x());
        assertEquals(caret.resolve(end, layout, textMeasurer).x(),
                rects.getLast().x() + rects.getLast().width());
        for (var rect : rects) {
            var line = layout.blocks().getFirst().lines().stream()
                    .filter(candidate -> candidate.y() == rect.y()).findFirst().orElseThrow();
            assertTrue(rect.x() >= line.x());
            assertTrue(rect.x() + rect.width() <= line.x() + line.width());
        }
    }

    @Test
    void resolvesSelectionsAcrossMarkedRuns() {
        var layout = layoutEngine.layout(document(paragraph(
                text("ab"),
                text("cd", TextMark.BOLD),
                text("ef", TextMark.ITALIC))), 100, textMeasurer);

        var rects = resolver.resolve(range(1, 5), layout, textMeasurer);

        assertEquals(List.of(
                new SelectionRect(10, 0, 10, 10),
                new SelectionRect(20, 0, 20, 10),
                new SelectionRect(40, 0, 10, 10)), rects);
    }

    @Test
    void resolvesWrappedSelectionAsMultipleRectangles() {
        var layout = layoutEngine.layout(document(paragraph(text("alpha beta gamma"))), 60, textMeasurer);

        var rects = resolver.resolve(range(3, 13), layout, textMeasurer);

        assertEquals(List.of(
                new SelectionRect(30, 0, 20, 10),
                new SelectionRect(0, 10, 40, 10),
                new SelectionRect(0, 20, 20, 10)), rects);
    }

    @Test
    void forwardAndBackwardRangesHaveSameGeometry() {
        var layout = layoutEngine.layout(document(paragraph(text("abcdef"))), 100, textMeasurer);

        var forward = resolver.resolve(DocumentRange.between(new DocumentPosition(0, 1), new DocumentPosition(0, 5)), layout, textMeasurer);
        var backward = resolver.resolve(DocumentRange.between(new DocumentPosition(0, 5), new DocumentPosition(0, 1)), layout, textMeasurer);

        assertEquals(forward, backward);
    }

    @Test
    void preservesTrailingSpaceGeometry() {
        var layout = layoutEngine.layout(document(paragraph(text("abc "))), 100, textMeasurer);

        var rects = resolver.resolve(range(3, 4), layout, textMeasurer);

        assertEquals(List.of(new SelectionRect(30, 0, 10, 10)), rects);
    }

    @Test
    void resolvesUnicodeUserCharacterGeometry() {
        var layout = layoutEngine.layout(document(paragraph(text("aΔe\u0301z"))), 100, textMeasurer);

        var rects = resolver.resolve(range(1, 3), layout, textMeasurer);

        assertEquals(List.of(new SelectionRect(10, 0, 20, 10)), rects);
    }

    @Test
    void resolvesHeadingSelectionAcrossMarksAndWrapping() {
        var layout = layoutEngine.layout(document(heading(2,
                text("alpha "),
                text("beta", TextMark.BOLD),
                text(" gamma", TextMark.ITALIC))), 60, textMeasurer);

        var rects = resolver.resolve(range(3, 14), layout, textMeasurer);

        assertEquals(List.of(
                new SelectionRect(30, 10, 20, 10),
                new SelectionRect(0, 20, 40, 10),
                new SelectionRect(0, 30, 30, 10)), rects);
    }

    @Test
    void resolvesMultiBlockSelectionAcrossTwoBlocks() {
        var layout = layoutEngine.layout(document(paragraph(text("abc")), paragraph(text("def"))), 100, textMeasurer);

        var rects = resolver.resolve(
                new DocumentRange(new DocumentPosition(0, 1), new DocumentPosition(1, 2)),
                layout,
                textMeasurer);

        assertEquals(List.of(
                new SelectionRect(10, 0, 20, 10),
                new SelectionRect(0, 20, 20, 10)), rects);
    }

    @Test
    void resolvesMultiBlockSelectionWithFullMiddleBlock() {
        var layout = layoutEngine.layout(document(paragraph(text("abc")), heading(2, text("TITLE")), paragraph(text("xyz"))), 100, textMeasurer);

        var rects = resolver.resolve(
                new DocumentRange(new DocumentPosition(0, 2), new DocumentPosition(2, 1)),
                layout,
                textMeasurer);

        assertEquals(3, rects.size());
        assertEquals(new SelectionRect(20, 0, 10, 10), rects.get(0));
        assertEquals(50, rects.get(1).width());
        assertEquals(new SelectionRect(0, layout.blocks().get(2).y(), 10, 10), rects.get(2));
    }

    @Test
    void backwardAndForwardMultiBlockRangesHaveSameGeometry() {
        var layout = layoutEngine.layout(document(paragraph(text("abc")), paragraph(text("def"))), 100, textMeasurer);

        var forward = resolver.resolve(DocumentRange.between(new DocumentPosition(0, 1), new DocumentPosition(1, 2)), layout, textMeasurer);
        var backward = resolver.resolve(DocumentRange.between(new DocumentPosition(1, 2), new DocumentPosition(0, 1)), layout, textMeasurer);

        assertEquals(forward, backward);
    }

    @Test
    void emptyMiddleBlockProducesVisibleSelectionRect() {
        var layout = layoutEngine.layout(document(
                paragraph(text("A")),
                new Paragraph(new InlineContent(List.of())),
                paragraph(text("B"))), 100, textMeasurer);

        var rects = resolver.resolve(
                new DocumentRange(new DocumentPosition(0, 0), new DocumentPosition(2, 1)),
                layout,
                textMeasurer);

        assertTrue(rects.stream().anyMatch(rect -> rect.y() == layout.blocks().get(1).y()
                && rect.width() > 0
                && rect.height() == 10));
    }

    @Test
    void boundaryOnlySelectionProducesVisibleMinimumGeometry() {
        var layout = layoutEngine.layout(document(paragraph(text("abc")), paragraph(text("def"))), 100, textMeasurer);

        var rects = resolver.resolve(
                new DocumentRange(new DocumentPosition(0, 3), new DocumentPosition(1, 0)),
                layout,
                textMeasurer);

        assertEquals(List.of(new SelectionRect(0, layout.blocks().get(1).y(), 8, 10)), rects);
    }

    @Test
    void rejectsGeometryAcrossUnsupportedBlocks() {
        var layout = layoutEngine.layout(
                document(paragraph(text("abc")), new EquationBlock(new MathIdentifier("x")), paragraph(text("def"))),
                100,
                textMeasurer,
                (content, kind) -> new dev.rgcb.scholar.math.layout.MathTextMetrics(10, 7, 3));

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(
                new DocumentRange(new DocumentPosition(0, 1), new DocumentPosition(2, 1)),
                layout,
                textMeasurer));
    }

    private static DocumentRange range(int start, int end) {
        return new DocumentRange(new DocumentPosition(0, start), new DocumentPosition(0, end));
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

    private static Text text(String content, TextMark... marks) {
        return new Text(content, Set.of(marks));
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
