package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathSequence;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DocumentEditorTest {
    private final DocumentEditor editor = new DocumentEditor();

    @Test
    void createsInitialCaretAtEndOfEditableParagraph() {
        var document = document(paragraph(text("velocity")));

        var state = editor.initialState(document, 0);

        assertEquals(new DocumentPosition(0, 8), state.caret());
        assertEquals(state.caret(), state.anchor());
        assertEquals(state.caret(), state.active());
    }

    @Test
    void rejectsInvalidPositions() {
        var document = document(paragraph(text("abc")));

        assertThrows(IllegalArgumentException.class, () -> editor.replaceRange(
                document,
                DocumentRange.caret(new DocumentPosition(1, 0)),
                "x"));
        assertThrows(IllegalArgumentException.class, () -> editor.replaceRange(
                document,
                DocumentRange.caret(new DocumentPosition(0, 4)),
                "x"));
    }

    @Test
    void traversesUserCharacterBoundaries() {
        assertEquals(2, TextBoundary.characterCount("Δx"));
        assertEquals(1, TextBoundary.characterCount("θ"));
        assertEquals(1, TextBoundary.characterCount("λ"));
        assertEquals(4, TextBoundary.characterCount("café"));
        assertEquals(1, TextBoundary.characterCount("e\u0301"));
    }

    @Test
    void insertAtStartMiddleAndEnd() {
        var document = document(paragraph(text("ac")));
        var atStart = editor.replaceRange(document, DocumentRange.caret(new DocumentPosition(0, 0)), "b");
        var middle = editor.replaceRange(document, DocumentRange.caret(new DocumentPosition(0, 1)), "b");
        var atEnd = editor.replaceRange(document, DocumentRange.caret(new DocumentPosition(0, 2)), "b");

        assertEquals("bac", paragraphText(atStart.document()));
        assertEquals("abc", paragraphText(middle.document()));
        assertEquals("acb", paragraphText(atEnd.document()));
    }

    @Test
    void insertAdvancesCaretByUserCharacters() {
        var result = editor.replaceRange(
                document(paragraph(text("x"))),
                DocumentRange.caret(new DocumentPosition(0, 1)),
                "Δθe\u0301");

        assertEquals(new DocumentPosition(0, 4), result.caret());
    }

    @Test
    void backspaceDeletesPreviousUserCharacter() {
        var state = new EditorState(document(paragraph(text("ae\u0301b"))), new DocumentPosition(0, 2));

        var result = editor.deleteBackward(state);

        assertEquals("ab", paragraphText(result.document()));
        assertEquals(new DocumentPosition(0, 1), result.caret());
    }

    @Test
    void deleteDeletesNextUserCharacter() {
        var state = new EditorState(document(paragraph(text("aθb"))), new DocumentPosition(0, 1));

        var result = editor.deleteForward(state);

        assertEquals("ab", paragraphText(result.document()));
        assertEquals(new DocumentPosition(0, 1), result.caret());
    }

    @Test
    void boundaryDeletesAndMovesAreNoOps() {
        var document = document(paragraph(text("abc")));
        var atStart = new EditorState(document, new DocumentPosition(0, 0));
        var atEnd = new EditorState(document, new DocumentPosition(0, 3));

        assertEquals(atStart, editor.deleteBackward(atStart).editorState());
        assertEquals(atEnd, editor.deleteForward(atEnd).editorState());
        assertEquals(atStart, editor.moveLeft(atStart).editorState());
        assertEquals(atEnd, editor.moveRight(atEnd).editorState());
    }

    @Test
    void paragraphEnterSplitsAtStartMiddleEndAndEmpty() {
        var middle = editor.insertParagraphBreak(new EditorState(document(paragraph(text("hello world"))), new DocumentPosition(0, 5)), Set.of(TextMark.BOLD));
        var start = editor.insertParagraphBreak(new EditorState(document(paragraph(text("hello"))), new DocumentPosition(0, 0)), Set.of());
        var end = editor.insertParagraphBreak(new EditorState(document(paragraph(text("hello"))), new DocumentPosition(0, 5)), Set.of());
        var empty = editor.insertParagraphBreak(new EditorState(document(new Paragraph(new InlineContent(List.of()))), new DocumentPosition(0, 0)), Set.of());

        assertEquals(List.of("hello", " world"), blockTexts(middle.document()));
        assertEquals(new DocumentPosition(1, 0), middle.caret());
        assertEquals(Set.of(TextMark.BOLD), middle.explicitTypingMarks().orElseThrow());
        assertEquals(List.of("", "hello"), blockTexts(start.document()));
        assertEquals(List.of("hello", ""), blockTexts(end.document()));
        assertEquals(List.of("", ""), blockTexts(empty.document()));
    }

    @Test
    void selectionEnterDeletesSelectionAndSplitsAsOneOperation() {
        var document = document(paragraph(text("hello beautiful world")));
        var state = new EditorState(document, new DocumentPosition(0, 6), new DocumentPosition(0, 16));

        var result = editor.insertParagraphBreak(state, Set.of(TextMark.ITALIC));

        assertEquals(List.of("hello ", "world"), blockTexts(result.document()));
        assertEquals(new DocumentPosition(1, 0), result.caret());
        assertFalse(result.editorState().hasSelection());
        assertEquals(Set.of(TextMark.ITALIC), result.explicitTypingMarks().orElseThrow());
    }

    @Test
    void headingEnterUsesApprovedBoundarySemantics() {
        var middle = editor.insertParagraphBreak(new EditorState(document(heading(6, text("Results"))), new DocumentPosition(0, 3)), Set.of());
        var end = editor.insertParagraphBreak(new EditorState(document(heading(1, text("Results"))), new DocumentPosition(0, 7)), Set.of());
        var start = editor.insertParagraphBreak(new EditorState(document(heading(2, text("Results"))), new DocumentPosition(0, 0)), Set.of());
        var empty = editor.insertParagraphBreak(new EditorState(document(new Heading(3, new InlineContent(List.of()))), new DocumentPosition(0, 0)), Set.of());

        assertEquals(6, ((Heading) middle.document().blocks().get(0)).level());
        assertEquals(6, ((Heading) middle.document().blocks().get(1)).level());
        assertEquals(List.of("Res", "ults"), blockTexts(middle.document()));
        assertEquals(new DocumentPosition(1, 0), middle.caret());
        assertEquals(Heading.class, end.document().blocks().get(0).getClass());
        assertEquals(Paragraph.class, end.document().blocks().get(1).getClass());
        assertEquals(List.of("Results", ""), blockTexts(end.document()));
        assertEquals(new DocumentPosition(1, 0), end.caret());
        assertEquals(Paragraph.class, start.document().blocks().get(0).getClass());
        assertEquals(Heading.class, start.document().blocks().get(1).getClass());
        assertEquals(List.of("", "Results"), blockTexts(start.document()));
        assertEquals(new DocumentPosition(0, 0), start.caret());
        assertEquals(1, empty.document().blocks().size());
        assertEquals(Paragraph.class, empty.document().blocks().get(0).getClass());
        assertEquals(new DocumentPosition(0, 0), empty.caret());
    }

    @Test
    void headingSelectionEnterEvaluatesSemanticsAfterDeletion() {
        var middle = editor.insertParagraphBreak(
                new EditorState(document(heading(2, text("abcdef"))), new DocumentPosition(0, 2), new DocumentPosition(0, 4)),
                Set.of());
        var end = editor.insertParagraphBreak(
                new EditorState(document(heading(2, text("abcdef"))), new DocumentPosition(0, 3), new DocumentPosition(0, 6)),
                Set.of());
        var empty = editor.insertParagraphBreak(
                new EditorState(document(heading(2, text("abcdef"))), new DocumentPosition(0, 0), new DocumentPosition(0, 6)),
                Set.of());

        assertEquals(List.of("ab", "ef"), blockTexts(middle.document()));
        assertEquals(List.of("abc", ""), blockTexts(end.document()));
        assertEquals(Paragraph.class, end.document().blocks().get(1).getClass());
        assertEquals(1, empty.document().blocks().size());
        assertEquals(Paragraph.class, empty.document().blocks().get(0).getClass());
    }

    @Test
    void backspaceAndDeleteJoinEditableBlockBoundaryWithLeftStyleOwnership() {
        var paragraphJoin = editor.deleteBackward(new EditorState(document(paragraph(text("hello")), paragraph(text("world"))), new DocumentPosition(1, 0)));
        var headingLeft = editor.deleteBackward(new EditorState(document(heading(2, text("Results")), paragraph(text("Body"))), new DocumentPosition(1, 0)));
        var paragraphLeft = editor.deleteForward(new EditorState(document(paragraph(text("Body")), heading(2, text("Results"))), new DocumentPosition(0, 4)));
        var headingLevelWins = editor.deleteForward(new EditorState(document(heading(2, text("A")), heading(3, text("B"))), new DocumentPosition(0, 1)));

        assertEquals(List.of("helloworld"), blockTexts(paragraphJoin.document()));
        assertEquals(new DocumentPosition(0, 5), paragraphJoin.caret());
        assertEquals(Heading.class, headingLeft.document().blocks().get(0).getClass());
        assertEquals(2, ((Heading) headingLeft.document().blocks().get(0)).level());
        assertEquals("ResultsBody", blockText(headingLeft.document(), 0));
        assertEquals(Paragraph.class, paragraphLeft.document().blocks().get(0).getClass());
        assertEquals("BodyResults", blockText(paragraphLeft.document(), 0));
        assertEquals(2, ((Heading) headingLevelWins.document().blocks().get(0)).level());
        assertEquals("AB", blockText(headingLevelWins.document(), 0));
    }

    @Test
    void replaceRangeDeletesAndReplacesAcrossTwoBlocks() {
        var document = document(paragraph(text("abc")), paragraph(text("def")));
        var range = new DocumentRange(new DocumentPosition(0, 1), new DocumentPosition(1, 2));

        var deleted = editor.replaceRange(document, range, "");
        var replaced = editor.replaceRange(document, range, "X", Set.of(TextMark.BOLD));

        assertEquals(List.of("af"), blockTexts(deleted.document()));
        assertEquals(new DocumentPosition(0, 1), deleted.caret());
        assertEquals(List.of("aXf"), blockTexts(replaced.document()));
        assertEquals(new DocumentPosition(0, 2), replaced.caret());
        assertEquals(Set.of(TextMark.BOLD), ((Text) ((Paragraph) replaced.document().blocks().get(0)).content().nodes().get(1)).marks());
    }

    @Test
    void replaceRangeRemovesIntermediateBlocksAndPreservesFollowingBlocks() {
        var document = document(
                paragraph(text("abc")),
                heading(2, text("middle1")),
                paragraph(text("middle2")),
                paragraph(text("xyz")),
                paragraph(text("later")));

        var result = editor.replaceRange(
                document,
                new DocumentRange(new DocumentPosition(0, 1), new DocumentPosition(3, 2)),
                "");

        assertEquals(List.of("az", "later"), blockTexts(result.document()));
        assertEquals(new DocumentPosition(0, 1), result.caret());
        assertEquals(document.blocks().get(4), result.document().blocks().get(1));
    }

    @Test
    void multiBlockReplacementUsesLeftBlockOwnership() {
        var headingParagraph = editor.replaceRange(
                document(heading(2, text("Title")), paragraph(text("Body"))),
                new DocumentRange(new DocumentPosition(0, 0), new DocumentPosition(1, 2)),
                "");
        var paragraphHeading = editor.replaceRange(
                document(paragraph(text("Intro")), heading(3, text("Title"))),
                new DocumentRange(new DocumentPosition(0, 2), new DocumentPosition(1, 2)),
                "");
        var headingHeading = editor.replaceRange(
                document(heading(2, text("Left")), heading(3, text("Right"))),
                new DocumentRange(new DocumentPosition(0, 2), new DocumentPosition(1, 2)),
                "");

        assertEquals(Heading.class, headingParagraph.document().blocks().get(0).getClass());
        assertEquals(2, ((Heading) headingParagraph.document().blocks().get(0)).level());
        assertEquals("dy", blockText(headingParagraph.document(), 0));
        assertEquals(Paragraph.class, paragraphHeading.document().blocks().get(0).getClass());
        assertEquals("Intle", blockText(paragraphHeading.document(), 0));
        assertEquals(2, ((Heading) headingHeading.document().blocks().get(0)).level());
        assertEquals("Leght", blockText(headingHeading.document(), 0));
    }

    @Test
    void deletingEntireCoveredRangeLeavesOneEmptyLeftOwnedBlock() {
        var paragraph = editor.replaceRange(
                document(paragraph(text("A")), paragraph(text("B"))),
                new DocumentRange(new DocumentPosition(0, 0), new DocumentPosition(1, 1)),
                "");
        var heading = editor.replaceRange(
                document(heading(6, text("A")), paragraph(text("B"))),
                new DocumentRange(new DocumentPosition(0, 0), new DocumentPosition(1, 1)),
                "");

        assertEquals(1, paragraph.document().blocks().size());
        assertEquals(Paragraph.class, paragraph.document().blocks().get(0).getClass());
        assertTrue(((Paragraph) paragraph.document().blocks().get(0)).content().nodes().isEmpty());
        assertEquals(Heading.class, heading.document().blocks().get(0).getClass());
        assertEquals(6, ((Heading) heading.document().blocks().get(0)).level());
        assertTrue(((Heading) heading.document().blocks().get(0)).content().nodes().isEmpty());
    }

    @Test
    void boundaryOnlySelectionDeleteJoinsBlocksForwardAndBackward() {
        var document = document(paragraph(text("abc")), paragraph(text("def")));
        var forward = new EditorState(document, new DocumentPosition(0, 3), new DocumentPosition(1, 0));
        var backward = new EditorState(document, new DocumentPosition(1, 0), new DocumentPosition(0, 3));

        var forwardDelete = editor.deleteBackward(forward);
        var backwardDelete = editor.deleteForward(backward);

        assertEquals(List.of("abcdef"), blockTexts(forwardDelete.document()));
        assertEquals(List.of("abcdef"), blockTexts(backwardDelete.document()));
        assertEquals(new DocumentPosition(0, 3), forwardDelete.caret());
        assertEquals(new DocumentPosition(0, 3), backwardDelete.caret());
    }

    @Test
    void multiBlockReplacementHandlesEmptyBlocksWithoutEmptyTextNodes() {
        var textEmptyText = editor.replaceRange(
                document(paragraph(text("A")), new Paragraph(new InlineContent(List.of())), paragraph(text("B"))),
                new DocumentRange(new DocumentPosition(0, 1), new DocumentPosition(2, 0)),
                "");
        var emptyEmpty = editor.replaceRange(
                document(new Paragraph(new InlineContent(List.of())), new Paragraph(new InlineContent(List.of()))),
                new DocumentRange(new DocumentPosition(0, 0), new DocumentPosition(1, 0)),
                "");

        assertEquals(List.of("AB"), blockTexts(textEmptyText.document()));
        assertEquals(List.of(""), blockTexts(emptyEmpty.document()));
        assertTrue(((Paragraph) emptyEmpty.document().blocks().get(0)).content().nodes().isEmpty());
    }

    @Test
    void multiBlockReplacementUsesTextBoundaryForUnicode() {
        var document = document(paragraph(text("café Δx")), paragraph(text("θ λ e\u0301")));

        var result = editor.replaceRange(
                document,
                new DocumentRange(new DocumentPosition(0, 3), new DocumentPosition(1, 3)),
                "Ω");

        assertEquals(List.of("cafΩ e\u0301"), blockTexts(result.document()));
        assertEquals(new DocumentPosition(0, 4), result.caret());
    }

    @Test
    void multiBlockReplacementRejectsEquationBarrier() {
        var document = document(paragraph(text("abc")), new EquationBlock(new MathIdentifier("x")), paragraph(text("def")));

        assertThrows(IllegalArgumentException.class, () -> editor.replaceRange(
                document,
                new DocumentRange(new DocumentPosition(0, 1), new DocumentPosition(2, 1)),
                ""));
    }

    @Test
    void boundaryJoinPreservesInlineMarksAndSegmentation() {
        var document = document(
                paragraph(text("a", TextMark.BOLD), text("b", TextMark.BOLD)),
                paragraph(text("c", TextMark.ITALIC)));

        var result = editor.deleteForward(new EditorState(document, new DocumentPosition(0, 2)));

        assertEquals(List.of(text("a", TextMark.BOLD), text("b", TextMark.BOLD), text("c", TextMark.ITALIC)),
                ((Paragraph) result.document().blocks().get(0)).content().nodes());
    }

    @Test
    void unsupportedBoundaryDeleteIsNoOp() {
        var paragraphEquation = document(paragraph(text("abc")), new EquationBlock(new MathIdentifier("x")));
        var equationParagraph = document(new EquationBlock(new MathIdentifier("x")), paragraph(text("abc")));

        var delete = editor.deleteForward(new EditorState(paragraphEquation, new DocumentPosition(0, 3)));
        var backspace = editor.deleteBackward(new EditorState(equationParagraph, new DocumentPosition(1, 0)));

        assertFalse(delete.changed());
        assertEquals(paragraphEquation, delete.document());
        assertFalse(backspace.changed());
        assertEquals(equationParagraph, backspace.document());
    }

    @Test
    void emptyBlockBoundaryJoinsAreValid() {
        var rightEmpty = editor.deleteBackward(new EditorState(document(paragraph(text("hello")), new Paragraph(new InlineContent(List.of()))), new DocumentPosition(1, 0)));
        var leftEmpty = editor.deleteForward(new EditorState(document(new Paragraph(new InlineContent(List.of())), paragraph(text("world"))), new DocumentPosition(0, 0)));
        var headingEmpty = editor.deleteForward(new EditorState(document(new Heading(2, new InlineContent(List.of())), paragraph(text("Body"))), new DocumentPosition(0, 0)));

        assertEquals(List.of("hello"), blockTexts(rightEmpty.document()));
        assertEquals(new DocumentPosition(0, 5), rightEmpty.caret());
        assertEquals(List.of("world"), blockTexts(leftEmpty.document()));
        assertEquals(new DocumentPosition(0, 0), leftEmpty.caret());
        assertEquals(Heading.class, headingEmpty.document().blocks().get(0).getClass());
        assertEquals("Body", blockText(headingEmpty.document(), 0));
    }

    @Test
    void plainLeftAndRightNavigateAcrossAdjacentEditableBlocksOnly() {
        var document = document(paragraph(text("hello")), heading(2, text("world")));
        var leftAcross = editor.moveLeft(new EditorState(document, new DocumentPosition(1, 0)));
        var rightAcross = editor.moveRight(new EditorState(document, new DocumentPosition(0, 5)));
        var unsupportedLeft = editor.moveLeft(new EditorState(document(new EquationBlock(new MathIdentifier("x")), paragraph(text("abc"))), new DocumentPosition(1, 0)));
        var unsupportedRight = editor.moveRight(new EditorState(document(paragraph(text("abc")), new EquationBlock(new MathIdentifier("x"))), new DocumentPosition(0, 3)));

        assertEquals(new DocumentPosition(0, 5), leftAcross.caret());
        assertEquals(new DocumentPosition(1, 0), rightAcross.caret());
        assertEquals(new BlockSelection(0), unsupportedLeft.editorState().selection());
        assertEquals(new BlockSelection(1), unsupportedRight.editorState().selection());
    }

    @Test
    void plainLeftAndRightCollapseMultiBlockSelectionsToNormalizedBoundaries() {
        var document = document(paragraph(text("abc")), paragraph(text("def")));
        var forward = new EditorState(document, new DocumentPosition(0, 2), new DocumentPosition(1, 1));
        var backward = new EditorState(document, new DocumentPosition(1, 1), new DocumentPosition(0, 2));

        assertEquals(new DocumentPosition(0, 2), editor.moveLeft(forward).caret());
        assertEquals(new DocumentPosition(1, 1), editor.moveRight(forward).caret());
        assertEquals(new DocumentPosition(0, 2), editor.moveLeft(backward).caret());
        assertEquals(new DocumentPosition(1, 1), editor.moveRight(backward).caret());
    }

    @Test
    void shiftMovementShrinksCrossesAnchorAndReversesDirection() {
        var document = document(paragraph(text("ab")), paragraph(text("cd")), paragraph(text("ef")));
        var state = new EditorState(document, new DocumentPosition(1, 1));

        state = editor.extendLeft(state);
        state = editor.extendLeft(state);
        state = editor.extendLeft(state);
        assertEquals(new DocumentPosition(1, 1), state.anchor());
        assertEquals(new DocumentPosition(0, 1), state.active());

        state = editor.extendRight(state);
        state = editor.extendRight(state);
        state = editor.extendRight(state);
        state = editor.extendRight(state);
        state = editor.extendRight(state);

        assertEquals(new DocumentPosition(1, 1), state.anchor());
        assertEquals(new DocumentPosition(2, 0), state.active());
        assertEquals(new DocumentRange(new DocumentPosition(1, 1), new DocumentPosition(2, 0)), state.selectionRange());
    }

    @Test
    void shiftMovementCrossesEditableBlockBoundariesAndStopsAtEquations() {
        var document = document(paragraph(text("abc")), paragraph(text("def")));

        assertEquals(new EditorState(document, new DocumentPosition(1, 0), new DocumentPosition(0, 3)),
                editor.extendLeft(new EditorState(document, new DocumentPosition(1, 0))));
        assertEquals(new EditorState(document, new DocumentPosition(0, 3), new DocumentPosition(1, 0)),
                editor.extendRight(new EditorState(document, new DocumentPosition(0, 3))));

        var withEquation = document(paragraph(text("abc")), new EquationBlock(new MathIdentifier("x")), paragraph(text("def")));
        assertEquals(new EditorState(withEquation, new DocumentPosition(0, 3)),
                editor.extendRight(new EditorState(withEquation, new DocumentPosition(0, 3))));
        assertEquals(new EditorState(withEquation, new DocumentPosition(2, 0)),
                editor.extendLeft(new EditorState(withEquation, new DocumentPosition(2, 0))));
    }

    @Test
    void selectionRangeNormalizesAnchorAndActiveWhileStatePreservesDirection() {
        var document = document(paragraph(text("abcdef")));
        var state = new EditorState(document, new DocumentPosition(0, 5), new DocumentPosition(0, 2));

        assertTrue(state.hasSelection());
        assertEquals(new DocumentPosition(0, 5), state.anchor());
        assertEquals(new DocumentPosition(0, 2), state.active());
        assertEquals(new DocumentRange(new DocumentPosition(0, 2), new DocumentPosition(0, 5)), state.selectionRange());
    }

    @Test
    void shiftMovementExtendsShrinksAndCrossesAnchor() {
        var document = document(paragraph(text("abc")));
        var state = new EditorState(document, new DocumentPosition(0, 1));

        var extended = editor.extendRight(state);
        var shrunk = editor.extendLeft(extended);
        var crossed = editor.extendLeft(shrunk);

        assertEquals(new DocumentPosition(0, 1), extended.anchor());
        assertEquals(new DocumentPosition(0, 2), extended.active());
        assertEquals(new DocumentRange(new DocumentPosition(0, 1), new DocumentPosition(0, 2)), extended.selectionRange());
        assertEquals(new DocumentPosition(0, 1), shrunk.active());
        assertTrue(!shrunk.hasSelection());
        assertEquals(new DocumentPosition(0, 0), crossed.active());
        assertEquals(new DocumentRange(new DocumentPosition(0, 0), new DocumentPosition(0, 1)), crossed.selectionRange());
    }

    @Test
    void plainArrowCollapsesExistingSelection() {
        var document = document(paragraph(text("abcdef")));
        var forward = new EditorState(document, new DocumentPosition(0, 2), new DocumentPosition(0, 5));
        var backward = new EditorState(document, new DocumentPosition(0, 5), new DocumentPosition(0, 2));

        assertEquals(new DocumentPosition(0, 2), editor.moveLeft(forward).caret());
        assertEquals(new DocumentPosition(0, 5), editor.moveRight(backward).caret());
    }

    @Test
    void typingReplacesSelectionAndCollapsesAfterReplacement() {
        var document = document(paragraph(text("abcdef")));
        var state = new EditorState(document, new DocumentPosition(0, 2), new DocumentPosition(0, 5));

        var result = editor.insertText(state, "X");

        assertEquals("abXf", paragraphText(result.document()));
        assertEquals(new DocumentPosition(0, 3), result.caret());
        assertTrue(!result.editorState().hasSelection());
    }

    @Test
    void backspaceAndDeleteReplaceSelectionWithNothing() {
        var document = document(paragraph(text("abcdef")));
        var state = new EditorState(document, new DocumentPosition(0, 5), new DocumentPosition(0, 2));

        var backspace = editor.deleteBackward(state);
        var delete = editor.deleteForward(state);

        assertEquals("abf", paragraphText(backspace.document()));
        assertEquals(new DocumentPosition(0, 2), backspace.caret());
        assertEquals("abf", paragraphText(delete.document()));
        assertEquals(new DocumentPosition(0, 2), delete.caret());
    }

    @Test
    void selectionReplacementUsesFormattingAffinityAtRangeStart() {
        var document = markedBoundaryDocument();
        var state = new EditorState(document, new DocumentPosition(0, 12), new DocumentPosition(0, 14));

        var result = editor.insertText(state, "X");

        assertEquals(Set.of(TextMark.BOLD), textAt(result.document(), 2).marks());
    }

    @Test
    void oldDocumentRemainsUnchangedAfterEdit() {
        var document = document(paragraph(text("abc")));

        var result = editor.replaceRange(document, DocumentRange.caret(new DocumentPosition(0, 1)), "x");

        assertEquals("abc", paragraphText(document));
        assertEquals("axbc", paragraphText(result.document()));
        assertNotEquals(document, result.document());
    }

    @Test
    void insertionInsideMarkedTextUsesContainingMarks() {
        var document = markedBoundaryDocument();

        var result = editor.replaceRange(document, DocumentRange.caret(new DocumentPosition(0, 12)), "X");

        assertEquals(Set.of(TextMark.BOLD), textAt(result.document(), 2).marks());
    }

    @Test
    void boundaryInsertionBeforeBoldUsesPreviousMarks() {
        var document = markedBoundaryDocument();

        var result = editor.replaceRange(document, DocumentRange.caret(new DocumentPosition(0, 9)), "X");

        assertEquals(Set.of(), textAt(result.document(), 1).marks());
    }

    @Test
    void boundaryInsertionAfterBoldUsesBoldMarks() {
        var document = markedBoundaryDocument();

        var result = editor.replaceRange(document, DocumentRange.caret(new DocumentPosition(0, 16)), "X");

        assertEquals(Set.of(TextMark.BOLD), textAt(result.document(), 2).marks());
    }

    @Test
    void paragraphStartInsertionUsesFollowingMarks() {
        var document = document(paragraph(text("bold", TextMark.BOLD)));

        var result = editor.replaceRange(document, DocumentRange.caret(new DocumentPosition(0, 0)), "X");

        assertEquals(Set.of(TextMark.BOLD), textAt(result.document(), 0).marks());
    }

    @Test
    void editsDoNotMergeAdjacentTextNodes() {
        var document = document(paragraph(text("abc"), text("def")));

        var result = editor.replaceRange(document, DocumentRange.caret(new DocumentPosition(0, 3)), "X");

        assertEquals(3, paragraph(result.document()).content().nodes().size());
    }

    @Test
    void rejectsEquationBlockEditing() {
        var document = document(new EquationBlock(new MathIdentifier("x")));

        assertThrows(IllegalArgumentException.class, () -> editor.initialState(document, 0));
    }

    @Test
    void rejectsUnsupportedInlineNodeEditing() {
        var document = document(new Paragraph(new InlineContent(List.of(new UnsupportedInlineNode()))));

        assertThrows(IllegalArgumentException.class, () -> editor.initialState(document, 0));
    }

    @Test
    void appliesBoldToPlainSelectionBySplittingText() {
        var document = document(paragraph(text("The velocity changes")));
        var state = new EditorState(document, new DocumentPosition(0, 4), new DocumentPosition(0, 12));

        var result = editor.toggleMark(state, TextMark.BOLD);

        assertEquals(List.of(
                text("The "),
                text("velocity", TextMark.BOLD),
                text(" changes")), paragraph(result.document()).content().nodes());
        assertEquals(state.anchor(), result.anchor());
        assertEquals(state.active(), result.active());
    }

    @Test
    void removesBoldFromFullyBoldSelection() {
        var document = document(paragraph(text("velocity", TextMark.BOLD)));
        var state = new EditorState(document, new DocumentPosition(0, 0), new DocumentPosition(0, 8));

        var result = editor.toggleMark(state, TextMark.BOLD);

        assertEquals(List.of(text("velocity")), paragraph(result.document()).content().nodes());
    }

    @Test
    void mixedBoldSelectionTogglesEntireSelectionOnAcrossTextNodes() {
        var document = document(paragraph(
                text("velocity "),
                text("changes", TextMark.BOLD),
                text(" quickly", TextMark.ITALIC)));
        var state = new EditorState(document, new DocumentPosition(0, 0), new DocumentPosition(0, 24));

        var result = editor.toggleMark(state, TextMark.BOLD);

        assertEquals(Set.of(TextMark.BOLD), textAt(result.document(), 0).marks());
        assertEquals(Set.of(TextMark.BOLD), textAt(result.document(), 1).marks());
        assertEquals(Set.of(TextMark.BOLD, TextMark.ITALIC), textAt(result.document(), 2).marks());
    }

    @Test
    void italicTogglePreservesUnrelatedBoldMark() {
        var document = document(paragraph(text("velocity", TextMark.BOLD)));
        var state = new EditorState(document, new DocumentPosition(0, 0), new DocumentPosition(0, 8));

        var result = editor.toggleMark(state, TextMark.ITALIC);

        assertEquals(Set.of(TextMark.BOLD, TextMark.ITALIC), textAt(result.document(), 0).marks());
    }

    @Test
    void formattingSupportsFirstLastEntireAndBackwardSelections() {
        var document = document(paragraph(text("abc")));
        var first = editor.toggleMark(new EditorState(document, new DocumentPosition(0, 0), new DocumentPosition(0, 1)), TextMark.BOLD);
        var last = editor.toggleMark(new EditorState(document, new DocumentPosition(0, 2), new DocumentPosition(0, 3)), TextMark.BOLD);
        var backward = editor.toggleMark(new EditorState(document, new DocumentPosition(0, 3), new DocumentPosition(0, 0)), TextMark.BOLD);

        assertEquals(text("a", TextMark.BOLD), textAt(first.document(), 0));
        assertEquals(text("c", TextMark.BOLD), textAt(last.document(), 1));
        assertEquals(List.of(text("abc", TextMark.BOLD)), paragraph(backward.document()).content().nodes());
        assertEquals(new DocumentPosition(0, 3), backward.anchor());
        assertEquals(new DocumentPosition(0, 0), backward.active());
    }

    @Test
    void formattingUsesUserCharacterBoundariesForUnicode() {
        var document = document(paragraph(text("café Δx θ λ e\u0301")));
        var state = new EditorState(document, new DocumentPosition(0, 0), new DocumentPosition(0, 13));

        var result = editor.toggleMark(state, TextMark.BOLD);

        assertEquals("café Δx θ λ e\u0301", paragraphText(result.document()));
        assertEquals(Set.of(TextMark.BOLD), textAt(result.document(), 0).marks());
    }

    @Test
    void formattingDoesNotCreateEmptyTextNodesOrNormalizeAdjacentEqualMarks() {
        var document = document(paragraph(
                text("a"),
                text("b", TextMark.BOLD),
                text("c")));
        var state = new EditorState(document, new DocumentPosition(0, 1), new DocumentPosition(0, 2));

        var result = editor.toggleMark(state, TextMark.BOLD);

        assertEquals(List.of(text("a"), text("b"), text("c")), paragraph(result.document()).content().nodes());
        assertTrue(paragraph(result.document()).content().nodes().stream()
                .map(Text.class::cast)
                .noneMatch(text -> text.content().isEmpty()));
    }

    @Test
    void formattingQueriesSelectionAndCaretStates() {
        var document = document(paragraph(
                text("plain "),
                text("bold", TextMark.BOLD)));

        assertEquals(FormattingState.OFF, editor.formattingState(new EditorState(document, new DocumentPosition(0, 0), new DocumentPosition(0, 5)), TextMark.BOLD));
        assertEquals(FormattingState.ON, editor.formattingState(new EditorState(document, new DocumentPosition(0, 6), new DocumentPosition(0, 10)), TextMark.BOLD));
        assertEquals(FormattingState.MIXED, editor.formattingState(new EditorState(document, new DocumentPosition(0, 0), new DocumentPosition(0, 10)), TextMark.BOLD));
        assertEquals(FormattingState.ON, editor.formattingState(new EditorState(document, new DocumentPosition(0, 10)), TextMark.BOLD));
        assertEquals(FormattingState.OFF, editor.formattingState(new EditorState(document, new DocumentPosition(0, 0)), TextMark.ITALIC));
        assertFalse(editor.formattingState(new EditorState(document, new DocumentPosition(0, 0)), TextMark.BOLD) == FormattingState.MIXED);
    }

    @Test
    void multiBlockInlineFormattingQueriesSelectedCharactersOnly() {
        var allBold = document(
                paragraph(text("abc", TextMark.BOLD)),
                new Paragraph(new InlineContent(List.of())),
                paragraph(text("def", TextMark.BOLD)));
        var allPlain = document(paragraph(text("abc")), paragraph(text("def")));
        var mixed = document(paragraph(text("abc", TextMark.BOLD)), paragraph(text("def")));

        assertEquals(FormattingState.ON, editor.formattingState(
                new EditorState(allBold, new DocumentPosition(0, 1), new DocumentPosition(2, 2)),
                TextMark.BOLD));
        assertEquals(FormattingState.OFF, editor.formattingState(
                new EditorState(allPlain, new DocumentPosition(0, 1), new DocumentPosition(1, 2)),
                TextMark.BOLD));
        assertEquals(FormattingState.MIXED, editor.formattingState(
                new EditorState(mixed, new DocumentPosition(0, 1), new DocumentPosition(1, 2)),
                TextMark.BOLD));
    }

    @Test
    void partialEndpointInlineFormattingQueryIgnoresUnselectedText() {
        var document = document(
                paragraph(text("a"), text("bc", TextMark.BOLD)),
                paragraph(text("de", TextMark.BOLD), text("f")));

        assertEquals(FormattingState.ON, editor.formattingState(
                new EditorState(document, new DocumentPosition(0, 1), new DocumentPosition(1, 2)),
                TextMark.BOLD));
    }

    @Test
    void boundaryOnlyInlineFormattingIsNotApplicable() {
        var document = document(paragraph(text("abc", TextMark.BOLD)), paragraph(text("def", TextMark.BOLD)));
        var state = new EditorState(document, new DocumentPosition(0, 3), new DocumentPosition(1, 0));

        assertEquals(FormattingState.NOT_APPLICABLE, editor.formattingState(state, TextMark.BOLD));
        assertFalse(editor.supportsInlineFormatting(state));
    }

    @Test
    void multiBlockBoldAppliesRemovesAndPreservesSelectionDirection() {
        var document = document(
                paragraph(text("abc")),
                paragraph(text("def", TextMark.BOLD)),
                paragraph(text("ghi")));
        var state = new EditorState(document, new DocumentPosition(2, 2), new DocumentPosition(0, 1));

        var added = editor.toggleMark(state, TextMark.BOLD);
        var removed = editor.toggleMark(added.editorState(), TextMark.BOLD);

        assertEquals(List.of(text("a"), text("bc", TextMark.BOLD)), contentAt(added.document(), 0).nodes());
        assertEquals(List.of(text("def", TextMark.BOLD)), contentAt(added.document(), 1).nodes());
        assertEquals(List.of(text("gh", TextMark.BOLD), text("i")), contentAt(added.document(), 2).nodes());
        assertEquals(state.anchor(), added.anchor());
        assertEquals(state.active(), added.active());
        assertEquals(List.of(text("a"), text("bc")), contentAt(removed.document(), 0).nodes());
        assertEquals(List.of(text("def")), contentAt(removed.document(), 1).nodes());
        assertEquals(List.of(text("gh"), text("i")), contentAt(removed.document(), 2).nodes());
    }

    @Test
    void multiBlockItalicPreservesBoldAndUsesUnicodeBoundaries() {
        var document = document(
                paragraph(text("café", TextMark.BOLD)),
                paragraph(text("Δx θ λ e\u0301", TextMark.BOLD)));
        var state = new EditorState(document, new DocumentPosition(0, 3), new DocumentPosition(1, 4));

        var result = editor.toggleMark(state, TextMark.ITALIC);

        assertEquals(List.of(
                text("caf", TextMark.BOLD),
                text("é", TextMark.BOLD, TextMark.ITALIC)), contentAt(result.document(), 0).nodes());
        assertEquals(List.of(
                text("Δx θ", TextMark.BOLD, TextMark.ITALIC),
                text(" λ e\u0301", TextMark.BOLD)), contentAt(result.document(), 1).nodes());
    }

    @Test
    void multiBlockInlineFormattingRejectsEquationBarrier() {
        var document = document(paragraph(text("abc")), new EquationBlock(new MathIdentifier("x")), paragraph(text("def")));
        var state = new EditorState(document, new DocumentPosition(0, 1), new DocumentPosition(2, 1));

        assertThrows(IllegalArgumentException.class, () -> editor.formattingState(state, TextMark.BOLD));
        assertThrows(IllegalArgumentException.class, () -> editor.toggleMark(state, TextMark.BOLD));
    }

    @Test
    void explicitTypingMarksOverrideInferredCaretMarks() {
        var document = document(paragraph(text("bold", TextMark.BOLD)));
        var state = new EditorState(document, new DocumentPosition(0, 4)).withExplicitTypingMarks(Set.of(TextMark.ITALIC));

        assertEquals(Set.of(TextMark.ITALIC), editor.marksForInsertion(state));
        assertEquals(FormattingState.OFF, editor.formattingState(state, TextMark.BOLD));
        assertEquals(FormattingState.ON, editor.formattingState(state, TextMark.ITALIC));
    }

    @Test
    void convertsParagraphToHeadingAndBackPreservingInlineContent() {
        var content = new InlineContent(List.of(
                text("Re", TextMark.BOLD),
                text("sults", TextMark.ITALIC)));
        var document = document(new Paragraph(content));
        var state = new EditorState(document, new DocumentPosition(0, 1), new DocumentPosition(0, 4));

        var heading = editor.setBlockStyle(state, BlockStyle.heading(2));

        var headingBlock = assertHeading(heading.document(), 2);
        assertEquals(content, headingBlock.content());
        assertEquals(state.anchor(), heading.anchor());
        assertEquals(state.active(), heading.active());
        assertEquals(document, state.document());

        var paragraph = editor.setBlockStyle(heading.editorState(), BlockStyle.paragraph());
        assertEquals(content, paragraph(paragraph.document()).content());
    }

    @Test
    void changesHeadingLevelOnlyAndPreservesExplicitTypingMarks() {
        var state = new EditorState(
                document(heading(1, text("Results"))),
                new DocumentPosition(0, 0),
                new DocumentPosition(0, 7),
                java.util.Optional.of(Set.of(TextMark.BOLD)));

        var result = editor.setBlockStyle(state, BlockStyle.heading(6));

        assertEquals(6, assertHeading(result.document(), 6).level());
        assertEquals(state.anchor(), result.anchor());
        assertEquals(state.active(), result.active());
        assertEquals(Set.of(TextMark.BOLD), result.explicitTypingMarks().orElseThrow());
    }

    @Test
    void blockStyleNoOpDoesNotChangeDocument() {
        var state = new EditorState(document(heading(2, text("Results"))), new DocumentPosition(0, 0));

        var result = editor.setBlockStyle(state, BlockStyle.heading(2));

        assertFalse(result.changed());
        assertEquals(state.document(), result.document());
    }

    @Test
    void multiBlockBlockStyleQueriesSingleMixedBoundaryAndEmptyBlocks() {
        var paragraphs = document(paragraph(text("A")), new Paragraph(new InlineContent(List.of())), paragraph(text("B")));
        var headings = document(heading(3, text("A")), heading(3, text("B")));
        var mixed = document(paragraph(text("abc")), heading(2, text("def")));

        assertEquals(BlockStyleSelectionState.single(BlockStyle.paragraph()), editor.blockStyleSelectionState(
                new EditorState(paragraphs, new DocumentPosition(0, 1), new DocumentPosition(2, 0))));
        assertEquals(BlockStyleSelectionState.single(BlockStyle.heading(3)), editor.blockStyleSelectionState(
                new EditorState(headings, new DocumentPosition(0, 1), new DocumentPosition(1, 1))));
        assertEquals(BlockStyleSelectionState.mixed(), editor.blockStyleSelectionState(
                new EditorState(mixed, new DocumentPosition(0, 3), new DocumentPosition(1, 0))));
    }

    @Test
    void multiBlockBlockStyleApplicationPreservesContentMarksAndSelection() {
        var document = document(
                paragraph(text("A", TextMark.BOLD)),
                heading(2, text("B", TextMark.ITALIC)),
                paragraph(text("C")));
        var state = new EditorState(document, new DocumentPosition(2, 1), new DocumentPosition(0, 0));

        var result = editor.setBlockStyle(state, BlockStyle.heading(4));

        assertEquals(BlockStyle.heading(4), EditableInlineBlock.styleOf(result.document().blocks().get(0)));
        assertEquals(BlockStyle.heading(4), EditableInlineBlock.styleOf(result.document().blocks().get(1)));
        assertEquals(BlockStyle.heading(4), EditableInlineBlock.styleOf(result.document().blocks().get(2)));
        assertEquals(List.of(text("A", TextMark.BOLD)), contentAt(result.document(), 0).nodes());
        assertEquals(List.of(text("B", TextMark.ITALIC)), contentAt(result.document(), 1).nodes());
        assertEquals(state.anchor(), result.anchor());
        assertEquals(state.active(), result.active());
    }

    @Test
    void boundaryOnlyBlockStyleApplicationTargetsBothBlocks() {
        var document = document(paragraph(text("abc")), heading(2, text("def")));
        var state = new EditorState(document, new DocumentPosition(0, 3), new DocumentPosition(1, 0));

        var result = editor.setBlockStyle(state, BlockStyle.paragraph());

        assertEquals(BlockStyle.paragraph(), EditableInlineBlock.styleOf(result.document().blocks().get(0)));
        assertEquals(BlockStyle.paragraph(), EditableInlineBlock.styleOf(result.document().blocks().get(1)));
        assertEquals(state.anchor(), result.anchor());
        assertEquals(state.active(), result.active());
    }

    @Test
    void multiBlockBlockStyleNoOpAndEquationBarrier() {
        var noOp = new EditorState(
                document(heading(2, text("A")), heading(2, text("B"))),
                new DocumentPosition(0, 0),
                new DocumentPosition(1, 1));
        var blocked = new EditorState(
                document(paragraph(text("A")), new EquationBlock(new MathIdentifier("x")), paragraph(text("B"))),
                new DocumentPosition(0, 0),
                new DocumentPosition(2, 1));

        assertFalse(editor.setBlockStyle(noOp, BlockStyle.heading(2)).changed());
        assertThrows(IllegalArgumentException.class, () -> editor.blockStyleSelectionState(blocked));
        assertThrows(IllegalArgumentException.class, () -> editor.setBlockStyle(blocked, BlockStyle.paragraph()));
    }

    @Test
    void insertsEquationAtParagraphStartMiddleEndAndBeforeFollowingText() {
        var start = editor.insertEmptyEquation(new EditorState(document(paragraph(text("abc"))), new DocumentPosition(0, 0)));
        var middle = editor.insertEmptyEquation(new EditorState(document(paragraph(text("abcd"))), new DocumentPosition(0, 2)));
        var finalEnd = editor.insertEmptyEquation(new EditorState(document(paragraph(text("abc"))), new DocumentPosition(0, 3)));
        var followedEnd = editor.insertEmptyEquation(new EditorState(document(paragraph(text("abc")), paragraph(text("def"))), new DocumentPosition(0, 3)));

        assertEquals(List.of(EquationBlock.class, Paragraph.class), blockClasses(start.document()));
        assertEquals(new BlockSelection(0), start.editorState().selection());
        assertEquals(List.of("abc"), blockTexts(start.document()));
        assertEquals(List.of(Paragraph.class, EquationBlock.class, Paragraph.class), blockClasses(middle.document()));
        assertEquals(List.of("ab", "cd"), blockTexts(middle.document()));
        assertEquals(new BlockSelection(1), middle.editorState().selection());
        assertEquals(List.of(Paragraph.class, EquationBlock.class, Paragraph.class), blockClasses(finalEnd.document()));
        assertTrue(contentAt(finalEnd.document(), 2).nodes().isEmpty());
        assertEquals(List.of(Paragraph.class, EquationBlock.class, Paragraph.class), blockClasses(followedEnd.document()));
        assertEquals(List.of("abc", "def"), blockTexts(followedEnd.document()));
    }

    @Test
    void insertsEquationReplacingEmptyParagraphOrHeadingWithTrailingAuthoringParagraph() {
        var emptyParagraph = editor.insertEmptyEquation(new EditorState(document(new Paragraph(new InlineContent(List.of()))), new DocumentPosition(0, 0)));
        var emptyHeading = editor.insertEmptyEquation(new EditorState(document(new Heading(2, new InlineContent(List.of()))), new DocumentPosition(0, 0)));

        assertEquals(List.of(EquationBlock.class, Paragraph.class), blockClasses(emptyParagraph.document()));
        assertEquals(List.of(EquationBlock.class, Paragraph.class), blockClasses(emptyHeading.document()));
        assertTrue(contentAt(emptyParagraph.document(), 1).nodes().isEmpty());
        assertTrue(contentAt(emptyHeading.document(), 1).nodes().isEmpty());
        assertEquals(new BlockSelection(0), emptyParagraph.editorState().selection());
        assertEquals(new MathSequence(List.of()), ((EquationBlock) emptyParagraph.document().blocks().get(0)).expression());
    }

    @Test
    void insertsEquationAroundHeadingUsingBodySemantics() {
        var start = editor.insertEmptyEquation(new EditorState(document(heading(2, text("Results"))), new DocumentPosition(0, 0)));
        var middle = editor.insertEmptyEquation(new EditorState(document(heading(2, text("Results", TextMark.BOLD))), new DocumentPosition(0, 3)));
        var end = editor.insertEmptyEquation(new EditorState(document(heading(2, text("Results"))), new DocumentPosition(0, 7)));

        assertEquals(List.of(EquationBlock.class, Heading.class), blockClasses(start.document()));
        assertEquals(List.of(Heading.class, EquationBlock.class, Paragraph.class), blockClasses(middle.document()));
        assertEquals("Res", blockText(middle.document(), 0));
        assertEquals("ults", blockText(middle.document(), 2));
        assertEquals(Set.of(TextMark.BOLD), ((Text) contentAt(middle.document(), 2).nodes().get(0)).marks());
        assertEquals(2, ((Heading) middle.document().blocks().get(0)).level());
        assertEquals(List.of(Heading.class, EquationBlock.class, Paragraph.class), blockClasses(end.document()));
        assertTrue(contentAt(end.document(), 2).nodes().isEmpty());
    }

    @Test
    void insertEquationUsesUnicodeBoundariesAndPreservesInlineSegmentation() {
        var document = document(paragraph(
                text("café ", TextMark.BOLD),
                text("Δx θ λ e\u0301", TextMark.BOLD)));

        var result = editor.insertEmptyEquation(new EditorState(document, new DocumentPosition(0, 7)));

        assertEquals(List.of(text("café ", TextMark.BOLD), text("Δx", TextMark.BOLD)), contentAt(result.document(), 0).nodes());
        assertEquals(List.of(text(" θ λ e\u0301", TextMark.BOLD)), contentAt(result.document(), 2).nodes());
    }

    @Test
    void insertEquationReplacesSameParagraphSelectionAndSelectsObject() {
        var result = editor.insertEmptyEquation(new EditorState(
                document(paragraph(text("abcDEFghi"))),
                new DocumentPosition(0, 3),
                new DocumentPosition(0, 6)));

        assertEquals(List.of(Paragraph.class, EquationBlock.class, Paragraph.class), blockClasses(result.document()));
        assertEquals(List.of("abc", "ghi"), blockTexts(result.document()));
        assertEquals(new BlockSelection(1), result.editorState().selection());
    }

    @Test
    void insertEquationReplacesSameParagraphSelectionBackward() {
        var result = editor.insertEmptyEquation(new EditorState(
                document(paragraph(text("abcDEFghi"))),
                new DocumentPosition(0, 6),
                new DocumentPosition(0, 3)));

        assertEquals(List.of(Paragraph.class, EquationBlock.class, Paragraph.class), blockClasses(result.document()));
        assertEquals(List.of("abc", "ghi"), blockTexts(result.document()));
        assertEquals(new BlockSelection(1), result.editorState().selection());
    }

    @Test
    void insertEquationReplacesParagraphPrefixSuffixAndWholeSelection() {
        var prefix = editor.insertEmptyEquation(new EditorState(
                document(paragraph(text("abcdef"))),
                new DocumentPosition(0, 0),
                new DocumentPosition(0, 3)));
        var suffix = editor.insertEmptyEquation(new EditorState(
                document(paragraph(text("abcdef"))),
                new DocumentPosition(0, 3),
                new DocumentPosition(0, 6)));
        var whole = editor.insertEmptyEquation(new EditorState(
                document(paragraph(text("abcdef"))),
                new DocumentPosition(0, 0),
                new DocumentPosition(0, 6)));

        assertEquals(List.of(EquationBlock.class, Paragraph.class), blockClasses(prefix.document()));
        assertEquals("def", blockText(prefix.document(), 1));
        assertEquals(List.of(Paragraph.class, EquationBlock.class, Paragraph.class), blockClasses(suffix.document()));
        assertEquals("abc", blockText(suffix.document(), 0));
        assertTrue(contentAt(suffix.document(), 2).nodes().isEmpty());
        assertEquals(List.of(EquationBlock.class, Paragraph.class), blockClasses(whole.document()));
        assertTrue(contentAt(whole.document(), 1).nodes().isEmpty());
    }

    @Test
    void insertEquationReplacesHeadingSelectionWithBodySuffixSemantics() {
        var middle = editor.insertEmptyEquation(new EditorState(
                document(heading(2, text("Results"))),
                new DocumentPosition(0, 3),
                new DocumentPosition(0, 5)));
        var prefix = editor.insertEmptyEquation(new EditorState(
                document(heading(2, text("Results"))),
                new DocumentPosition(0, 0),
                new DocumentPosition(0, 3)));
        var suffix = editor.insertEmptyEquation(new EditorState(
                document(heading(2, text("Results"))),
                new DocumentPosition(0, 3),
                new DocumentPosition(0, 7)));
        var whole = editor.insertEmptyEquation(new EditorState(
                document(heading(2, text("Results"))),
                new DocumentPosition(0, 0),
                new DocumentPosition(0, 7)));

        assertEquals(List.of(Heading.class, EquationBlock.class, Paragraph.class), blockClasses(middle.document()));
        assertEquals("Res", blockText(middle.document(), 0));
        assertEquals("ts", blockText(middle.document(), 2));
        assertEquals(2, ((Heading) middle.document().blocks().get(0)).level());
        assertEquals(List.of(EquationBlock.class, Paragraph.class), blockClasses(prefix.document()));
        assertEquals("ults", blockText(prefix.document(), 1));
        assertEquals(List.of(Heading.class, EquationBlock.class, Paragraph.class), blockClasses(suffix.document()));
        assertEquals("Res", blockText(suffix.document(), 0));
        assertTrue(contentAt(suffix.document(), 2).nodes().isEmpty());
        assertEquals(List.of(EquationBlock.class, Paragraph.class), blockClasses(whole.document()));
        assertTrue(contentAt(whole.document(), 1).nodes().isEmpty());
    }

    @Test
    void insertEquationReplacesMultiBlockTextSelection() {
        var result = editor.insertEmptyEquation(new EditorState(
                document(paragraph(text("AAA")), paragraph(text("BBB")), paragraph(text("CCC"))),
                new DocumentPosition(0, 2),
                new DocumentPosition(2, 1)));

        assertEquals(List.of(Paragraph.class, EquationBlock.class, Paragraph.class), blockClasses(result.document()));
        assertEquals("AA", blockText(result.document(), 0));
        assertEquals("CC", blockText(result.document(), 2));
        assertEquals(new BlockSelection(1), result.editorState().selection());
    }

    @Test
    void insertEquationUsesLeftOwnershipAndParagraphRightSuffixAcrossBlockStyles() {
        var headingToParagraph = editor.insertEmptyEquation(new EditorState(
                document(heading(2, text("Results")), paragraph(text("Body"))),
                new DocumentPosition(0, 3),
                new DocumentPosition(1, 2)));
        var paragraphToHeading = editor.insertEmptyEquation(new EditorState(
                document(paragraph(text("Intro")), heading(3, text("Results"))),
                new DocumentPosition(0, 3),
                new DocumentPosition(1, 3)));

        assertEquals(List.of(Heading.class, EquationBlock.class, Paragraph.class), blockClasses(headingToParagraph.document()));
        assertEquals("Res", blockText(headingToParagraph.document(), 0));
        assertEquals("dy", blockText(headingToParagraph.document(), 2));
        assertEquals(2, ((Heading) headingToParagraph.document().blocks().get(0)).level());
        assertEquals(List.of(Paragraph.class, EquationBlock.class, Paragraph.class), blockClasses(paragraphToHeading.document()));
        assertEquals("Int", blockText(paragraphToHeading.document(), 0));
        assertEquals("ults", blockText(paragraphToHeading.document(), 2));
    }

    @Test
    void insertEquationReplacesBoundaryOnlySelectionWithoutChangingAdjacentBlocks() {
        var paragraphs = editor.insertEmptyEquation(new EditorState(
                document(paragraph(text("A")), paragraph(text("B"))),
                new DocumentPosition(0, 1),
                new DocumentPosition(1, 0)));
        var headingParagraph = editor.insertEmptyEquation(new EditorState(
                document(heading(2, text("Title")), paragraph(text("Body"))),
                new DocumentPosition(0, 5),
                new DocumentPosition(1, 0)));
        var paragraphHeading = editor.insertEmptyEquation(new EditorState(
                document(paragraph(text("Intro")), heading(3, text("Results"))),
                new DocumentPosition(0, 5),
                new DocumentPosition(1, 0)));
        var emptyBoundary = editor.insertEmptyEquation(new EditorState(
                document(paragraph(), paragraph()),
                new DocumentPosition(0, 0),
                new DocumentPosition(1, 0)));

        assertEquals(List.of(Paragraph.class, EquationBlock.class, Paragraph.class), blockClasses(paragraphs.document()));
        assertEquals(List.of("A", "B"), blockTexts(paragraphs.document()));
        assertEquals(new BlockSelection(1), paragraphs.editorState().selection());
        assertEquals(List.of(Heading.class, EquationBlock.class, Paragraph.class), blockClasses(headingParagraph.document()));
        assertEquals("Title", blockText(headingParagraph.document(), 0));
        assertEquals("Body", blockText(headingParagraph.document(), 2));
        assertEquals(List.of(Paragraph.class, EquationBlock.class, Heading.class), blockClasses(paragraphHeading.document()));
        assertEquals("Results", blockText(paragraphHeading.document(), 2));
        assertEquals(List.of(Paragraph.class, EquationBlock.class, Paragraph.class), blockClasses(emptyBoundary.document()));
        assertTrue(contentAt(emptyBoundary.document(), 0).nodes().isEmpty());
        assertTrue(contentAt(emptyBoundary.document(), 2).nodes().isEmpty());
    }

    @Test
    void insertEquationHandlesWholeMultiBlockSelectionsAndUntouchedFollowingBlocks() {
        var whole = editor.insertEmptyEquation(new EditorState(
                document(paragraph(text("AAA")), paragraph(text("BBB"))),
                new DocumentPosition(0, 0),
                new DocumentPosition(1, 3)));
        var followedByHeading = editor.insertEmptyEquation(new EditorState(
                document(paragraph(text("AAA")), paragraph(text("BBB")), heading(2, text("Keep"))),
                new DocumentPosition(0, 0),
                new DocumentPosition(1, 3)));
        var followedByEquation = editor.insertEmptyEquation(new EditorState(
                document(paragraph(text("AAA")), paragraph(text("BBB")), new EquationBlock(new MathIdentifier("x"))),
                new DocumentPosition(0, 0),
                new DocumentPosition(1, 3)));

        assertEquals(List.of(EquationBlock.class, Paragraph.class), blockClasses(whole.document()));
        assertTrue(contentAt(whole.document(), 1).nodes().isEmpty());
        assertEquals(List.of(EquationBlock.class, Heading.class), blockClasses(followedByHeading.document()));
        assertEquals("Keep", blockText(followedByHeading.document(), 1));
        assertEquals(List.of(EquationBlock.class, EquationBlock.class), blockClasses(followedByEquation.document()));
    }

    @Test
    void insertEquationPreservesExistingObjectsAroundSelectionAndInsertedIndex() {
        var before = new EquationBlock(new MathIdentifier("a"));
        var after = new EquationBlock(new MathIdentifier("b"));
        var result = editor.insertEmptyEquation(new EditorState(
                document(before, paragraph(text("AAA")), paragraph(text("BBB")), after),
                new DocumentPosition(1, 1),
                new DocumentPosition(2, 2)));

        assertEquals(List.of(EquationBlock.class, Paragraph.class, EquationBlock.class, Paragraph.class, EquationBlock.class), blockClasses(result.document()));
        assertEquals(before, result.document().blocks().get(0));
        assertEquals(after, result.document().blocks().get(4));
        assertEquals(new BlockSelection(2), result.editorState().selection());
    }

    @Test
    void insertEquationTextReplacementPreservesSurvivingMarksAndSegmentation() {
        var document = document(paragraph(
                text("ab", TextMark.BOLD),
                text("cd", TextMark.ITALIC),
                text("ef", TextMark.BOLD, TextMark.ITALIC)));

        var result = editor.insertEmptyEquation(new EditorState(document, new DocumentPosition(0, 1), new DocumentPosition(0, 5)));

        assertEquals(List.of(Paragraph.class, EquationBlock.class, Paragraph.class), blockClasses(result.document()));
        assertEquals(List.of(text("a", TextMark.BOLD)), contentAt(result.document(), 0).nodes());
        assertEquals(List.of(text("f", TextMark.BOLD, TextMark.ITALIC)), contentAt(result.document(), 2).nodes());
    }

    @Test
    void insertEquationTextReplacementUsesUnicodeBoundaries() {
        var document = document(paragraph(text("café Δx θ λ e\u0301", TextMark.BOLD)));

        var result = editor.insertEmptyEquation(new EditorState(document, new DocumentPosition(0, 5), new DocumentPosition(0, 10)));

        assertEquals(List.of(Paragraph.class, EquationBlock.class, Paragraph.class), blockClasses(result.document()));
        assertEquals(List.of(text("café ", TextMark.BOLD)), contentAt(result.document(), 0).nodes());
        assertEquals(List.of(text("λ e\u0301", TextMark.BOLD)), contentAt(result.document(), 2).nodes());
    }

    @Test
    void insertEquationReplacementRejectsEquationBarrierAndInvalidSelections() {
        var single = new EditorState(document(paragraph(text("abc"))), new DocumentPosition(0, 0), new DocumentPosition(0, 1));
        var collapsed = new EditorState(document(paragraph(text("abc"))), new DocumentPosition(0, 1));
        var blockSelection = new EditorState(document(new EquationBlock(new MathIdentifier("x"))), new BlockSelection(0), java.util.Optional.empty());
        var barrier = new EditorState(
                document(paragraph(text("A")), new EquationBlock(new MathIdentifier("x")), paragraph(text("B"))),
                new DocumentPosition(0, 0),
                new DocumentPosition(2, 1));

        assertTrue(editor.supportsInsertBlock(single));
        assertFalse(editor.replaceSelectionWithBlock(collapsed, new EquationBlock(new MathSequence(List.of()))).changed());
        assertFalse(editor.replaceSelectionWithBlock(blockSelection, new EquationBlock(new MathSequence(List.of()))).changed());
        assertFalse(editor.supportsInsertBlock(barrier));
        assertFalse(editor.insertEmptyEquation(barrier).changed());
        assertThrows(IllegalArgumentException.class, () -> editor.replaceSelectionWithBlock(barrier, new EquationBlock(new MathSequence(List.of()))));
    }

    @Test
    void insertEquationAfterBlockSelectionSelectsInsertedIndexAndFallbacksAtEnd() {
        var document = document(new EquationBlock(new MathSequence(List.of())), paragraph(text("after")));
        var result = editor.insertEmptyEquation(new EditorState(document, new BlockSelection(0), java.util.Optional.empty()));
        var finalResult = editor.insertEmptyEquation(new EditorState(document(new EquationBlock(new MathSequence(List.of()))), new BlockSelection(0), java.util.Optional.empty()));

        assertEquals(List.of(EquationBlock.class, EquationBlock.class, Paragraph.class), blockClasses(result.document()));
        assertEquals(new BlockSelection(1), result.editorState().selection());
        assertEquals(List.of(EquationBlock.class, EquationBlock.class, Paragraph.class), blockClasses(finalResult.document()));
        assertTrue(contentAt(finalResult.document(), 2).nodes().isEmpty());
    }

    @Test
    void blockSelectionNavigationTraversesTextAndConsecutiveObjects() {
        var document = document(paragraph(text("abc")), new EquationBlock(new MathSequence(List.of())), new EquationBlock(new MathSequence(List.of())), paragraph(text("def")));

        var firstObject = editor.moveRight(new EditorState(document, new DocumentPosition(0, 3)));
        var secondObject = editor.moveRight(firstObject.editorState());
        var nextText = editor.moveRight(secondObject.editorState());
        var backToSecond = editor.moveLeft(nextText.editorState());
        var backToFirst = editor.moveLeft(backToSecond.editorState());
        var backToText = editor.moveLeft(backToFirst.editorState());

        assertEquals(new BlockSelection(1), firstObject.editorState().selection());
        assertEquals(new BlockSelection(2), secondObject.editorState().selection());
        assertEquals(new DocumentPosition(3, 0), nextText.caret());
        assertEquals(new BlockSelection(2), backToSecond.editorState().selection());
        assertEquals(new BlockSelection(1), backToFirst.editorState().selection());
        assertEquals(new DocumentPosition(0, 3), backToText.caret());
    }

    @Test
    void shiftArrowAndTextCommandsAreNoOpsForBlockSelection() {
        var state = new EditorState(document(paragraph(text("abc")), new EquationBlock(new MathSequence(List.of()))), new BlockSelection(1), java.util.Optional.empty());

        assertEquals(state, editor.extendLeft(state));
        assertEquals(state, editor.extendRight(state));
        assertFalse(editor.insertText(state, "x").changed());
        assertFalse(editor.insertParagraphBreak(state, Set.of()).changed());
        assertEquals(FormattingState.NOT_APPLICABLE, editor.formattingState(state, TextMark.BOLD));
        assertFalse(editor.supportsInlineFormatting(state));
        assertFalse(editor.supportsBlockStyle(state));
    }

    @Test
    void deleteSelectedObjectChoosesPostDeleteSelectionAndFallback() {
        var between = editor.deleteForward(new EditorState(
                document(paragraph(text("A")), new EquationBlock(new MathSequence(List.of())), paragraph(text("B"))),
                new BlockSelection(1),
                java.util.Optional.empty()));
        var first = editor.deleteForward(new EditorState(
                document(new EquationBlock(new MathSequence(List.of())), paragraph(text("B"))),
                new BlockSelection(0),
                java.util.Optional.empty()));
        var last = editor.deleteBackward(new EditorState(
                document(paragraph(text("A")), new EquationBlock(new MathSequence(List.of()))),
                new BlockSelection(1),
                java.util.Optional.empty()));
        var only = editor.deleteForward(new EditorState(
                document(new EquationBlock(new MathSequence(List.of()))),
                new BlockSelection(0),
                java.util.Optional.empty()));
        var adjacent = editor.deleteForward(new EditorState(
                document(new EquationBlock(new MathSequence(List.of())), new EquationBlock(new MathSequence(List.of())), paragraph(text("B"))),
                new BlockSelection(0),
                java.util.Optional.empty()));

        assertEquals(List.of("A", "B"), blockTexts(between.document()));
        assertEquals(new DocumentPosition(0, 1), between.caret());
        assertEquals(new DocumentPosition(0, 0), first.caret());
        assertEquals(new DocumentPosition(0, 1), last.caret());
        assertEquals(List.of(Paragraph.class), blockClasses(only.document()));
        assertEquals(new DocumentPosition(0, 0), only.caret());
        assertEquals(new BlockSelection(0), adjacent.editorState().selection());
    }

    @Test
    void deleteAndBackspaceAtTextBoundarySelectObjectBeforeDeleting() {
        var document = document(paragraph(text("A")), new EquationBlock(new MathSequence(List.of())), paragraph(text("B")));
        var selectedByDelete = editor.deleteForward(new EditorState(document, new DocumentPosition(0, 1)));
        var deletedByDelete = editor.deleteForward(selectedByDelete.editorState());
        var selectedByBackspace = editor.deleteBackward(new EditorState(document, new DocumentPosition(2, 0)));
        var deletedByBackspace = editor.deleteBackward(selectedByBackspace.editorState());

        assertFalse(selectedByDelete.changed());
        assertEquals(document, selectedByDelete.document());
        assertEquals(new BlockSelection(1), selectedByDelete.editorState().selection());
        assertEquals(List.of("A", "B"), blockTexts(deletedByDelete.document()));
        assertFalse(selectedByBackspace.changed());
        assertEquals(new BlockSelection(1), selectedByBackspace.editorState().selection());
        assertEquals(List.of("A", "B"), blockTexts(deletedByBackspace.document()));
    }

    @Test
    void editsInsideHeadingPreserveHeadingTypeAndLevel() {
        var document = document(heading(2, text("Result")));
        var state = new EditorState(document, new DocumentPosition(0, 6));

        var typed = editor.insertText(state, "s");
        var bolded = editor.toggleMark(new EditorState(typed.document(), new DocumentPosition(0, 0), new DocumentPosition(0, 7)), TextMark.BOLD);
        var deleted = editor.deleteBackward(new EditorState(bolded.document(), new DocumentPosition(0, 7)));

        assertEquals("Results", paragraphText(typed.document()));
        assertEquals(2, assertHeading(typed.document(), 2).level());
        assertEquals(Set.of(TextMark.BOLD), textAt(bolded.document(), 0).marks());
        assertEquals(2, assertHeading(deleted.document(), 2).level());
        assertEquals("Result", paragraphText(deleted.document()));
    }

    private static Document markedBoundaryDocument() {
        return document(paragraph(
                text("velocity "),
                text("changes", TextMark.BOLD),
                text(" quickly")));
    }

    private static Document document(BlockNode... blocks) {
        return new Document(List.of(blocks));
    }

    private static Paragraph paragraph(Text... text) {
        return new Paragraph(new InlineContent(List.of(text).stream()
                .map(InlineNode.class::cast)
                .toList()));
    }

    private static Heading heading(int level, Text... text) {
        return new Heading(level, new InlineContent(List.of(text).stream()
                .map(InlineNode.class::cast)
                .toList()));
    }

    private static Paragraph paragraph(Document document) {
        return (Paragraph) document.blocks().get(0);
    }

    private static Heading assertHeading(Document document, int level) {
        var heading = (Heading) document.blocks().get(0);
        assertEquals(level, heading.level());
        return heading;
    }

    private static Text textAt(Document document, int index) {
        var block = document.blocks().get(0);
        var content = block instanceof Paragraph paragraph ? paragraph.content() : ((Heading) block).content();
        return (Text) content.nodes().get(index);
    }

    private static InlineContent contentAt(Document document, int blockIndex) {
        var block = document.blocks().get(blockIndex);
        return block instanceof Paragraph paragraph ? paragraph.content() : ((Heading) block).content();
    }

    private static Text text(String content, TextMark... marks) {
        return new Text(content, Set.of(marks));
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

    private static final class UnsupportedInlineNode implements InlineNode {
    }
}
