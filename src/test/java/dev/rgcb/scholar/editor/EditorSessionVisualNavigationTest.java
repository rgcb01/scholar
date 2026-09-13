package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EditorSessionVisualNavigationTest {
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine();
    private final TextMeasurer textMeasurer = new FixedTextMeasurer();

    @Test
    void moveDownPreservesPreferredXAcrossRepeatedVerticalMoves() {
        var document = document(paragraph("abcdef x abcdef"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 5)));

        session.moveDown(layout(document), textMeasurer);
        assertEquals(new DocumentPosition(0, 8), session.current().caret());
        assertEquals(Optional.of(50), session.preferredCaretX());

        session.moveDown(layout(document), textMeasurer);
        assertEquals(new DocumentPosition(0, 14), session.current().caret());
        assertEquals(Optional.of(50), session.preferredCaretX());
    }

    @Test
    void leftRightHomeEndTypingAndUndoClearPreferredX() {
        var document = document(paragraph("alpha beta gamma"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 2)));
        session.moveDown(layout(document), textMeasurer);
        assertTrue(session.preferredCaretX().isPresent());

        session.moveLeft();
        assertTrue(session.preferredCaretX().isEmpty());
        session.moveDown(layout(session.current().document()), textMeasurer);
        assertTrue(session.preferredCaretX().isPresent());

        session.moveHome(layout(session.current().document()));
        assertTrue(session.preferredCaretX().isEmpty());
        session.moveDown(layout(session.current().document()), textMeasurer);
        session.moveEnd(layout(session.current().document()));
        assertTrue(session.preferredCaretX().isEmpty());
        session.moveDown(layout(session.current().document()), textMeasurer);
        session.typeText("x");
        assertTrue(session.preferredCaretX().isEmpty());
        session.moveDown(layout(session.current().document()), textMeasurer);
        session.undo();
        assertTrue(session.preferredCaretX().isEmpty());
    }

    @Test
    void plainMoveWithSelectionCollapsesToActiveThenNavigates() {
        var document = document(paragraph("alpha beta gamma"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 0), new DocumentPosition(0, 2)));

        session.moveDown(layout(document), textMeasurer);

        assertFalse(session.current().hasSelection());
        assertEquals(new DocumentPosition(0, 8), session.current().caret());
    }

    @Test
    void shiftDownPreservesAnchorAndMovesActiveAcrossEditableBlocks() {
        var document = document(paragraph("abc"), heading(2, "def"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 1)));

        session.extendDown(layout(document, 100), textMeasurer);

        assertEquals(new DocumentPosition(0, 1), session.current().anchor());
        assertEquals(new DocumentPosition(1, 0), session.current().active());
    }

    @Test
    void shiftNavigationDoesNotCrossEquationBlock() {
        var document = document(paragraph("abc"), new EquationBlock(new MathIdentifier("x")), paragraph("def"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 1)));

        session.extendDown(layoutWithMath(document, 100), textMeasurer);

        assertEquals(new DocumentPosition(0, 1), session.current().anchor());
        assertEquals(new DocumentPosition(0, 3), session.current().active());
    }

    @Test
    void equationBlockIsAtomicStopForPlainUpAndDown() {
        var document = document(paragraph("abc"), new EquationBlock(new MathIdentifier("x")), paragraph("def"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 2)));
        var layout = layoutWithMath(document, 100);

        session.moveDown(layout, textMeasurer);
        assertEquals(new BlockSelection(1), session.current().selection());
        session.moveDown(layout, textMeasurer);
        assertEquals(new DocumentPosition(2, 2), session.current().caret());
        session.moveUp(layout, textMeasurer);
        assertEquals(new BlockSelection(1), session.current().selection());
        session.moveUp(layout, textMeasurer);
        assertEquals(new DocumentPosition(0, 2), session.current().caret());
    }

    @Test
    void homeEndUseVisualLineBoundariesAndShiftExtendsSelection() {
        var document = document(paragraph("alpha beta gamma"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 8)));

        session.moveHome(layout(document));
        assertEquals(new DocumentPosition(0, 6), session.current().caret());

        session.moveEnd(layout(document));
        assertEquals(new DocumentPosition(0, 10), session.current().caret());

        session.extendEnd(layout(document));
        assertEquals(new DocumentPosition(0, 10), session.current().anchor());
        assertEquals(new DocumentPosition(0, 10), session.current().active());

        session.extendHome(layout(document));
        assertEquals(new DocumentPosition(0, 10), session.current().anchor());
        assertEquals(new DocumentPosition(0, 6), session.current().active());
    }

    @Test
    void visualNavigationDoesNotCreateUndoHistoryAndBreaksTypingCoalescing() {
        var document = document(paragraph(""));
        var session = new EditorSession(document, 0);

        session.typeText("h");
        session.moveHome(layout(session.current().document()));
        session.typeText("i");
        assertEquals("ih", paragraphText(session.current().document(), 0));

        session.undo();
        assertEquals("h", paragraphText(session.current().document(), 0));
        assertTrue(session.canUndo());
        session.undo();
        assertEquals("", paragraphText(session.current().document(), 0));
    }

    @Test
    void mathModeUpDownHomeEndAreSafeNoOps() {
        var document = document(paragraph("a"), new EquationBlock(new MathIdentifier("x")), paragraph("b"));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), Optional.empty()));
        session.enter();
        var equationSelection = session.current().selection();

        session.moveUp(layoutWithMath(document, 100), textMeasurer);
        session.moveDown(layoutWithMath(document, 100), textMeasurer);
        session.moveHome(layoutWithMath(document, 100));
        session.moveEnd(layoutWithMath(document, 100));

        assertEquals(equationSelection, session.current().selection());
        assertEquals(document, session.current().document());
    }

    private LaidOutDocument layout(Document document) {
        return layout(document, 50);
    }

    private LaidOutDocument layout(Document document, int width) {
        return layoutEngine.layout(document, width, textMeasurer);
    }

    private LaidOutDocument layoutWithMath(Document document, int width) {
        return layoutEngine.layout(document, width, textMeasurer, (content, kind) -> new MathTextMetrics(10, 7, 3));
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }

    private static Heading heading(int level, String text) {
        return new Heading(level, new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }

    private static String paragraphText(Document document, int blockIndex) {
        var paragraph = (Paragraph) document.blocks().get(blockIndex);
        return paragraph.content().nodes().stream()
                .map(Text.class::cast)
                .map(Text::content)
                .reduce("", String::concat);
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
