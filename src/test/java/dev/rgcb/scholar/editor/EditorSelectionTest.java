package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.math.editor.MathCaretSelection;
import dev.rgcb.scholar.math.editor.MathPath;
import dev.rgcb.scholar.math.editor.MathSequencePosition;
import dev.rgcb.scholar.math.MathSequence;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EditorSelectionTest {
    @Test
    void textSelectionPreservesDirectionAndRange() {
        var selection = new TextSelection(new DocumentPosition(2, 3), new DocumentPosition(0, 1));

        assertEquals(new DocumentPosition(2, 3), selection.anchor());
        assertEquals(new DocumentPosition(0, 1), selection.active());
        assertEquals(new DocumentRange(new DocumentPosition(0, 1), new DocumentPosition(2, 3)), selection.range());
    }

    @Test
    void editorStateAcceptsExactlyOneSelectionMode() {
        var document = document(paragraph("text"), new EquationBlock(new MathSequence(List.of())));
        var textState = new EditorState(document, new TextSelection(new DocumentPosition(0, 0), new DocumentPosition(0, 4)), Optional.empty());
        var blockState = new EditorState(document, new BlockSelection(1), Optional.empty());

        assertTrue(textState.isTextSelection());
        assertTrue(blockState.isBlockSelection());
        assertThrows(IllegalStateException.class, blockState::caret);
        assertThrows(IllegalStateException.class, textState::blockSelection);
    }

    @Test
    void blockSelectionRejectsInvalidOrEditableBlocks() {
        var document = document(paragraph("text"), heading("Title"), new EquationBlock(new MathSequence(List.of())));

        assertThrows(IllegalArgumentException.class, () -> new BlockSelection(-1));
        assertThrows(IllegalArgumentException.class, () -> new EditorState(document, new BlockSelection(3), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new EditorState(document, new BlockSelection(0), Optional.empty()));
        assertTrue(new EditorState(document, new BlockSelection(1), Optional.empty()).isBlockSelection());
    }

    @Test
    void documentInsertionPointRejectsNegativeAndOperationRejectsPastEnd() {
        var editor = new DocumentEditor();
        var document = document(paragraph("text"));

        assertThrows(IllegalArgumentException.class, () -> new DocumentInsertionPoint(-1));
        assertThrows(IllegalArgumentException.class, () -> editor.insertBlock(
                document,
                new DocumentInsertionPoint(2),
                new EquationBlock(new MathSequence(List.of()))));
    }

    @Test
    void supportsEquationEditingSelectionOnlyForEquationBlocks() {
        var document = document(paragraph("text"), new EquationBlock(new MathSequence(List.of())));
        var selection = new EquationEditingSelection(
                1,
                new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0)));
        var state = new EditorState(document, selection, Optional.empty());

        assertTrue(state.isEquationEditingSelection());
        assertEquals(selection, state.equationEditingSelection());
        assertThrows(IllegalArgumentException.class, () -> new EquationEditingSelection(-1, selection.selection()));
        assertThrows(IllegalArgumentException.class, () -> new EditorState(document, new EquationEditingSelection(2, selection.selection()), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new EditorState(document, new EquationEditingSelection(0, selection.selection()), Optional.empty()));
    }

    private static Document document(dev.rgcb.scholar.document.BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of(new Text(text, Set.of()))));
    }

    private static Heading heading(String text) {
        return new Heading("heading", 1, new InlineContent(List.of(new Text(text, Set.of()))));
    }
}
