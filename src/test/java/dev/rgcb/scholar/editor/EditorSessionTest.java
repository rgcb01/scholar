package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathDelimiter;
import dev.rgcb.scholar.math.MathGroup;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNamedOperator;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathOperatorRole;
import dev.rgcb.scholar.math.MathRoot;
import dev.rgcb.scholar.math.MathScript;
import dev.rgcb.scholar.math.MathText;
import dev.rgcb.scholar.math.editor.MathCaretSelection;
import dev.rgcb.scholar.math.editor.MathPath;
import dev.rgcb.scholar.math.editor.MathRangeSelection;
import dev.rgcb.scholar.math.editor.MathSequencePosition;
import dev.rgcb.scholar.math.editor.MathTokenPosition;
import dev.rgcb.scholar.math.editor.GroupContent;
import dev.rgcb.scholar.math.editor.RootRadicand;
import dev.rgcb.scholar.math.editor.ScriptSuperscript;
import dev.rgcb.scholar.math.editor.SemanticMathTokenKind;
import dev.rgcb.scholar.math.editor.SequenceChild;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EditorSessionTest {
    @Test
    void createsInitialSessionState() {
        var session = session("abc", 0);

        assertEquals(new DocumentPosition(0, 3), session.current().caret());
        assertFalse(session.canUndo());
        assertFalse(session.canRedo());
    }

    @Test
    void delegatesUndoAndRedo() {
        var session = session("abc", 0);

        assertTrue(session.typeText("X"));
        assertEquals("abcX", paragraphText(session.current().document()));
        assertTrue(session.undo());
        assertEquals("abc", paragraphText(session.current().document()));
        assertTrue(session.redo());
        assertEquals("abcX", paragraphText(session.current().document()));
    }

    @Test
    void copiesLogicalText() {
        var session = session("abcdef", 0);

        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 2), new DocumentPosition(0, 5)));

        assertEquals("cde", session.copySelection().orElseThrow());
    }

    @Test
    void cutsSelectionAndCollapsesState() {
        var session = session("abcdef", 0);

        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 2), new DocumentPosition(0, 5)));
        var cut = session.cutSelection().orElseThrow();
        assertEquals("cde", cut.clipboardText());
        assertTrue(session.applyCut(cut));

        assertEquals("abf", paragraphText(session.current().document()));
        assertEquals(new DocumentPosition(0, 2), session.current().caret());
        assertFalse(session.current().hasSelection());
    }

    @Test
    void pastesAndPreservesHistory() {
        var session = session("abc", 0);

        assertTrue(session.pasteText("X"));
        assertEquals("abcX", paragraphText(session.current().document()));
        assertTrue(session.canUndo());
        session.undo();
        assertEquals("abc", paragraphText(session.current().document()));
    }

    @Test
    void updatesCurrentStateWithoutHistoryForNavigation() {
        var session = session("abc", 0);

        session.moveLeft();
        session.extendRight();

        assertFalse(session.canUndo());
        assertEquals(new DocumentRange(new DocumentPosition(0, 2), new DocumentPosition(0, 3)), session.current().selectionRange());
    }

    @Test
    void typingCoalescingStillWorksThroughSession() {
        var session = session("", 0);

        session.typeText("a");
        session.typeText("b");
        session.typeText("c");
        session.undo();

        assertEquals("", paragraphText(session.current().document()));
    }

    @Test
    void collapsedCaretToggleAppliesExplicitMarksToTypedText() {
        var session = session("", 0);

        assertTrue(session.toggleMark(TextMark.BOLD));
        assertEquals(Set.of(TextMark.BOLD), session.current().explicitTypingMarks().orElseThrow());
        session.typeText("bold");

        assertEquals(Set.of(TextMark.BOLD), textAt(session.current().document(), 0).marks());
        assertEquals(Set.of(TextMark.BOLD), session.current().explicitTypingMarks().orElseThrow());
    }

    @Test
    void collapsedCaretToggleCanTurnMarkOffAndCombineMarks() {
        var session = session("", 0);

        session.toggleMark(TextMark.BOLD);
        session.typeText("bold");
        session.toggleMark(TextMark.BOLD);
        session.typeText(" plain");
        session.toggleMark(TextMark.BOLD);
        session.toggleMark(TextMark.ITALIC);
        session.typeText(" both");

        assertEquals(Set.of(TextMark.BOLD), textAt(session.current().document(), 0).marks());
        assertEquals(Set.of(), textAt(session.current().document(), 1).marks());
        assertEquals(Set.of(TextMark.BOLD, TextMark.ITALIC), textAt(session.current().document(), 2).marks());
    }

    @Test
    void firstCollapsedToggleStartsFromInferredMarks() {
        var session = new EditorSession(document(paragraph(text("bold", TextMark.BOLD))), 0);

        session.toggleMark(TextMark.ITALIC);
        assertEquals(Set.of(TextMark.BOLD, TextMark.ITALIC), session.current().explicitTypingMarks().orElseThrow());

        session.toggleMark(TextMark.BOLD);
        assertEquals(Set.of(TextMark.ITALIC), session.current().explicitTypingMarks().orElseThrow());
    }

    @Test
    void movementAndSelectionUpdatesClearExplicitTypingMarks() {
        var session = session("abc", 0);

        session.toggleMark(TextMark.BOLD);
        session.moveLeft();
        assertTrue(session.current().explicitTypingMarks().isEmpty());

        session.toggleMark(TextMark.BOLD);
        session.extendLeft();
        assertTrue(session.current().explicitTypingMarks().isEmpty());

        session.toggleMark(TextMark.BOLD);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 1)));
        assertTrue(session.current().explicitTypingMarks().isEmpty());
    }

    @Test
    void selectionFormattingPreservesSelectionAndSupportsUndoRedo() {
        var session = session("velocity", 0);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 0), new DocumentPosition(0, 8)));

        assertTrue(session.toggleMark(TextMark.BOLD));

        assertEquals(Set.of(TextMark.BOLD), textAt(session.current().document(), 0).marks());
        assertEquals(new DocumentPosition(0, 0), session.current().anchor());
        assertEquals(new DocumentPosition(0, 8), session.current().active());
        assertTrue(session.undo());
        assertEquals(Set.of(), textAt(session.current().document(), 0).marks());
        assertEquals(new DocumentPosition(0, 0), session.current().anchor());
        assertEquals(new DocumentPosition(0, 8), session.current().active());
        assertTrue(session.redo());
        assertEquals(Set.of(TextMark.BOLD), textAt(session.current().document(), 0).marks());
    }

    @Test
    void collapsedCaretToggleDoesNotCreateUndoEntryButBreaksTypingCoalescing() {
        var session = session("", 0);

        session.typeText("hello");
        session.toggleMark(TextMark.BOLD);
        session.typeText("world");
        session.undo();

        assertEquals("hello", paragraphText(session.current().document()));
        assertEquals(Set.of(), textAt(session.current().document(), 0).marks());
    }

    @Test
    void pasteUsesExplicitMarksAndThenInvalidatesTypingMarks() {
        var session = session("", 0);

        session.toggleMark(TextMark.BOLD);
        assertTrue(session.pasteText("clip"));

        assertEquals(Set.of(TextMark.BOLD), textAt(session.current().document(), 0).marks());
        assertTrue(session.current().explicitTypingMarks().isEmpty());
    }

    @Test
    void setBlockStylePreservesSelectionAndSupportsUndoRedo() {
        var session = session("Results", 0);
        session.setCurrent(new EditorState(
                session.current().document(),
                new DocumentPosition(0, 7),
                new DocumentPosition(0, 0)));

        assertTrue(session.setBlockStyle(BlockStyle.heading(2)));

        assertEquals(BlockStyle.heading(2), session.blockStyle().orElseThrow());
        assertEquals(new DocumentPosition(0, 7), session.current().anchor());
        assertEquals(new DocumentPosition(0, 0), session.current().active());
        assertTrue(session.undo());
        assertEquals(BlockStyle.paragraph(), session.blockStyle().orElseThrow());
        assertTrue(session.redo());
        assertEquals(BlockStyle.heading(2), session.blockStyle().orElseThrow());
    }

    @Test
    void setBlockStylePreservesExplicitTypingMarksAtCaret() {
        var session = session("Results", 0);

        session.toggleMark(TextMark.BOLD);
        assertTrue(session.setBlockStyle(BlockStyle.heading(2)));

        assertEquals(Set.of(TextMark.BOLD), session.current().explicitTypingMarks().orElseThrow());
    }

    @Test
    void blockStyleNoOpDoesNotCreateHistoryEntry() {
        var session = session("Results", 0);

        assertFalse(session.setBlockStyle(BlockStyle.paragraph()));
        assertFalse(session.canUndo());
    }

    @Test
    void blockStyleCommandBreaksTypingCoalescing() {
        var session = session("", 0);

        session.typeText("Result");
        session.setBlockStyle(BlockStyle.heading(2));
        session.typeText("s");
        session.undo();

        assertEquals("Result", paragraphText(session.current().document()));
        assertEquals(BlockStyle.heading(2), session.blockStyle().orElseThrow());
    }

    @Test
    void enterCreatesOneUndoableStructuralTransaction() {
        var session = session("hello world", 0);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 5)));

        assertTrue(session.enter());

        assertEquals(List.of("hello", " world"), blockTexts(session.current().document()));
        assertEquals(new DocumentPosition(1, 0), session.current().caret());
        assertTrue(session.undo());
        assertEquals(List.of("hello world"), blockTexts(session.current().document()));
        assertTrue(session.redo());
        assertEquals(List.of("hello", " world"), blockTexts(session.current().document()));
    }

    @Test
    void selectionEnterUndoRestoresOriginalSelection() {
        var session = session("abcdef", 0);
        var selected = new EditorState(session.current().document(), new DocumentPosition(0, 1), new DocumentPosition(0, 5));
        session.setCurrent(selected);

        assertTrue(session.enter());
        assertTrue(session.undo());

        assertEquals(selected.withoutExplicitTypingMarks(), session.current());
    }

    @Test
    void enterCarriesExplicitAndInferredTypingMarks() {
        var explicit = session("", 0);
        explicit.toggleMark(TextMark.BOLD);
        explicit.toggleMark(TextMark.ITALIC);
        explicit.typeText("hello");

        assertTrue(explicit.enter());

        assertEquals(Set.of(TextMark.BOLD, TextMark.ITALIC), explicit.current().explicitTypingMarks().orElseThrow());
        explicit.typeText("world");
        assertEquals(Set.of(TextMark.BOLD, TextMark.ITALIC), textAt(explicit.current().document(), 1, 0).marks());

        var inferred = new EditorSession(document(paragraph(text("bold", TextMark.BOLD))), 0);
        assertTrue(inferred.enter());

        assertEquals(Set.of(TextMark.BOLD), inferred.current().explicitTypingMarks().orElseThrow());
    }

    @Test
    void enterStoresExplicitEmptyTypingContext() {
        var session = session("plain", 0);

        assertTrue(session.enter());

        assertTrue(session.current().explicitTypingMarks().isPresent());
        assertEquals(Set.of(), session.current().explicitTypingMarks().orElseThrow());
    }

    @Test
    void headingEnterUpdatesCurrentBlockStyle() {
        var session = new EditorSession(document(heading(2, text("Results"))), 0);

        assertTrue(session.enter());

        assertEquals(BlockStyle.paragraph(), session.blockStyle().orElseThrow());
        assertEquals(Paragraph.class, session.current().document().blocks().get(1).getClass());
    }

    @Test
    void boundaryJoinIsUndoableAndClearsExplicitTypingMarks() {
        var session = new EditorSession(document(paragraph(text("hello")), paragraph(text("world"))), 1);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(1, 0)));
        session.toggleMark(TextMark.BOLD);

        assertTrue(session.deleteBackward());

        assertEquals(List.of("helloworld"), blockTexts(session.current().document()));
        assertTrue(session.current().explicitTypingMarks().isEmpty());
        assertTrue(session.undo());
        assertEquals(List.of("hello", "world"), blockTexts(session.current().document()));
        assertTrue(session.redo());
        assertEquals(List.of("helloworld"), blockTexts(session.current().document()));
    }

    @Test
    void enterAndBoundaryJoinBreakTypingCoalescing() {
        var enterSession = session("", 0);
        enterSession.typeText("abc");
        enterSession.enter();
        enterSession.typeText("def");
        enterSession.undo();
        assertEquals(List.of("abc", ""), blockTexts(enterSession.current().document()));

        var joinSession = new EditorSession(document(paragraph(text("a")), paragraph(text("b"))), 1);
        joinSession.setCurrent(new EditorState(joinSession.current().document(), new DocumentPosition(1, 0)));
        joinSession.deleteBackward();
        joinSession.typeText("c");
        joinSession.undo();
        assertEquals(List.of("ab"), blockTexts(joinSession.current().document()));
    }

    @Test
    void crossBlockNavigationClearsExplicitTypingMarksWithoutHistory() {
        var session = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 1);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(1, 0)));
        session.toggleMark(TextMark.BOLD);

        session.moveLeft();

        assertEquals(new DocumentPosition(0, 3), session.current().caret());
        assertTrue(session.current().explicitTypingMarks().isEmpty());
        assertFalse(session.canUndo());
    }

    @Test
    void editorStatePreservesMultiBlockSelectionDirection() {
        var document = document(paragraph(text("abc")), paragraph(text("def")));
        var state = new EditorState(document, new DocumentPosition(1, 2), new DocumentPosition(0, 1));

        assertEquals(new DocumentPosition(1, 2), state.anchor());
        assertEquals(new DocumentPosition(0, 1), state.active());
        assertEquals(new DocumentRange(new DocumentPosition(0, 1), new DocumentPosition(1, 2)), state.selectionRange());
    }

    @Test
    void multiBlockReplacementOperationsMutateFor10C() {
        var typed = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 0);
        typed.setCurrent(new EditorState(typed.current().document(), new DocumentPosition(0, 1), new DocumentPosition(1, 2)));
        assertTrue(typed.typeText("X"));
        assertEquals(List.of("aXf"), blockTexts(typed.current().document()));

        var enter = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 0);
        enter.setCurrent(new EditorState(enter.current().document(), new DocumentPosition(0, 1), new DocumentPosition(1, 2)));
        assertTrue(enter.enter());
        assertEquals(List.of("a", "f"), blockTexts(enter.current().document()));

        var delete = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 0);
        delete.setCurrent(new EditorState(delete.current().document(), new DocumentPosition(0, 1), new DocumentPosition(1, 2)));
        assertTrue(delete.deleteBackward());
        assertEquals(List.of("af"), blockTexts(delete.current().document()));

        var paste = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 0);
        paste.setCurrent(new EditorState(paste.current().document(), new DocumentPosition(0, 1), new DocumentPosition(1, 2)));
        assertTrue(paste.pasteText("X"));
        assertEquals(List.of("aXf"), blockTexts(paste.current().document()));
    }

    @Test
    void multiBlockDeleteUndoRedoRestoresOriginalSelectionThenCollapsedCaret() {
        var session = new EditorSession(document(paragraph(text("Hello ABC")), paragraph(text("middle")), paragraph(text("XYZ world"))), 0);
        var selection = new EditorState(session.current().document(), new DocumentPosition(0, 6), new DocumentPosition(2, 3));
        session.setCurrent(selection);

        assertTrue(session.deleteForward());

        assertEquals(List.of("Hello  world"), blockTexts(session.current().document()));
        assertEquals(new DocumentPosition(0, 6), session.current().caret());
        assertTrue(session.undo());
        assertEquals(selection.withoutExplicitTypingMarks(), session.current());
        assertTrue(session.redo());
        assertEquals(List.of("Hello  world"), blockTexts(session.current().document()));
        assertEquals(new DocumentPosition(0, 6), session.current().caret());
    }

    @Test
    void boundaryOnlyCutCopiesNewlineJoinsAndUndoRestoresSelection() {
        var session = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 0);
        var selection = new EditorState(session.current().document(), new DocumentPosition(0, 3), new DocumentPosition(1, 0));
        session.setCurrent(selection);

        var cut = session.cutSelection().orElseThrow();

        assertEquals("\n", cut.clipboardText());
        assertTrue(session.applyCut(cut));
        assertEquals(List.of("abcdef"), blockTexts(session.current().document()));
        assertTrue(session.undo());
        assertEquals(selection.withoutExplicitTypingMarks(), session.current());
    }

    @Test
    void typingOverMultiBlockSelectionCoalescesAndUndoRestoresSelection() {
        var session = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 0);
        var selection = new EditorState(session.current().document(), new DocumentPosition(0, 1), new DocumentPosition(1, 2));
        session.setCurrent(selection);

        assertTrue(session.typeText("h"));
        assertTrue(session.typeText("e"));
        assertTrue(session.typeText("y"));

        assertEquals(List.of("aheyf"), blockTexts(session.current().document()));
        assertTrue(session.undo());
        assertEquals(selection.withoutExplicitTypingMarks(), session.current());
    }

    @Test
    void typingReplacementUsesNormalizedSelectionStartAffinityForForwardAndBackwardSelections() {
        var document = document(
                paragraph(text("AB", TextMark.BOLD), text("CD")),
                paragraph(text("EF")));
        var forward = new EditorSession(document, 0);
        var backward = new EditorSession(document, 0);

        forward.setCurrent(new EditorState(document, new DocumentPosition(0, 1), new DocumentPosition(1, 1)));
        backward.setCurrent(new EditorState(document, new DocumentPosition(1, 1), new DocumentPosition(0, 1)));
        assertTrue(forward.typeText("x"));
        assertTrue(backward.typeText("x"));

        assertEquals(forward.current().document(), backward.current().document());
        assertEquals(Set.of(TextMark.BOLD), textAt(forward.current().document(), 0, 1).marks());
    }

    @Test
    void pasteOverMultiBlockSelectionFlattensMultilineClipboardAndUndoRestoresSelection() {
        var session = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 0);
        var selection = new EditorState(session.current().document(), new DocumentPosition(0, 1), new DocumentPosition(1, 2));
        session.setCurrent(selection);

        assertTrue(session.pasteText("x\ny"));

        assertEquals(List.of("ax yf"), blockTexts(session.current().document()));
        assertTrue(session.undo());
        assertEquals(selection.withoutExplicitTypingMarks(), session.current());
    }

    @Test
    void enterOverMultiBlockParagraphSelectionIsOneHistoryTransaction() {
        var session = new EditorSession(document(paragraph(text("Hello ABC")), paragraph(text("middle")), paragraph(text("XYZ world"))), 0);
        var selection = new EditorState(session.current().document(), new DocumentPosition(0, 6), new DocumentPosition(2, 3));
        session.setCurrent(selection);

        assertTrue(session.enter());

        assertEquals(List.of("Hello ", " world"), blockTexts(session.current().document()));
        assertEquals(new DocumentPosition(1, 0), session.current().caret());
        assertTrue(session.undo());
        assertEquals(selection.withoutExplicitTypingMarks(), session.current());
    }

    @Test
    void enterOverMultiBlockHeadingSelectionUsesLeftOwnedHeadingSemanticsAndStartMarks() {
        var document = document(
                heading(2, text("Title ", TextMark.ITALIC), text("ABC")),
                paragraph(text("middle")),
                paragraph(text("XYZ tail")));
        var session = new EditorSession(document, 0);
        var selection = new EditorState(document, new DocumentPosition(0, 6), new DocumentPosition(2, 3));
        session.setCurrent(selection);

        assertTrue(session.enter());

        assertEquals(Heading.class, session.current().document().blocks().get(0).getClass());
        assertEquals(Heading.class, session.current().document().blocks().get(1).getClass());
        assertEquals(List.of("Title ", " tail"), blockTexts(session.current().document()));
        assertEquals(Set.of(TextMark.ITALIC), session.current().explicitTypingMarks().orElseThrow());
    }

    @Test
    void multiBlockBoldAndItalicUndoRedoPreserveSelection() {
        var document = document(paragraph(text("abc")), paragraph(text("def")));
        var session = new EditorSession(document, 0);
        var selection = new EditorState(document, new DocumentPosition(1, 2), new DocumentPosition(0, 1));
        session.setCurrent(selection);

        assertTrue(session.toggleMark(TextMark.BOLD));

        assertEquals(selection.anchor(), session.current().anchor());
        assertEquals(selection.active(), session.current().active());
        assertEquals(Set.of(TextMark.BOLD), textAt(session.current().document(), 0, 1).marks());
        assertEquals(Set.of(TextMark.BOLD), textAt(session.current().document(), 1, 0).marks());
        assertTrue(session.undo());
        assertEquals(selection.withoutExplicitTypingMarks(), session.current());
        assertTrue(session.redo());
        assertEquals(selection.anchor(), session.current().anchor());
        assertEquals(selection.active(), session.current().active());

        assertTrue(session.toggleMark(TextMark.ITALIC));
        assertEquals(Set.of(TextMark.BOLD, TextMark.ITALIC), textAt(session.current().document(), 0, 1).marks());
    }

    @Test
    void multiBlockBlockStyleUndoRedoAndNoOpHistory() {
        var document = document(paragraph(text("A")), heading(2, text("B")), paragraph(text("C")));
        var session = new EditorSession(document, 0);
        var selection = new EditorState(document, new DocumentPosition(0, 0), new DocumentPosition(2, 1));
        session.setCurrent(selection);

        assertEquals(BlockStyleSelectionState.mixed(), session.blockStyleSelectionState());
        assertTrue(session.setBlockStyle(BlockStyle.heading(3)));

        assertEquals(BlockStyleSelectionState.single(BlockStyle.heading(3)), session.blockStyleSelectionState());
        assertEquals(selection.anchor(), session.current().anchor());
        assertEquals(selection.active(), session.current().active());
        assertTrue(session.undo());
        assertEquals(selection.withoutExplicitTypingMarks(), session.current());
        assertTrue(session.redo());
        assertEquals(BlockStyleSelectionState.single(BlockStyle.heading(3)), session.blockStyleSelectionState());
        assertFalse(session.setBlockStyle(BlockStyle.heading(3)));
        assertTrue(session.undo());
        assertEquals(selection.withoutExplicitTypingMarks(), session.current());
    }

    @Test
    void multiBlockBlockStyleCorePreservesExplicitTypingMarks() {
        var document = document(paragraph(text("A")), paragraph(text("B")));
        var editor = new DocumentEditor();
        var state = new EditorState(
                document,
                new DocumentPosition(0, 0),
                new DocumentPosition(1, 1),
                java.util.Optional.of(Set.of(TextMark.ITALIC)));

        var result = editor.setBlockStyle(state, BlockStyle.heading(2));

        assertTrue(result.changed());
        assertEquals(Set.of(TextMark.ITALIC), result.explicitTypingMarks().orElseThrow());
    }

    @Test
    void boundaryOnlyInlineFormattingIsNoOpThroughSession() {
        var session = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 0);
        var selection = new EditorState(session.current().document(), new DocumentPosition(0, 3), new DocumentPosition(1, 0));
        session.setCurrent(selection);

        assertFalse(session.supportsInlineFormatting());
        assertFalse(session.toggleMark(TextMark.BOLD));
        assertEquals(selection.withoutExplicitTypingMarks(), session.current());
        assertFalse(session.canUndo());
    }

    @Test
    void insertEquationHistoryRestoresCaretAndBlockSelection() {
        var session = new EditorSession(document(paragraph(text("abcd"))), 0);
        var original = new EditorState(session.current().document(), new DocumentPosition(0, 2));
        session.setCurrent(original);

        assertTrue(session.insertEmptyEquation());

        assertEquals(List.of(Paragraph.class, dev.rgcb.scholar.document.EquationBlock.class, Paragraph.class), blockClasses(session.current().document()));
        assertEquals(new BlockSelection(1), session.current().selection());
        assertTrue(session.undo());
        assertEquals(original.withoutExplicitTypingMarks(), session.current());
        assertTrue(session.redo());
        assertEquals(new BlockSelection(1), session.current().selection());
    }

    @Test
    void insertEquationUndoRestoresExplicitTypingMarksSnapshot() {
        var session = new EditorSession(document(paragraph(text("abcd"))), 0);
        var original = new EditorState(
                session.current().document(),
                new DocumentPosition(0, 2),
                new DocumentPosition(0, 2),
                java.util.Optional.of(Set.of(TextMark.BOLD)));
        session.setCurrent(original);
        session.toggleMark(TextMark.BOLD);

        assertTrue(session.insertEmptyEquation());
        assertTrue(session.undo());

        assertEquals(Set.of(TextMark.BOLD), session.current().explicitTypingMarks().orElseThrow());
    }

    @Test
    void insertEquationReplacesTextSelectionAsSingleHistoryTransaction() {
        var session = new EditorSession(document(paragraph(text("abcDEFghi"))), 0);
        var original = new EditorState(
                session.current().document(),
                new DocumentPosition(0, 3),
                new DocumentPosition(0, 6));
        session.setCurrent(original);

        assertTrue(session.insertEmptyEquation());

        assertEquals(List.of(Paragraph.class, dev.rgcb.scholar.document.EquationBlock.class, Paragraph.class), blockClasses(session.current().document()));
        assertEquals(new BlockSelection(1), session.current().selection());
        assertTrue(session.canUndo());
        assertTrue(session.undo());
        assertEquals(original.withoutExplicitTypingMarks(), session.current());
        assertFalse(session.canUndo());
        assertTrue(session.redo());
        assertEquals(new BlockSelection(1), session.current().selection());
    }

    @Test
    void insertEquationUndoRestoresBackwardAndBoundarySelections() {
        var backward = new EditorSession(document(paragraph(text("AAA")), paragraph(text("BBB"))), 0);
        var backwardOriginal = new EditorState(
                backward.current().document(),
                new DocumentPosition(1, 2),
                new DocumentPosition(0, 1));
        backward.setCurrent(backwardOriginal);

        assertTrue(backward.insertEmptyEquation());
        assertEquals(new BlockSelection(1), backward.current().selection());
        assertTrue(backward.undo());
        assertEquals(backwardOriginal.withoutExplicitTypingMarks(), backward.current());

        var boundary = new EditorSession(document(paragraph(text("A")), paragraph(text("B"))), 0);
        var boundaryOriginal = new EditorState(
                boundary.current().document(),
                new DocumentPosition(0, 1),
                new DocumentPosition(1, 0));
        boundary.setCurrent(boundaryOriginal);

        assertTrue(boundary.insertEmptyEquation());
        assertEquals(List.of(Paragraph.class, dev.rgcb.scholar.document.EquationBlock.class, Paragraph.class), blockClasses(boundary.current().document()));
        assertTrue(boundary.undo());
        assertEquals(boundaryOriginal.withoutExplicitTypingMarks(), boundary.current());
    }

    @Test
    void insertEquationReplacementClearsExplicitTypingMarksInResult() {
        var editor = new DocumentEditor();
        var document = document(paragraph(text("abcd")));
        var original = new EditorState(
                document,
                new DocumentPosition(0, 1),
                new DocumentPosition(0, 3),
                java.util.Optional.of(Set.of(TextMark.BOLD)));

        var result = editor.insertEmptyEquation(original);

        assertEquals(new BlockSelection(1), result.editorState().selection());
        assertTrue(result.explicitTypingMarks().isEmpty());
    }

    @Test
    void deleteSelectedEquationHistoryRestoresObjectSelection() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of())), paragraph(text("B")));
        var session = new EditorSession(document, 0);
        var selected = new EditorState(document, new BlockSelection(1), java.util.Optional.empty());
        session.setCurrent(selected);

        assertTrue(session.deleteForward());

        assertEquals(List.of("A", "B"), blockTexts(session.current().document()));
        assertEquals(new DocumentPosition(0, 1), session.current().caret());
        assertTrue(session.undo());
        assertEquals(selected, session.current());
        assertTrue(session.redo());
        assertEquals(List.of("A", "B"), blockTexts(session.current().document()));
    }

    @Test
    void boundaryObjectSelectionNavigationDoesNotCreateHistory() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of())), paragraph(text("B")));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new DocumentPosition(0, 1)));

        assertFalse(session.deleteForward());
        assertEquals(new BlockSelection(1), session.current().selection());
        assertFalse(session.canUndo());

        session.moveRight();
        assertEquals(new DocumentPosition(2, 0), session.current().caret());
        assertFalse(session.canUndo());
    }

    @Test
    void blockSelectionEditingClipboardFormattingAndEnterAreNoOps() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of())));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), java.util.Optional.empty()));

        assertFalse(session.typeText("x"));
        assertFalse(session.pasteText("x"));
        assertTrue(session.copySelection().isEmpty());
        assertTrue(session.cutSelection().isEmpty());
        assertFalse(session.enter());
        assertFalse(session.toggleMark(TextMark.BOLD));
        assertFalse(session.supportsInlineFormatting());
        assertFalse(session.supportsBlockStyle());
        assertEquals(document, session.current().document());
        assertFalse(session.canUndo());
    }

    @Test
    void enterFromEquationBlockSelectionStartsEquationEditingWithoutHistory() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of())));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), java.util.Optional.empty()));

        assertFalse(session.enter());

        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), session.current().selection());
        assertEquals(document, session.current().document());
        assertFalse(session.canUndo());
    }

    @Test
    void equationEditingTypingMutatesEquationAndUndoRestoresMathCaret() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of())), paragraph(text("B")));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new BlockSelection(1), java.util.Optional.empty()));
        session.enter();

        assertTrue(session.typeText("v"));
        assertTrue(session.typeText("="));
        assertTrue(session.typeText("1"));
        assertTrue(session.typeText("2"));

        var equation = (dev.rgcb.scholar.document.EquationBlock) session.current().document().blocks().get(1);
        assertEquals(new MathSequence(List.of(
                new MathIdentifier("v"),
                new MathOperator("=", MathOperatorRole.RELATION),
                new MathNumber("12"))), equation.expression());
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 3))), session.current().selection());
        assertTrue(session.undo());
        assertEquals(new MathSequence(List.of()),
                ((dev.rgcb.scholar.document.EquationBlock) session.current().document().blocks().get(1)).expression());
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), session.current().selection());
        assertTrue(session.redo());
        assertEquals(equation.expression(), ((dev.rgcb.scholar.document.EquationBlock) session.current().document().blocks().get(1)).expression());
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 3))), session.current().selection());
    }

    @Test
    void equationLetterTypingUsesIndividualAtomsButOneTypingHistoryGroup() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of())));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), java.util.Optional.empty()));

        for (var character : List.of("v", "e", "l", "o", "c", "i", "t", "y")) {
            assertTrue(session.typeText(character));
        }

        var expected = new MathSequence(List.of(
                new MathIdentifier("v"),
                new MathIdentifier("e"),
                new MathIdentifier("l"),
                new MathIdentifier("o"),
                new MathIdentifier("c"),
                new MathIdentifier("i"),
                new MathIdentifier("t"),
                new MathIdentifier("y")));
        assertEquals(expected, equationExpression(session, 1));

        assertTrue(session.undo());
        assertEquals(new MathSequence(List.of()), equationExpression(session, 1));
        assertTrue(session.redo());
        assertEquals(expected, equationExpression(session, 1));
    }

    @Test
    void equationEditingRootBoundariesExitToDocumentNavigationWithoutHistory() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of(new MathIdentifier("x")))), paragraph(text("B")));
        var left = new EditorSession(document, 0);
        left.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), java.util.Optional.empty()));

        left.moveLeft();

        assertEquals(new DocumentPosition(0, 1), left.current().caret());
        assertFalse(left.canUndo());

        var right = new EditorSession(document, 0);
        right.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));

        right.moveRight();

        assertEquals(new DocumentPosition(2, 0), right.current().caret());
        assertFalse(right.canUndo());
    }

    @Test
    void equationEditingCanEditInsideExistingToken() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of(new MathNumber("123")))));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(
                1,
                new MathCaretSelection(new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 1))), java.util.Optional.empty()));

        assertTrue(session.typeText("4"));
        assertTrue(session.deleteBackward());

        assertEquals(new MathSequence(List.of(new MathNumber("123"))),
                ((dev.rgcb.scholar.document.EquationBlock) session.current().document().blocks().get(1)).expression());
    }

    @Test
    void insertFractionIsAtomicAndRedoRestoresNumeratorCaret() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of())));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), java.util.Optional.empty()));

        assertTrue(session.supportsInsertFraction());
        assertTrue(session.insertFraction());

        var expectedExpression = new MathSequence(List.of(emptyFraction()));
        var expectedSelection = new EquationEditingSelection(
                1,
                new MathCaretSelection(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(0)).append(new dev.rgcb.scholar.math.editor.FractionNumerator()), 0)));
        assertEquals(expectedExpression, equationExpression(session, 1));
        assertEquals(expectedSelection, session.current().selection());

        assertTrue(session.undo());
        assertEquals(new MathSequence(List.of()), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), session.current().selection());

        assertTrue(session.redo());
        assertEquals(expectedExpression, equationExpression(session, 1));
        assertEquals(expectedSelection, session.current().selection());
    }

    @Test
    void insertFractionBreaksMathTypingCoalescingGroups() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of())));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), java.util.Optional.empty()));

        assertTrue(session.typeText("1"));
        assertTrue(session.typeText("2"));
        assertTrue(session.insertFraction());
        assertTrue(session.typeText("3"));
        assertTrue(session.typeText("4"));

        assertEquals(new MathSequence(List.of(
                new MathNumber("12"),
                new MathFraction(new MathSequence(List.of(new MathNumber("34"))), new MathSequence(List.of())))), equationExpression(session, 1));

        assertTrue(session.undo());
        assertEquals(new MathSequence(List.of(new MathNumber("12"), emptyFraction())), equationExpression(session, 1));
        assertTrue(session.undo());
        assertEquals(new MathSequence(List.of(new MathNumber("12"))), equationExpression(session, 1));
        assertTrue(session.undo());
        assertEquals(new MathSequence(List.of()), equationExpression(session, 1));
    }

    @Test
    void slashStillCreatesLinearDivisionAfterFractionSupport() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of())));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), java.util.Optional.empty()));

        session.typeText("a");
        session.typeText("/");
        session.typeText("b");

        assertEquals(new MathSequence(List.of(
                new MathIdentifier("a"),
                new MathOperator("/", MathOperatorRole.BINARY),
                new MathIdentifier("b"))), equationExpression(session, 1));
    }

    @Test
    void shiftArrowsCreateMathRangeAndPlainArrowsCollapseIt() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of(new MathIdentifier("x"), new MathIdentifier("y")))));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), java.util.Optional.empty()));

        session.extendRight();

        assertEquals(new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 1))), session.current().selection());

        session.moveRight();

        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), session.current().selection());
    }

    @Test
    void typingOverMathRangeIsOneUndoableReplacement() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of(new MathIdentifier("a"), new MathIdentifier("b"), new MathIdentifier("c")))));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 2))), java.util.Optional.empty()));

        assertTrue(session.typeText("x"));

        assertEquals(new MathSequence(List.of(new MathIdentifier("x"), new MathIdentifier("c"))), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), session.current().selection());

        assertTrue(session.undo());
        assertEquals(new MathSequence(List.of(new MathIdentifier("a"), new MathIdentifier("b"), new MathIdentifier("c"))), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 2))), session.current().selection());
    }

    @Test
    void insertFractionOverMathRangeIsOneStructuralHistoryTransaction() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of(new MathIdentifier("a"), new MathIdentifier("b")))));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 2))), java.util.Optional.empty()));

        assertTrue(session.insertFraction());

        assertEquals(new MathSequence(List.of(new MathFraction(
                new MathSequence(List.of(new MathIdentifier("a"), new MathIdentifier("b"))),
                new MathSequence(List.of())))), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(
                new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(0)).append(new dev.rgcb.scholar.math.editor.FractionDenominator()), 0))), session.current().selection());

        assertTrue(session.undo());
        assertEquals(new MathSequence(List.of(new MathIdentifier("a"), new MathIdentifier("b"))), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 2))), session.current().selection());
    }

    @Test
    void insertRootAtCaretIsUndoableAndRedoRestoresRadicandCaret() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of())));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), java.util.Optional.empty()));

        assertTrue(session.supportsInsertRoot());
        assertTrue(session.insertRoot());

        var rootPath = MathPath.ROOT.append(new SequenceChild(0));
        assertEquals(new MathSequence(List.of(emptyRoot())), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(rootPath.append(new RootRadicand()), 0))), session.current().selection());

        assertTrue(session.undo());
        assertEquals(new MathSequence(List.of()), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), session.current().selection());

        assertTrue(session.redo());
        assertEquals(new MathSequence(List.of(emptyRoot())), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(rootPath.append(new RootRadicand()), 0))), session.current().selection());
    }

    @Test
    void insertRootOverMathRangeIsOneStructuralHistoryTransaction() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of(new MathIdentifier("x"), new MathOperator("+", MathOperatorRole.BINARY), new MathNumber("1")))));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 3));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, selection), java.util.Optional.empty()));

        assertTrue(session.insertRoot());

        assertEquals(new MathSequence(List.of(new MathRoot(new MathSequence(List.of(new MathIdentifier("x"), new MathOperator("+", MathOperatorRole.BINARY), new MathNumber("1"))), java.util.Optional.empty()))), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), session.current().selection());
        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertEquals(new EquationEditingSelection(1, selection), session.current().selection());
    }

    @Test
    void insertGroupAtCaretIsUndoableAndRedoRestoresContentCaret() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of())));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), java.util.Optional.empty()));

        assertTrue(session.supportsInsertGroup(MathDelimiter.PARENTHESES));
        assertTrue(session.insertGroup(MathDelimiter.PARENTHESES));

        var groupPath = MathPath.ROOT.append(new SequenceChild(0));
        assertEquals(sequence(new MathGroup(sequence(), MathDelimiter.PARENTHESES)), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(groupPath.append(new GroupContent()), 0))), session.current().selection());

        assertTrue(session.undo());
        assertEquals(sequence(), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), session.current().selection());

        assertTrue(session.redo());
        assertEquals(sequence(new MathGroup(sequence(), MathDelimiter.PARENTHESES)), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(groupPath.append(new GroupContent()), 0))), session.current().selection());
    }

    @Test
    void insertGroupOverMathRangeIsOneStructuralHistoryTransaction() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(sequence(identifier("x"), operator("+"), number("1"))));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 3));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, selection), java.util.Optional.empty()));

        assertTrue(session.insertGroup(MathDelimiter.BRACKETS));

        assertEquals(sequence(new MathGroup(sequence(identifier("x"), operator("+"), number("1")), MathDelimiter.BRACKETS)), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), session.current().selection());
        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertEquals(new EquationEditingSelection(1, selection), session.current().selection());
    }

    @Test
    void scriptKeyboardAuthoringIsStructuralAndDoesNotCoalesceWithTypedExponent() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of())));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), java.util.Optional.empty()));

        assertTrue(session.typeText("x"));
        assertTrue(session.typeText("^"));
        assertTrue(session.typeText("2"));

        var scriptPath = MathPath.ROOT.append(new SequenceChild(0));
        assertEquals(new MathSequence(List.of(new MathScript(new MathIdentifier("x"), java.util.Optional.empty(), java.util.Optional.of(new MathSequence(List.of(new MathNumber("2"))))))), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(scriptPath.append(new ScriptSuperscript()), 1))), session.current().selection());

        assertTrue(session.undo());
        assertEquals(new MathSequence(List.of(new MathScript(new MathIdentifier("x"), java.util.Optional.empty(), java.util.Optional.of(new MathSequence(List.of()))))), equationExpression(session, 1));
        assertTrue(session.undo());
        assertEquals(new MathSequence(List.of(new MathIdentifier("x"))), equationExpression(session, 1));
        assertTrue(session.undo());
        assertEquals(new MathSequence(List.of()), equationExpression(session, 1));
    }

    @Test
    void editingOperationsWorkInsideHeadingAndPreserveHeadingLevel() {
        var session = new EditorSession(document(heading(2, text("Result"))), 0);

        assertTrue(session.typeText("s"));
        session.moveLeft();
        session.extendLeft();
        assertTrue(session.toggleMark(TextMark.BOLD));
        var copied = session.copySelection().orElseThrow();
        var cut = session.cutSelection().orElseThrow();
        assertTrue(session.applyCut(cut));
        assertTrue(session.pasteText(copied));

        assertEquals(2, heading(session.current().document()).level());
        assertEquals("Results", paragraphText(session.current().document()));
    }

    @Test
    void semanticTokenConversionThroughSessionIsUndoableAndRestoresSelection() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of(
                new MathIdentifier("s"),
                new MathIdentifier("i"),
                new MathIdentifier("n")))));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 3));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, selection), java.util.Optional.empty()));

        assertTrue(session.supportsSemanticTokenConversion(SemanticMathTokenKind.NAMED_OPERATOR));
        assertEquals("sin", session.semanticTokenDraft(SemanticMathTokenKind.NAMED_OPERATOR).orElseThrow().content());
        assertTrue(session.applySemanticToken(SemanticMathTokenKind.NAMED_OPERATOR, "sin"));

        assertEquals(new MathSequence(List.of(new MathNamedOperator("sin"))), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), session.current().selection());
        assertTrue(session.undo());
        assertEquals(document, session.current().document());
        assertEquals(new EquationEditingSelection(1, selection), session.current().selection());
        assertTrue(session.redo());
        assertEquals(new MathSequence(List.of(new MathNamedOperator("sin"))), equationExpression(session, 1));
    }

    @Test
    void semanticTokenConversionAuthorsMultiWordMathTextAndEditsExistingTokens() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of(
                new MathIdentifier("i"),
                new MathIdentifier("f"),
                new MathNamedOperator("sin")))));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 2))), java.util.Optional.empty()));

        assertTrue(session.applySemanticToken(SemanticMathTokenKind.MATH_TEXT, "for all"));
        assertEquals(new MathSequence(List.of(new MathText("for all"), new MathNamedOperator("sin"))), equationExpression(session, 1));

        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 1),
                new MathSequencePosition(MathPath.ROOT, 2))), java.util.Optional.empty()));
        assertEquals("sin", session.semanticTokenDraft(SemanticMathTokenKind.NAMED_OPERATOR).orElseThrow().content());
        assertTrue(session.applySemanticToken(SemanticMathTokenKind.NAMED_OPERATOR, "rank"));

        assertEquals(new MathSequence(List.of(new MathText("for all"), new MathNamedOperator("rank"))), equationExpression(session, 1));
    }

    @Test
    void semanticTokenConversionRejectsInvalidSelectionsAndInvalidContent() {
        var document = document(paragraph(text("A")), new dev.rgcb.scholar.document.EquationBlock(new MathSequence(List.of(
                new MathIdentifier("x"),
                new MathOperator("+", MathOperatorRole.BINARY),
                new MathIdentifier("y")))));
        var session = new EditorSession(document, 0);
        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 3))), java.util.Optional.empty()));

        assertFalse(session.supportsSemanticTokenConversion(SemanticMathTokenKind.NAMED_OPERATOR));
        assertFalse(session.applySemanticToken(SemanticMathTokenKind.NAMED_OPERATOR, "sin"));

        session.setCurrent(new EditorState(document, new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));
        assertFalse(session.applySemanticToken(SemanticMathTokenKind.MATH_TEXT, " if"));
        assertFalse(session.applySemanticToken(SemanticMathTokenKind.MATH_TEXT, "if "));
        assertFalse(session.applySemanticToken(SemanticMathTokenKind.NAMED_OPERATOR, " "));
        assertEquals(new MathSequence(List.of(new MathIdentifier("x"), new MathOperator("+", MathOperatorRole.BINARY), new MathIdentifier("y"))), equationExpression(session, 1));
    }

    private static EditorSession session(String text, int blockIndex) {
        return new EditorSession(document(paragraph(text(text))), blockIndex);
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

    private static Heading heading(Document document) {
        return (Heading) document.blocks().get(0);
    }

    private static dev.rgcb.scholar.math.MathExpression equationExpression(EditorSession session, int blockIndex) {
        return ((dev.rgcb.scholar.document.EquationBlock) session.current().document().blocks().get(blockIndex)).expression();
    }

    private static MathFraction emptyFraction() {
        return new MathFraction(new MathSequence(List.of()), new MathSequence(List.of()));
    }

    private static MathRoot emptyRoot() {
        return new MathRoot(new MathSequence(List.of()), java.util.Optional.empty());
    }

    private static MathSequence sequence(dev.rgcb.scholar.math.MathExpression... expressions) {
        return new MathSequence(List.of(expressions));
    }

    private static MathIdentifier identifier(String value) {
        return new MathIdentifier(value);
    }

    private static MathNumber number(String value) {
        return new MathNumber(value);
    }

    private static MathOperator operator(String symbol) {
        return new MathOperator(symbol, MathOperatorRole.BINARY);
    }

    private static Text text(String content, TextMark... marks) {
        return new Text(content, Set.of(marks));
    }

    private static Text textAt(Document document, int index) {
        return textAt(document, 0, index);
    }

    private static Text textAt(Document document, int blockIndex, int inlineIndex) {
        var block = document.blocks().get(blockIndex);
        var content = block instanceof Paragraph paragraph ? paragraph.content() : ((Heading) block).content();
        return (Text) content.nodes().get(inlineIndex);
    }

    private static String paragraphText(Document document) {
        return blockText(document, 0);
    }

    private static List<String> blockTexts(Document document) {
        return document.blocks().stream()
                .filter(block -> block instanceof Paragraph || block instanceof Heading)
                .map(block -> {
                    var content = block instanceof Paragraph paragraph ? paragraph.content() : ((Heading) block).content();
                    return content.nodes().stream()
                            .map(Text.class::cast)
                            .map(Text::content)
                            .reduce("", String::concat);
                })
                .toList();
    }

    private static List<Class<? extends BlockNode>> blockClasses(Document document) {
        return document.blocks().stream()
                .map(BlockNode::getClass)
                .toList();
    }

    private static String blockText(Document document, int blockIndex) {
        var block = document.blocks().get(blockIndex);
        var content = block instanceof Paragraph paragraph ? paragraph.content() : ((Heading) block).content();
        return content.nodes().stream()
                .map(Text.class::cast)
                .map(Text::content)
                .reduce("", String::concat);
    }
}
