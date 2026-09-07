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
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VisualLineNavigatorTest {
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();
    private final VisualLineNavigator navigator = new VisualLineNavigator();

    @Test
    void movesUpAndDownWithinWrappedParagraphUsingClosestX() {
        var layout = layout(document(paragraph(text("alpha beta gamma"))), 50);
        var state = new EditorState(document(paragraph(text("alpha beta gamma"))), new DocumentPosition(0, 2));

        var down = navigator.moveDown(state, layout, textMeasurer, 20, true).orElseThrow();
        var downAgain = navigator.moveDown(new EditorState(state.document(), down, java.util.Optional.empty()), layout, textMeasurer, 20, true).orElseThrow();
        var up = navigator.moveUp(new EditorState(state.document(), downAgain, java.util.Optional.empty()), layout, textMeasurer, 20, true).orElseThrow();

        assertEquals(caret(0, 8), down);
        assertEquals(caret(0, 13), downAgain);
        assertEquals(caret(0, 8), up);
    }

    @Test
    void preservesPreferredXAcrossShortMiddleLine() {
        var document = document(paragraph(text("abcdef x abcdef")));
        var layout = layout(document, 60);
        var state = new EditorState(document, new DocumentPosition(0, 5));

        var shortLine = navigator.moveDown(state, layout, textMeasurer, 50, true).orElseThrow();
        var longLine = navigator.moveDown(new EditorState(document, shortLine, java.util.Optional.empty()), layout, textMeasurer, 50, true).orElseThrow();

        assertEquals(caret(0, 8), shortLine);
        assertEquals(caret(0, 14), longLine);
    }

    @Test
    void movesAcrossEditableParagraphAndHeadingBlocks() {
        var document = document(
                paragraph(text("abc")),
                heading(2, text("def")),
                paragraph(text("ghi")));
        var layout = layout(document, 100);

        var toHeading = navigator.moveDown(new EditorState(document, new DocumentPosition(0, 2)), layout, textMeasurer, 20, true).orElseThrow();
        var toParagraph = navigator.moveDown(new EditorState(document, toHeading, java.util.Optional.empty()), layout, textMeasurer, 20, true).orElseThrow();
        var backToHeading = navigator.moveUp(new EditorState(document, toParagraph, java.util.Optional.empty()), layout, textMeasurer, 20, true).orElseThrow();

        assertEquals(caret(1, 2), toHeading);
        assertEquals(caret(2, 2), toParagraph);
        assertEquals(caret(1, 2), backToHeading);
    }

    @Test
    void movesThroughEmptyEditableBlockWithOffsetZero() {
        var document = document(
                paragraph(text("abc")),
                new Paragraph(new InlineContent(List.of())),
                paragraph(text("def")));
        var layout = layout(document, 100);

        var empty = navigator.moveDown(new EditorState(document, new DocumentPosition(0, 2)), layout, textMeasurer, 20, true).orElseThrow();
        var below = navigator.moveDown(new EditorState(document, empty, java.util.Optional.empty()), layout, textMeasurer, 20, true).orElseThrow();
        var up = navigator.moveUp(new EditorState(document, below, java.util.Optional.empty()), layout, textMeasurer, 20, true).orElseThrow();

        assertEquals(caret(1, 0), empty);
        assertEquals(caret(2, 2), below);
        assertEquals(caret(1, 0), up);
    }

    @Test
    void plainVerticalNavigationUsesEquationBlockAsAtomicSelection() {
        var document = document(
                paragraph(text("abc")),
                new EquationBlock(new MathIdentifier("x")),
                paragraph(text("def")));
        var layout = layoutWithMath(document, 100);

        var equation = navigator.moveDown(new EditorState(document, new DocumentPosition(0, 2)), layout, textMeasurer, 20, true).orElseThrow();
        var below = navigator.moveDown(new EditorState(document, equation, java.util.Optional.empty()), layout, textMeasurer, 20, true).orElseThrow();
        var equationAgain = navigator.moveUp(new EditorState(document, below, java.util.Optional.empty()), layout, textMeasurer, 20, true).orElseThrow();
        var above = navigator.moveUp(new EditorState(document, equationAgain, java.util.Optional.empty()), layout, textMeasurer, 20, true).orElseThrow();

        assertEquals(new BlockSelection(1), equation);
        assertEquals(caret(2, 2), below);
        assertEquals(new BlockSelection(1), equationAgain);
        assertEquals(caret(0, 2), above);
    }

    @Test
    void shiftVerticalNavigationClampsBeforeEquationBlock() {
        var document = document(
                paragraph(text("abc")),
                new EquationBlock(new MathSequence(List.of())),
                paragraph(text("def")));
        var layout = layoutWithMath(document, 100);
        var state = new EditorState(document, new DocumentPosition(0, 1));

        var target = navigator.moveDown(state, layout, textMeasurer, 10, false).orElseThrow();

        assertEquals(caret(0, 3), target);
    }

    @Test
    void homeAndEndResolveCurrentVisualLine() {
        var document = document(paragraph(text("alpha beta gamma ")));
        var layout = layout(document, 50);

        assertEquals(new DocumentPosition(0, 6), navigator.lineStart(new DocumentPosition(0, 8), layout).orElseThrow());
        assertEquals(new DocumentPosition(0, 10), navigator.lineEnd(new DocumentPosition(0, 8), layout).orElseThrow());
        assertEquals(new DocumentPosition(0, 16), navigator.lineEnd(new DocumentPosition(0, 15), layout).orElseThrow());
        assertEquals(new DocumentPosition(0, 17), navigator.lineEnd(new DocumentPosition(0, 17), layout).orElseThrow());
    }

    @Test
    void closestXRespectsFormattedRunMetricsAndTieChoosesRightBoundary() {
        var document = document(paragraph(
                text("ab", TextMark.BOLD),
                text("cd", TextMark.ITALIC)));
        var layout = layout(document, 25);
        var state = new EditorState(document, new DocumentPosition(0, 1));

        var target = navigator.moveDown(state, layout, textMeasurer, 4, true).orElseThrow();

        assertEquals(caret(0, 3), target);
    }

    @Test
    void unicodeOffsetsRemainTextBoundaryOffsets() {
        var document = document(paragraph(text("cafe\u0301 Δθ")));
        var layout = layout(document, 50);
        var state = new EditorState(document, new DocumentPosition(0, 2));

        var target = navigator.moveDown(state, layout, textMeasurer, 20, true).orElseThrow();

        assertEquals(caret(0, 8), target);
    }

    private LaidOutDocument layout(Document document, int width) {
        return layoutEngine.layout(document, width, textMeasurer);
    }

    private LaidOutDocument layoutWithMath(Document document, int width) {
        return layoutEngine.layout(document, width, textMeasurer, (content, kind) -> new MathTextMetrics(10, 7, 3));
    }

    private static TextSelection caret(int blockIndex, int offset) {
        var position = new DocumentPosition(blockIndex, offset);
        return new TextSelection(position, position);
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
            return TextBoundary.characterCount(text) * widthFor(style);
        }

        @Override
        public int lineHeight(TextStyle style) {
            return 10;
        }

        private int widthFor(TextStyle style) {
            if (style.marks().contains(TextMark.BOLD)) {
                return 12;
            }
            if (style.marks().contains(TextMark.ITALIC)) {
                return 8;
            }
            return 10;
        }
    }
}
