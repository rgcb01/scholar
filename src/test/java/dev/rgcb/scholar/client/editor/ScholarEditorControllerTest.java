package dev.rgcb.scholar.client.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.editor.BlockStyle;
import dev.rgcb.scholar.editor.BlockStyleSelectionState;
import dev.rgcb.scholar.editor.BlockSelection;
import dev.rgcb.scholar.editor.BuiltInEditorActions;
import dev.rgcb.scholar.editor.ClipboardAdapter;
import dev.rgcb.scholar.editor.DocumentPosition;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.editor.EditorState;
import dev.rgcb.scholar.editor.EquationEditingSelection;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.editor.MathRangeSelection;
import dev.rgcb.scholar.math.editor.MathPath;
import dev.rgcb.scholar.math.editor.MathSequencePosition;
import dev.rgcb.scholar.math.editor.SemanticMathTokenKind;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScholarEditorControllerTest {
    @Test
    void formattingAndBlockStyleActionsTriggerFullRelayout() {
        var session = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 0);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 1), new DocumentPosition(1, 2)));
        var relayout = new Counter();
        var visible = new Counter();
        var controller = new ScholarEditorController(session, new FakeClipboard(), relayout::increment, visible::increment);

        controller.execute(BuiltInEditorActions.bold());
        controller.execute(BuiltInEditorActions.heading(2));

        assertEquals(2, relayout.count);
        assertEquals(2, visible.count);
        assertEquals(BlockStyleSelectionState.single(BlockStyle.heading(2)), controller.blockStyleSelectionState());
    }

    @Test
    void insertEquationActionChangesDocumentAndSelectsInsertedObject() {
        var session = new EditorSession(document(paragraph(text("abc"))), 0);
        var relayout = new Counter();
        var visible = new Counter();
        var controller = new ScholarEditorController(session, new FakeClipboard(), relayout::increment, visible::increment);

        controller.execute(BuiltInEditorActions.insertEquation());

        assertEquals(3, session.current().document().blocks().size());
        assertEquals(new BlockSelection(1), session.current().selection());
        assertEquals(1, relayout.count);
        assertEquals(1, visible.count);
    }

    @Test
    void semanticConversionActionRequestsPopupWithoutRelayout() {
        var session = new EditorSession(document(
                paragraph(text("A")),
                new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of(
                        new MathIdentifier("s"),
                        new MathIdentifier("i"),
                        new MathIdentifier("n"))))), 0);
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 3))), java.util.Optional.empty()));
        var relayout = new Counter();
        var visible = new Counter();
        var popup = new PopupCounter();
        var controller = new ScholarEditorController(session, new FakeClipboard(), relayout::increment, visible::increment, popup::open);

        controller.execute(BuiltInEditorActions.convertToNamedOperator());

        assertEquals(0, relayout.count);
        assertEquals(0, visible.count);
        assertEquals(SemanticMathTokenKind.NAMED_OPERATOR, popup.kind);
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(Text... text) {
        return new Paragraph(new InlineContent(List.of(text).stream().map(InlineNode.class::cast).toList()));
    }

    private static Text text(String content) {
        return new Text(content, Set.of());
    }

    private static final class Counter {
        private int count;

        private void increment() {
            count++;
        }
    }

    private static final class PopupCounter {
        private SemanticMathTokenKind kind;

        private void open(SemanticMathTokenKind kind) {
            this.kind = kind;
        }
    }

    private static final class FakeClipboard implements ClipboardAdapter {
        @Override
        public String getText() {
            return "";
        }

        @Override
        public boolean setText(String text) {
            return true;
        }
    }
}
