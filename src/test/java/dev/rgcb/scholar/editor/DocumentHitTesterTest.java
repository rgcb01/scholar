package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.LaidOutBlock;
import dev.rgcb.scholar.layout.LaidOutBlockKind;
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.layout.LaidOutLine;
import dev.rgcb.scholar.layout.LaidOutText;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathSequence;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DocumentHitTesterTest {
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();
    private final DocumentHitTester hitTester = new DocumentHitTester();

    @Test void overlappingBoundsOfCrossColumnParagraphDoNotStealLaterParagraphHit() {
        var style = TextStyle.paragraph();
        var first = new LaidOutBlock(LaidOutBlockKind.PARAGRAPH, 0, 0, 0, 250, 90, List.of(
                new LaidOutLine(0, 80, 10, 10, List.of(new LaidOutText("a", style, 0, 80, 10, 0, 0, 1, false))),
                new LaidOutLine(200, 0, 10, 10, List.of(new LaidOutText("b", style, 200, 0, 10, 0, 1, 2, false)))));
        var later = new LaidOutBlock(LaidOutBlockKind.PARAGRAPH, 0, 200, 20, 50, 10, List.of(
                new LaidOutLine(200, 20, 40, 10, List.of(new LaidOutText("tail", style, 200, 20, 40, 1, 0, 4, false)))));
        var document = new LaidOutDocument(300, 100, List.of(first, later));

        assertEquals(new DocumentPosition(1, 0), hitTester.hitTest(document, 200, 25, textMeasurer).orElseThrow());
        assertEquals(new DocumentPosition(0, 1), hitTester.hitTest(document, 200, 5, textMeasurer).orElseThrow());
    }

    @Test
    void hitsStartMiddleTieAndEndOfLine() {
        var layout = layoutEngine.layout(document(paragraph(text("abcd"))), 100, textMeasurer);

        assertEquals(position(0), hit(layout, -5, 5));
        assertEquals(position(0), hit(layout, 4, 5));
        assertEquals(position(1), hit(layout, 5, 5));
        assertEquals(position(3), hit(layout, 26, 5));
        assertEquals(position(4), hit(layout, 80, 5));
    }

    @Test
    void hitsWrappedLinesAndBetweenLines() {
        var layout = layoutEngine.layout(document(paragraph(text("alpha beta gamma"))), 60, textMeasurer);

        assertEquals(position(0), hit(layout, 0, 0));
        assertEquals(position(5), hit(layout, 80, 0));
        assertEquals(position(6), hit(layout, 0, 10));
        assertEquals(position(10), hit(layout, 80, 10));
        assertEquals(position(0), hit(layout, 0, 9));
    }

    @Test
    void preservesTrailingSpacesInHitTesting() {
        var layout = layoutEngine.layout(document(paragraph(text("abc "))), 100, textMeasurer);

        assertEquals(position(4), hit(layout, 80, 5));
    }

    @Test
    void hitsUnicodeUserCharacterBoundaries() {
        var layout = layoutEngine.layout(document(paragraph(text("aΔθcafe\u0301"))), 200, textMeasurer);

        assertEquals(position(2), hit(layout, 15, 5));
        assertEquals(position(5), hit(layout, 45, 5));
    }

    @Test
    void aboveAndBelowParagraphClampToNearestBoundary() {
        var layout = layoutEngine.layout(document(paragraph(text("abc"))), 100, textMeasurer);

        assertEquals(position(0), hit(layout, 100, -40));
        assertEquals(position(3), hit(layout, 0, 80));
    }

    @Test
    void emptyParagraphHitsOffsetZero() {
        var layout = layoutEngine.layout(document(paragraph(text(""))), 100, textMeasurer);

        assertEquals(position(0), hit(layout, 50, 5));
    }

    @Test
    void emptyParagraphInlineContentHitsOffsetZeroAcrossColumn() {
        var layout = layoutEngine.layout(document(new Paragraph(new InlineContent(List.of()))), 100, textMeasurer);

        assertEquals(position(0), hit(layout, 0, 5));
        assertEquals(position(0), hit(layout, 50, 5));
        assertEquals(position(0), hit(layout, 100, 5));
    }

    @Test
    void emptyHeadingInlineContentHitsOffsetZero() {
        var layout = layoutEngine.layout(document(new Heading(2, new InlineContent(List.of()))), 100, textMeasurer);

        assertEquals(position(0), hit(layout, 80, layout.blocks().get(0).y() + 5));
    }

    @Test
    void equationBlockDoesNotCreateEditableHit() {
        var layout = layoutEngine.layout(
                document(new EquationBlock(new MathIdentifier("x"))),
                100,
                textMeasurer,
                (content, kind) -> new dev.rgcb.scholar.math.layout.MathTextMetrics(10, 7, 3));

        assertTrue(hitTester.hitTest(layout, 10, 10, textMeasurer).isEmpty());
        assertEquals(DocumentHit.block(0), hitTester.hit(layout, 45, 5, textMeasurer));
    }

    @Test
    void emptyEquationHasUsableBlockHitBounds() {
        var layout = layoutEngine.layout(
                document(new EquationBlock(new MathSequence(List.of()))),
                100,
                textMeasurer,
                (content, kind) -> new dev.rgcb.scholar.math.layout.MathTextMetrics(10, 7, 3));
        var equation = layout.blocks().get(0);

        assertTrue(equation.width() >= 48);
        assertTrue(equation.height() >= 18);
        assertEquals(DocumentHit.block(0), hitTester.hit(layout, equation.x() + 1, equation.y() + 1, textMeasurer));
    }

    @Test
    void explicitBlockHitClampsDragWithinAnchorParagraph() {
        var layout = layoutEngine.layout(document(
                paragraph(text("first")),
                new EquationBlock(new MathIdentifier("x")),
                paragraph(text("second"))), 100, textMeasurer,
                (content, kind) -> new dev.rgcb.scholar.math.layout.MathTextMetrics(10, 7, 3));

        assertEquals(new DocumentPosition(0, 0), hitTester.hitTestInBlock(layout, 0, 100, -500, textMeasurer).orElseThrow());
        assertEquals(new DocumentPosition(0, 5), hitTester.hitTestInBlock(layout, 0, 0, 500, textMeasurer).orElseThrow());
    }

    @Test
    void hitsHeadingStartMiddleEndWrappedAndUnicode() {
        var layout = layoutEngine.layout(document(heading(2, text("alpha Δe\u0301 gamma"))), 50, textMeasurer);

        assertEquals(position(0), hit(layout, -5, 0));
        assertEquals(position(0), hit(layout, 15, 0));
        assertEquals(position(2), hit(layout, 15, 10));
        assertEquals(position(TextBoundary.characterCount("alpha \u0394e\u0301 gamma")), hit(layout, 200, 40));
    }

    private DocumentPosition hit(dev.rgcb.scholar.layout.LaidOutDocument layout, int x, int y) {
        return hitTester.hitTest(layout, x, y, textMeasurer).orElseThrow();
    }

    private static DocumentPosition position(int offset) {
        return new DocumentPosition(0, offset);
    }

    private static Document document(dev.rgcb.scholar.document.BlockNode... blocks) {
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
