package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EditorHistoryTest {
    private final DocumentEditor editor = new DocumentEditor();
    private final PlainTextClipboard clipboard = new PlainTextClipboard(editor);

    @Test
    void insertUndoAndRedoRestoreDocumentAndCaret() {
        var history = history("abc", 3);

        history.applyEdit(editor.insertText(history.current(), "X"));
        assertEquals("abcX", paragraphText(history.current().document()));
        assertEquals(new DocumentPosition(0, 4), history.current().caret());

        assertTrue(history.undo());
        assertEquals("abc", paragraphText(history.current().document()));
        assertEquals(new DocumentPosition(0, 3), history.current().caret());

        assertTrue(history.redo());
        assertEquals("abcX", paragraphText(history.current().document()));
        assertEquals(new DocumentPosition(0, 4), history.current().caret());
    }

    @Test
    void undoRestoresBackspaceDeleteSelectionCutAndPaste() {
        var backspace = history("abc", 2);
        backspace.applyEdit(editor.deleteBackward(backspace.current()));
        assertEquals("ac", paragraphText(backspace.current().document()));
        backspace.undo();
        assertEquals("abc", paragraphText(backspace.current().document()));

        var delete = history("abc", 1);
        delete.applyEdit(editor.deleteForward(delete.current()));
        assertEquals("ac", paragraphText(delete.current().document()));
        delete.undo();
        assertEquals("abc", paragraphText(delete.current().document()));

        var replacement = history("abcdef", 2, 5);
        replacement.applyEdit(editor.insertText(replacement.current(), "X"));
        assertEquals("abXf", paragraphText(replacement.current().document()));
        replacement.undo();
        assertEquals("abcdef", paragraphText(replacement.current().document()));

        var cut = history("abcdef", 2, 5);
        cut.applyEdit(clipboard.cut(cut.current()).orElseThrow().editResult());
        assertEquals("abf", paragraphText(cut.current().document()));
        cut.undo();
        assertEquals("abcdef", paragraphText(cut.current().document()));

        var paste = history("abc", 1);
        paste.applyEdit(clipboard.paste(paste.current(), "X").orElseThrow());
        assertEquals("aXbc", paragraphText(paste.current().document()));
        paste.undo();
        assertEquals("abc", paragraphText(paste.current().document()));
    }

    @Test
    void supportsMultipleUndoRedoSteps() {
        var history = history("a", 1);

        history.applyEdit(editor.insertText(history.current(), "b"));
        history.applyEdit(editor.insertText(history.current(), "c"));

        assertEquals("abc", paragraphText(history.current().document()));
        history.undo();
        assertEquals("ab", paragraphText(history.current().document()));
        history.undo();
        assertEquals("a", paragraphText(history.current().document()));
        history.redo();
        assertEquals("ab", paragraphText(history.current().document()));
        history.redo();
        assertEquals("abc", paragraphText(history.current().document()));
    }

    @Test
    void undoRedoRestoreForwardAndBackwardSelections() {
        var history = history("abcdef", 2, 5);

        history.applyEdit(editor.insertText(history.current(), "X"));
        history.undo();
        assertEquals(new DocumentPosition(0, 2), history.current().anchor());
        assertEquals(new DocumentPosition(0, 5), history.current().active());

        history.setCurrent(new EditorState(history.current().document(), new DocumentPosition(0, 5), new DocumentPosition(0, 2)));
        history.applyEdit(editor.insertText(history.current(), "Y"));
        history.undo();
        assertEquals(new DocumentPosition(0, 5), history.current().anchor());
        assertEquals(new DocumentPosition(0, 2), history.current().active());
    }

    @Test
    void editAfterUndoClearsRedo() {
        var history = history("a", 1);

        history.applyEdit(editor.insertText(history.current(), "b"));
        history.undo();
        history.applyEdit(editor.insertText(history.current(), "c"));

        assertFalse(history.canRedo());
        assertFalse(history.redo());
        assertEquals("ac", paragraphText(history.current().document()));
    }

    @Test
    void navigationAndSelectionMovementDoNotCreateHistoryEntries() {
        var history = history("abc", 2);

        history.setCurrent(editor.moveLeft(history.current()).editorState());
        history.setCurrent(editor.extendRight(history.current()));

        assertFalse(history.canUndo());
    }

    @Test
    void typingCoalescesUntilMovementOrOtherEditBreaksGroup() {
        var history = history("", 0);

        history.applyTyping(editor.insertText(history.current(), "h"), "h");
        history.applyTyping(editor.insertText(history.current(), "i"), "i");
        assertEquals("hi", paragraphText(history.current().document()));
        history.undo();
        assertEquals("", paragraphText(history.current().document()));

        history.redo();
        history.setCurrent(editor.moveLeft(history.current()).editorState());
        history.applyTyping(editor.insertText(history.current(), "!"), "!");
        history.undo();
        assertEquals("hi", paragraphText(history.current().document()));
    }

    @Test
    void historyCapacityDiscardsOldestSnapshots() {
        var history = new EditorHistory(state("a", 1), 2);

        history.applyEdit(editor.insertText(history.current(), "b"));
        history.applyEdit(editor.insertText(history.current(), "c"));
        history.applyEdit(editor.insertText(history.current(), "d"));

        assertEquals("abcd", paragraphText(history.current().document()));
        history.undo();
        assertEquals("abc", paragraphText(history.current().document()));
        history.undo();
        assertEquals("ab", paragraphText(history.current().document()));
        assertFalse(history.undo());
    }

    @Test
    void oldDocumentSnapshotsRemainUnchanged() {
        var document = document(paragraph(text("abc")));
        var history = new EditorHistory(new EditorState(document, new DocumentPosition(0, 1)));

        history.applyEdit(editor.insertText(history.current(), "X"));

        assertEquals("abc", paragraphText(document));
        assertEquals("aXbc", paragraphText(history.current().document()));
    }

    private static EditorHistory history(String text, int caret) {
        return new EditorHistory(state(text, caret));
    }

    private static EditorHistory history(String text, int anchor, int active) {
        return new EditorHistory(new EditorState(
                document(paragraph(text(text))),
                new DocumentPosition(0, anchor),
                new DocumentPosition(0, active)));
    }

    private static EditorState state(String text, int caret) {
        return new EditorState(document(paragraph(text(text))), new DocumentPosition(0, caret));
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

    private static String paragraphText(Document document) {
        return ((Paragraph) document.blocks().get(0)).content().nodes().stream()
                .map(Text.class::cast)
                .map(Text::content)
                .reduce("", String::concat);
    }
}
