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
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.clipboard.ScholarClipboardService;
import dev.rgcb.scholar.math.MathDelimiter;
import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathGroup;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNamedOperator;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathOperatorRole;
import dev.rgcb.scholar.math.MathRoot;
import dev.rgcb.scholar.math.MathScript;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.MathText;
import dev.rgcb.scholar.math.editor.MathCaretSelection;
import dev.rgcb.scholar.math.editor.MathRangeSelection;
import dev.rgcb.scholar.math.editor.MathPath;
import dev.rgcb.scholar.math.editor.MathSequencePosition;
import dev.rgcb.scholar.math.editor.MathTokenPosition;
import dev.rgcb.scholar.math.editor.FractionDenominator;
import dev.rgcb.scholar.math.editor.FractionNumerator;
import dev.rgcb.scholar.math.editor.GroupContent;
import dev.rgcb.scholar.math.editor.RootRadicand;
import dev.rgcb.scholar.math.editor.ScriptSuperscript;
import dev.rgcb.scholar.math.editor.SemanticMathTokenKind;
import dev.rgcb.scholar.math.editor.SequenceChild;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EditorActionTest {
    @Test
    void undoIsEnabledOnlyWhenHistoryExistsAndExecutes() {
        var session = session("abc");
        var clipboard = new FakeClipboard();
        var context = new EditorActionContext(session, clipboard);
        var undo = BuiltInEditorActions.undo();

        assertFalse(undo.isEnabled(context));
        session.typeText("X");
        assertTrue(undo.isEnabled(context));

        var result = undo.execute(context);

        assertTrue(result.documentChanged());
        assertEquals("abc", paragraphText(session.current().document()));
    }

    @Test
    void redoIsEnabledOnlyAfterUndoAndExecutes() {
        var session = session("abc");
        var context = new EditorActionContext(session, new FakeClipboard());
        var redo = BuiltInEditorActions.redo();

        session.typeText("X");
        session.undo();
        assertTrue(redo.isEnabled(context));

        redo.execute(context);

        assertEquals("abcX", paragraphText(session.current().document()));
    }

    @Test
    void cutAndCopyAreEnabledOnlyWithSelection() {
        var session = session("abcdef");
        var clipboard = new FakeClipboard();
        var context = new EditorActionContext(session, clipboard);
        var cut = BuiltInEditorActions.cut();
        var copy = BuiltInEditorActions.copy();

        assertFalse(cut.isEnabled(context));
        assertFalse(copy.isEnabled(context));

        select(session, 2, 5);
        assertTrue(cut.isEnabled(context));
        assertTrue(copy.isEnabled(context));
    }

    @Test
    void copyWritesClipboardWithoutChangingDocumentOrSelection() {
        var session = session("abcdef");
        var clipboard = new FakeClipboard();
        var copy = BuiltInEditorActions.copy();
        select(session, 2, 5);

        var result = copy.execute(new EditorActionContext(session, clipboard));

        assertFalse(result.documentChanged());
        assertEquals("cde", clipboard.text);
        assertEquals("abcdef", paragraphText(session.current().document()));
        assertTrue(session.current().hasSelection());
    }

    @Test
    void cutWritesClipboardAndChangesDocumentOnce() {
        var session = session("abcdef");
        var clipboard = new FakeClipboard();
        var cut = BuiltInEditorActions.cut();
        select(session, 2, 5);

        var result = cut.execute(new EditorActionContext(session, clipboard));

        assertTrue(result.documentChanged());
        assertEquals("cde", clipboard.text);
        assertEquals("abf", paragraphText(session.current().document()));
        assertTrue(session.canUndo());
    }

    @Test
    void failedTextCutDoesNotDeleteSelection() {
        var session = session("abcdef");
        var clipboard = new FakeClipboard();
        clipboard.writeSucceeds = false;
        select(session, 2, 5);

        var result = BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard));

        assertFalse(result.documentChanged());
        assertEquals("abcdef", paragraphText(session.current().document()));
        assertTrue(session.current().hasSelection());
        assertFalse(session.canUndo());
    }

    @Test
    void pasteReadsClipboardAndChangesDocumentWhenNonEmpty() {
        var session = session("abc");
        var clipboard = new FakeClipboard();
        clipboard.text = "X";
        var paste = BuiltInEditorActions.paste();

        var result = paste.execute(new EditorActionContext(session, clipboard));

        assertTrue(paste.isEnabled(new EditorActionContext(session, clipboard)));
        assertTrue(result.documentChanged());
        assertEquals("abcX", paragraphText(session.current().document()));
    }

    @Test
    void pasteWithEmptyClipboardIsNoOp() {
        var session = session("abc");
        var paste = BuiltInEditorActions.paste();

        var result = paste.execute(new EditorActionContext(session, new FakeClipboard()));

        assertFalse(result.documentChanged());
        assertEquals("abc", paragraphText(session.current().document()));
    }

    @Test
    void editMenuActionsHaveExpectedOrderAndShortcuts() {
        var actions = BuiltInEditorActions.editMenuActions();

        assertEquals(List.of(
                EditorActionId.UNDO,
                EditorActionId.REDO,
                EditorActionId.CUT,
                EditorActionId.COPY,
                EditorActionId.PASTE,
                EditorActionId.DELETE), actions.stream().map(EditorAction::id).toList());
        assertEquals("Ctrl+Z", actions.get(0).shortcut().orElseThrow().displayText());
        assertEquals("Ctrl+Y", actions.get(1).shortcut().orElseThrow().displayText());
        assertEquals("Ctrl+X", actions.get(2).shortcut().orElseThrow().displayText());
        assertEquals("Ctrl+C", actions.get(3).shortcut().orElseThrow().displayText());
        assertEquals("Ctrl+V", actions.get(4).shortcut().orElseThrow().displayText());
        assertEquals("Del", actions.get(5).shortcut().orElseThrow().displayText());
    }

    @Test
    void insertMenuActionsContainEquation() {
        var actions = BuiltInEditorActions.insertMenuActions();

        assertEquals(List.of(
                EditorActionId.INSERT_EQUATION,
                EditorActionId.INSERT_TABLE,
                EditorActionId.INSERT_PLOT,
                EditorActionId.INSERT_DIAGRAM,
                EditorActionId.INSERT_CROSS_REFERENCE,
                EditorActionId.INSERT_TABLE_OF_CONTENTS,
                EditorActionId.MATH_INSERT_FRACTION,
                EditorActionId.MATH_INSERT_ROOT,
                EditorActionId.MATH_INSERT_PARENTHESES_GROUP,
                EditorActionId.MATH_INSERT_BRACKETS_GROUP,
                EditorActionId.MATH_INSERT_BRACES_GROUP,
                EditorActionId.MATH_INSERT_SUPERSCRIPT,
                EditorActionId.MATH_INSERT_SUBSCRIPT,
                EditorActionId.MATH_CONVERT_NAMED_OPERATOR,
                EditorActionId.MATH_CONVERT_TEXT), actions.stream().map(EditorAction::id).toList());
        assertEquals("Equation", actions.get(0).label());
        assertEquals("Table", actions.get(1).label());
        assertEquals("Plot", actions.get(2).label());
        assertEquals("Diagram", actions.get(3).label());
        assertEquals("Cross Reference", actions.get(4).label());
        assertEquals("Insert cross-reference", actions.get(4).tooltip());
        assertEquals("Table of Contents", actions.get(5).label());
        assertEquals("Insert table of contents", actions.get(5).tooltip());
        assertEquals("Fraction", actions.get(6).label());
        assertEquals("Insert fraction", actions.get(6).tooltip());
        assertEquals("Root", actions.get(7).label());
        assertEquals("Insert square root", actions.get(7).tooltip());
        assertEquals("Parentheses", actions.get(8).label());
        assertEquals("Insert parentheses group", actions.get(8).tooltip());
        assertEquals("Brackets", actions.get(9).label());
        assertEquals("Insert brackets group", actions.get(9).tooltip());
        assertEquals("Braces", actions.get(10).label());
        assertEquals("Insert braces group", actions.get(10).tooltip());
        assertEquals("Superscript", actions.get(11).label());
        assertEquals("Insert superscript", actions.get(11).tooltip());
        assertEquals("Subscript", actions.get(12).label());
        assertEquals("Insert subscript", actions.get(12).tooltip());
    }

    @Test
    void viewMenuActionsContainOutlineToggle() {
        var actions = BuiltInEditorActions.viewMenuActions();

        assertEquals(List.of(EditorActionId.TOGGLE_OUTLINE), actions.stream().map(EditorAction::id).toList());
        assertEquals("Outline", actions.get(0).label());
        assertEquals("Show or hide document outline", actions.get(0).tooltip());
    }

    @Test
    void formatMenuActionsHaveExpectedOrderAndShortcuts() {
        var actions = BuiltInEditorActions.formatMenuActions();

        assertEquals(List.of(
                EditorActionId.PARAGRAPH,
                EditorActionId.HEADING_1,
                EditorActionId.HEADING_2,
                EditorActionId.HEADING_3,
                EditorActionId.HEADING_4,
                EditorActionId.HEADING_5,
                EditorActionId.HEADING_6,
                EditorActionId.BOLD,
                EditorActionId.ITALIC), actions.stream().map(EditorAction::id).toList());
        assertEquals("Ctrl+B", actions.get(7).shortcut().orElseThrow().displayText());
        assertEquals("Ctrl+I", actions.get(8).shortcut().orElseThrow().displayText());
    }

    @Test
    void tableMenuActionsHaveExpectedOrderAndNoShortcuts() {
        var actions = BuiltInEditorActions.tableMenuActions();

        assertEquals(List.of(
                EditorActionId.TABLE_INSERT_ROW_ABOVE,
                EditorActionId.TABLE_INSERT_ROW_BELOW,
                EditorActionId.TABLE_DELETE_ROW,
                EditorActionId.TABLE_INSERT_COLUMN_LEFT,
                EditorActionId.TABLE_INSERT_COLUMN_RIGHT,
                EditorActionId.TABLE_DELETE_COLUMN), actions.stream().map(EditorAction::id).toList());
        assertTrue(actions.stream().allMatch(action -> action.shortcut().isEmpty()));
    }

    @Test
    void boldActionReportsOffOnAndMixedSelectionState() {
        var session = new EditorSession(document(paragraph(
                text("plain "),
                text("bold", TextMark.BOLD))), 0);
        var bold = BuiltInEditorActions.bold();
        var context = new EditorActionContext(session, new FakeClipboard());

        select(session, 0, 5);
        assertEquals(ActionSelectionState.OFF, bold.selectionState(context));
        select(session, 6, 10);
        assertEquals(ActionSelectionState.ON, bold.selectionState(context));
        select(session, 0, 10);
        assertEquals(ActionSelectionState.MIXED, bold.selectionState(context));
    }

    @Test
    void boldAndItalicActionsExecuteThroughSharedActionAtSelectionAndCaret() {
        var session = session("velocity");
        var context = new EditorActionContext(session, new FakeClipboard());
        var bold = BuiltInEditorActions.bold();
        var italic = BuiltInEditorActions.italic();

        select(session, 0, 8);
        assertTrue(bold.execute(context).documentChanged());
        assertEquals(Set.of(TextMark.BOLD), textAt(session.current().document(), 0).marks());
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 8)));
        assertFalse(italic.execute(context).documentChanged());
        session.typeText("!");

        assertEquals(Set.of(TextMark.BOLD, TextMark.ITALIC), textAt(session.current().document(), 1).marks());
    }

    @Test
    void blockStyleActionsReportSelectedStateAndExecute() {
        var session = session("Results");
        var context = new EditorActionContext(session, new FakeClipboard());
        var paragraph = BuiltInEditorActions.paragraph();
        var heading3 = BuiltInEditorActions.heading(3);

        assertEquals(ActionSelectionState.ON, paragraph.selectionState(context));
        assertEquals(ActionSelectionState.OFF, heading3.selectionState(context));

        var result = heading3.execute(context);

        assertTrue(result.documentChanged());
        assertEquals(ActionSelectionState.ON, heading3.selectionState(context));
        assertEquals(3, ((Heading) session.current().document().blocks().get(0)).level());
    }

    @Test
    void blockAndInlineActionsAreDisabledForUnsupportedBlocks() {
        var session = session("editable");
        session.setCurrent(new EditorState(document(new EquationBlock(new MathIdentifier("x"))), new BlockSelection(0), java.util.Optional.empty()));
        var context = new EditorActionContext(session, new FakeClipboard());
        var heading1 = BuiltInEditorActions.heading(1);
        var bold = BuiltInEditorActions.bold();

        assertFalse(heading1.isEnabled(context));
        assertFalse(bold.isEnabled(context));
        assertEquals(ActionSelectionState.NOT_APPLICABLE, heading1.selectionState(context));
        assertEquals(ActionSelectionState.NOT_APPLICABLE, bold.selectionState(context));
    }

    @Test
    void multiBlockSelectionEnablesEditingAndFormattingFor10D() {
        var session = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 0);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 1), new DocumentPosition(1, 2)));
        var clipboard = new FakeClipboard();
        clipboard.text = "X";
        var context = new EditorActionContext(session, clipboard);
        var original = session.current();

        assertTrue(BuiltInEditorActions.copy().isEnabled(context));
        assertTrue(BuiltInEditorActions.cut().isEnabled(context));
        assertTrue(BuiltInEditorActions.paste().isEnabled(context));
        assertTrue(BuiltInEditorActions.bold().isEnabled(context));
        assertTrue(BuiltInEditorActions.italic().isEnabled(context));
        assertTrue(BuiltInEditorActions.heading(2).isEnabled(context));
        assertTrue(BuiltInEditorActions.paragraph().isEnabled(context));
        assertEquals(ActionSelectionState.OFF, BuiltInEditorActions.bold().selectionState(context));
        assertEquals(ActionSelectionState.ON, BuiltInEditorActions.paragraph().selectionState(context));

        BuiltInEditorActions.copy().execute(context);

        assertEquals("bc\nde", clipboard.text);
        assertEquals(original, session.current());
    }

    @Test
    void boundaryOnlySelectionDisablesInlineFormattingButAllowsBlockStyle() {
        var session = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 0);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 3), new DocumentPosition(1, 0)));
        var context = new EditorActionContext(session, new FakeClipboard());

        assertTrue(BuiltInEditorActions.copy().isEnabled(context));
        assertTrue(BuiltInEditorActions.cut().isEnabled(context));
        assertTrue(BuiltInEditorActions.paste().isEnabled(context));
        assertFalse(BuiltInEditorActions.bold().isEnabled(context));
        assertFalse(BuiltInEditorActions.italic().isEnabled(context));
        assertTrue(BuiltInEditorActions.paragraph().isEnabled(context));
        assertTrue(BuiltInEditorActions.heading(3).isEnabled(context));
        assertEquals(ActionSelectionState.NOT_APPLICABLE, BuiltInEditorActions.bold().selectionState(context));
        assertEquals(ActionSelectionState.ON, BuiltInEditorActions.paragraph().selectionState(context));
    }

    @Test
    void insertEquationActionApplicabilityMatches11CScope() {
        var action = BuiltInEditorActions.insertEquation();
        var caret = session("abc");
        var sameBlockSelection = session("abc");
        var multiBlockSelection = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 0);
        var boundarySelection = new EditorSession(document(paragraph(text("abc")), paragraph(text("def"))), 0);
        var blockDocument = document(paragraph(text("A")), new EquationBlock(new MathSequence(List.of())));
        var blockSelection = new EditorSession(blockDocument, 0);

        select(sameBlockSelection, 0, 1);
        multiBlockSelection.setCurrent(new EditorState(multiBlockSelection.current().document(), new DocumentPosition(0, 1), new DocumentPosition(1, 1)));
        boundarySelection.setCurrent(new EditorState(boundarySelection.current().document(), new DocumentPosition(0, 3), new DocumentPosition(1, 0)));
        blockSelection.setCurrent(new EditorState(blockSelection.current().document(), new BlockSelection(1), java.util.Optional.empty()));

        assertTrue(action.isEnabled(new EditorActionContext(caret, new FakeClipboard())));
        assertTrue(action.isEnabled(new EditorActionContext(sameBlockSelection, new FakeClipboard())));
        assertTrue(action.isEnabled(new EditorActionContext(multiBlockSelection, new FakeClipboard())));
        assertTrue(action.isEnabled(new EditorActionContext(boundarySelection, new FakeClipboard())));
        assertTrue(action.isEnabled(new EditorActionContext(blockSelection, new FakeClipboard())));
    }

    @Test
    void blockSelectionDisablesFormattingAndClipboardButAllowsInsertEquation() {
        var session = new EditorSession(document(paragraph(text("A")), new EquationBlock(new MathSequence(List.of()))), 0);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(1), java.util.Optional.empty()));
        var context = new EditorActionContext(session, new FakeClipboard());

        assertTrue(BuiltInEditorActions.copy().isEnabled(context));
        assertTrue(BuiltInEditorActions.cut().isEnabled(context));
        assertFalse(BuiltInEditorActions.paste().isEnabled(context));
        assertFalse(BuiltInEditorActions.bold().isEnabled(context));
        assertFalse(BuiltInEditorActions.italic().isEnabled(context));
        assertFalse(BuiltInEditorActions.paragraph().isEnabled(context));
        assertFalse(BuiltInEditorActions.heading(1).isEnabled(context));
        assertTrue(BuiltInEditorActions.insertEquation().isEnabled(context));
        assertEquals(ActionSelectionState.NOT_APPLICABLE, BuiltInEditorActions.bold().selectionState(context));
        assertEquals(ActionSelectionState.NOT_APPLICABLE, BuiltInEditorActions.paragraph().selectionState(context));
    }

    @Test
    void equationEditingSelectionDisablesDocumentActionsAndClipboard() {
        var session = new EditorSession(document(paragraph(text("A")), new EquationBlock(new MathSequence(List.of()))), 0);
        session.setCurrent(new EditorState(
                session.current().document(),
                new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))),
                java.util.Optional.empty()));
        var context = new EditorActionContext(session, new FakeClipboard());

        assertFalse(BuiltInEditorActions.copy().isEnabled(context));
        assertFalse(BuiltInEditorActions.cut().isEnabled(context));
        assertFalse(BuiltInEditorActions.paste().isEnabled(context));
        assertFalse(BuiltInEditorActions.bold().isEnabled(context));
        assertFalse(BuiltInEditorActions.italic().isEnabled(context));
        assertFalse(BuiltInEditorActions.paragraph().isEnabled(context));
        assertFalse(BuiltInEditorActions.heading(1).isEnabled(context));
        assertFalse(BuiltInEditorActions.insertEquation().isEnabled(context));
        assertFalse(BuiltInEditorActions.convertToNamedOperator().isEnabled(context));
        assertFalse(BuiltInEditorActions.convertToMathText().isEnabled(context));
    }

    @Test
    void semanticConversionActionsRequestPopupOnlyForEligibleEquationRanges() {
        var session = equationSession(sequence(identifier("s"), identifier("i"), identifier("n")));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 3));
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, selection), java.util.Optional.empty()));
        var context = new EditorActionContext(session, new FakeClipboard());

        var named = BuiltInEditorActions.convertToNamedOperator();
        var text = BuiltInEditorActions.convertToMathText();

        assertTrue(named.isEnabled(context));
        assertTrue(text.isEnabled(context));
        assertEquals(java.util.Optional.of(SemanticMathTokenKind.NAMED_OPERATOR), named.execute(context).semanticTokenPopup());
        assertEquals(java.util.Optional.of(SemanticMathTokenKind.MATH_TEXT), text.execute(context).semanticTokenPopup());
        assertEquals(sequence(identifier("s"), identifier("i"), identifier("n")), equationExpression(session, 1));

        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), java.util.Optional.empty()));
        assertFalse(named.isEnabled(context));
        assertFalse(text.isEnabled(context));
    }

    @Test
    void semanticConversionActionApplicabilityRejectsStructuredAndOperatorSelections() {
        var operatorSession = equationSession(sequence(identifier("x"), operator("+"), identifier("y")));
        operatorSession.setCurrent(new EditorState(operatorSession.current().document(), new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 3))), java.util.Optional.empty()));
        var fractionSession = equationSession(sequence(identifier("x"), new MathFraction(identifier("a"), identifier("b"))));
        fractionSession.setCurrent(new EditorState(fractionSession.current().document(), new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 1),
                new MathSequencePosition(MathPath.ROOT, 2))), java.util.Optional.empty()));

        assertFalse(BuiltInEditorActions.convertToNamedOperator().isEnabled(new EditorActionContext(operatorSession, new FakeClipboard())));
        assertFalse(BuiltInEditorActions.convertToMathText().isEnabled(new EditorActionContext(fractionSession, new FakeClipboard())));
    }

    @Test
    void mathRangeCopyWritesFallbackAndInstallsStructuredSidecar() {
        var session = equationSession(sequence(identifier("x"), operator("+"), number("1")));
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 3))), java.util.Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        var context = new EditorActionContext(session, clipboard, sidecar);

        var result = BuiltInEditorActions.copy().execute(context);

        assertFalse(result.documentChanged());
        assertEquals("x + 1", clipboard.text);
        assertTrue(sidecar.snapshot().orElseThrow().payload() instanceof dev.rgcb.scholar.math.clipboard.MathClipboardPayload);
        assertEquals(sequence(identifier("x"), operator("+"), number("1")), equationExpression(session, 1));
    }

    @Test
    void nativeClipboardPreservesMathScriptStructure() {
        var script = new MathScript(
                new MathRoot(identifier("x"), java.util.Optional.empty()),
                java.util.Optional.empty(),
                java.util.Optional.of(number("2")));
        var session = equationSession(sequence(script));
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        var context = new EditorActionContext(session, clipboard, sidecar);

        BuiltInEditorActions.copy().execute(context);
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));
        BuiltInEditorActions.paste().execute(context);

        assertEquals("sqrt(x)^2", clipboard.text);
        assertEquals(sequence(script, script), equationExpression(session, 1));
    }

    @Test
    void failedMathCopyLeavesExistingSidecarUnchanged() {
        var session = equationSession(sequence(identifier("x")));
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));
        var clipboard = new FakeClipboard();
        clipboard.writeSucceeds = false;
        var sidecar = new ScholarClipboardService();
        sidecar.install("old", new dev.rgcb.scholar.math.clipboard.MathClipboardPayload(sequence(identifier("old"))));

        BuiltInEditorActions.copy().execute(new EditorActionContext(session, clipboard, sidecar));

        assertEquals("old", sidecar.snapshot().orElseThrow().plainText());
    }

    @Test
    void mathCutWritesClipboardThenDeletesInOneUndoableTransaction() {
        var session = equationSession(sequence(identifier("a"), identifier("b"), identifier("c")));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 2), new MathSequencePosition(MathPath.ROOT, 0));
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, selection), java.util.Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard, sidecar));

        assertTrue(result.documentChanged());
        assertEquals("a b", clipboard.text);
        assertEquals(sequence(identifier("c")), equationExpression(session, 1));
        assertTrue(session.undo());
        assertEquals(sequence(identifier("a"), identifier("b"), identifier("c")), equationExpression(session, 1));
        assertEquals(new EquationEditingSelection(1, selection), session.current().selection());
    }

    @Test
    void failedMathCutDoesNotDeleteOrInstallSidecar() {
        var session = equationSession(sequence(identifier("a"), identifier("b")));
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 2))), java.util.Optional.empty()));
        var clipboard = new FakeClipboard();
        clipboard.writeSucceeds = false;
        var sidecar = new ScholarClipboardService();

        var result = BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard, sidecar));

        assertFalse(result.documentChanged());
        assertEquals(sequence(identifier("a"), identifier("b")), equationExpression(session, 1));
        assertTrue(sidecar.snapshot().isEmpty());
        assertFalse(session.canUndo());
    }

    @Test
    void structuredPasteUsesNativeSidecarFirstAndFallsBackToExternalImportOnMismatch() {
        var session = equationSession(new MathSequence(List.of()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        sidecar.install("x + 1", new dev.rgcb.scholar.math.clipboard.MathClipboardPayload(sequence(identifier("x"), operator("+"), number("1"))));
        clipboard.text = "x + 1";
        var context = new EditorActionContext(session, clipboard, sidecar);

        assertTrue(BuiltInEditorActions.paste().isEnabled(context));
        assertTrue(BuiltInEditorActions.paste().execute(context).documentChanged());
        assertEquals(sequence(identifier("x"), operator("+"), number("1")), equationExpression(session, 1));

        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 3))), java.util.Optional.empty()));
        clipboard.text = "x + 2";

        assertTrue(BuiltInEditorActions.paste().isEnabled(context));
        assertTrue(sidecar.snapshot().isEmpty());
        assertTrue(BuiltInEditorActions.paste().execute(context).documentChanged());
        assertEquals(sequence(identifier("x"), operator("+"), number("1"), identifier("x"), operator("+"), number("2")), equationExpression(session, 1));
        assertTrue(sidecar.snapshot().isEmpty());
    }

    @Test
    void structuredPasteInsideTokenAndOverRangePreservesSegmentation() {
        var fragment = sequence(identifier("x"), operator("+"), number("1"));
        var session = equationSession(sequence(number("1234")));
        var clipboard = new FakeClipboard();
        clipboard.text = "x + 1";
        var sidecar = new ScholarClipboardService();
        sidecar.install("x + 1", new dev.rgcb.scholar.math.clipboard.MathClipboardPayload(fragment));
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathCaretSelection(
                new dev.rgcb.scholar.math.editor.MathTokenPosition(MathPath.ROOT.append(new dev.rgcb.scholar.math.editor.SequenceChild(0)), 2))), java.util.Optional.empty()));

        BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, sidecar));

        assertEquals(sequence(number("12"), identifier("x"), operator("+"), number("1"), number("34")), equationExpression(session, 1));
    }

    @Test
    void externalMathPasteTargetsCaretsRangesTokensAndFractionSlots() {
        var clipboard = new FakeClipboard();
        clipboard.text = "x+1";

        var empty = equationSession(new MathSequence(List.of()));
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(empty, clipboard, new ScholarClipboardService())).documentChanged());
        assertEquals(sequence(identifier("x"), operator("+"), number("1")), equationExpression(empty, 1));

        var middle = equationSession(sequence(identifier("a"), identifier("b")));
        middle.setCurrent(new EditorState(middle.current().document(), new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(middle, clipboard, new ScholarClipboardService())).documentChanged());
        assertEquals(sequence(identifier("a"), identifier("x"), operator("+"), number("1"), identifier("b")), equationExpression(middle, 1));

        var insideNumber = equationSession(sequence(number("1234")));
        insideNumber.setCurrent(new EditorState(insideNumber.current().document(), new EquationEditingSelection(1, new MathCaretSelection(
                new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 2))), java.util.Optional.empty()));
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(insideNumber, clipboard, new ScholarClipboardService())).documentChanged());
        assertEquals(sequence(number("12"), identifier("x"), operator("+"), number("1"), number("34")), equationExpression(insideNumber, 1));

        var insideIdentifier = equationSession(sequence(identifier("velocity")));
        insideIdentifier.setCurrent(new EditorState(insideIdentifier.current().document(), new EquationEditingSelection(1, new MathCaretSelection(
                new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 3))), java.util.Optional.empty()));
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(insideIdentifier, clipboard, new ScholarClipboardService())).documentChanged());
        assertEquals(sequence(identifier("vel"), identifier("x"), operator("+"), number("1"), identifier("ocity")), equationExpression(insideIdentifier, 1));

        var range = equationSession(sequence(identifier("a"), identifier("b"), identifier("c")));
        var backwardSelection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 2), new MathSequencePosition(MathPath.ROOT, 0));
        range.setCurrent(new EditorState(range.current().document(), new EquationEditingSelection(1, backwardSelection), java.util.Optional.empty()));
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(range, clipboard, new ScholarClipboardService())).documentChanged());
        assertEquals(sequence(identifier("x"), operator("+"), number("1"), identifier("c")), equationExpression(range, 1));
        assertTrue(range.undo());
        assertEquals(new EquationEditingSelection(1, backwardSelection), range.current().selection());

        var fraction = equationSession(sequence(new MathFraction(identifier("n"), identifier("d"))));
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));
        fraction.setCurrent(new EditorState(fraction.current().document(), new EquationEditingSelection(1, new MathCaretSelection(
                new MathSequencePosition(fractionPath.append(new FractionNumerator()), 1))), java.util.Optional.empty()));
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(fraction, clipboard, new ScholarClipboardService())).documentChanged());
        assertEquals(sequence(new MathFraction(sequence(identifier("n"), identifier("x"), operator("+"), number("1")), identifier("d"))), equationExpression(fraction, 1));

        fraction.setCurrent(new EditorState(fraction.current().document(), new EquationEditingSelection(1, new MathCaretSelection(
                new MathSequencePosition(fractionPath.append(new FractionDenominator()), 0))), java.util.Optional.empty()));
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(fraction, clipboard, new ScholarClipboardService())).documentChanged());
        assertEquals(sequence(new MathFraction(
                sequence(identifier("n"), identifier("x"), operator("+"), number("1")),
                sequence(identifier("x"), operator("+"), number("1"), identifier("d")))), equationExpression(fraction, 1));
    }

    @Test
    void externalMathPasteKeepsSlashLinearAndTokenizesLettersIndividually() {
        var session = equationSession(new MathSequence(List.of()));
        var clipboard = new FakeClipboard();
        clipboard.text = "velocity+1";

        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, new ScholarClipboardService())).documentChanged());
        assertEquals(sequence(
                identifier("v"),
                identifier("e"),
                identifier("l"),
                identifier("o"),
                identifier("c"),
                identifier("i"),
                identifier("t"),
                identifier("y"),
                operator("+"),
                number("1")), equationExpression(session, 1));

        session = equationSession(new MathSequence(List.of()));
        clipboard.text = "(x+1)/y";
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, new ScholarClipboardService())).documentChanged());
        assertEquals(sequence(symbol("("), identifier("x"), operator("+"), number("1"), symbol(")"), operator("/"), identifier("y")), equationExpression(session, 1));

        session = equationSession(new MathSequence(List.of()));
        clipboard.text = "Δx − θ";
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, new ScholarClipboardService())).documentChanged());
        assertEquals(sequence(identifier("Δ"), identifier("x"), operator("-"), identifier("θ")), equationExpression(session, 1));

        session = equationSession(new MathSequence(List.of()));
        clipboard.text = "x/y";
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, new ScholarClipboardService())).documentChanged());
        assertEquals(sequence(identifier("x"), operator("/"), identifier("y")), equationExpression(session, 1));

        session = equationSession(new MathSequence(List.of()));
        clipboard.text = "sqrt(x)";
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, new ScholarClipboardService())).documentChanged());
        assertEquals(sequence(identifier("s"), identifier("q"), identifier("r"), identifier("t"), symbol("("), identifier("x"), symbol(")")), equationExpression(session, 1));
    }

    @Test
    void nativeStructuredClipboardPreservesExplicitMultiCharacterIdentifier() {
        var session = equationSession(new MathSequence(List.of()));
        var clipboard = new FakeClipboard();
        clipboard.text = "velocity";
        var sidecar = new ScholarClipboardService();
        sidecar.install("velocity", new dev.rgcb.scholar.math.clipboard.MathClipboardPayload(sequence(identifier("velocity"))));

        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, sidecar)).documentChanged());

        assertEquals(sequence(identifier("velocity")), equationExpression(session, 1));
    }

    @Test
    void nativeStructuredClipboardPreservesSemanticMathTokens() {
        var fragment = sequence(
                new MathNamedOperator("sin"),
                symbol("("),
                identifier("x"),
                symbol(")"),
                new MathText("if"));
        var session = equationSession(fragment);
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 5))), java.util.Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        var context = new EditorActionContext(session, clipboard, sidecar);

        assertTrue(BuiltInEditorActions.copy().isEnabled(context));
        assertFalse(BuiltInEditorActions.copy().execute(context).documentChanged());
        assertEquals("sin ( x ) if", clipboard.text);

        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 5))), java.util.Optional.empty()));
        assertTrue(BuiltInEditorActions.paste().execute(context).documentChanged());

        assertEquals(sequence(
                new MathNamedOperator("sin"),
                symbol("("),
                identifier("x"),
                symbol(")"),
                new MathText("if"),
                new MathNamedOperator("sin"),
                symbol("("),
                identifier("x"),
                symbol(")"),
                new MathText("if")), equationExpression(session, 1));
    }

    @Test
    void nativeFractionSidecarTakesPrecedenceOverExternalSlashImport() {
        var session = equationSession(new MathSequence(List.of()));
        var clipboard = new FakeClipboard();
        clipboard.text = "x / y";
        var sidecar = new ScholarClipboardService();
        sidecar.install("x / y", new dev.rgcb.scholar.math.clipboard.MathClipboardPayload(
                sequence(new MathFraction(identifier("x"), identifier("y")))));

        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(session, clipboard, sidecar)).documentChanged());

        assertEquals(sequence(new MathFraction(identifier("x"), identifier("y"))), equationExpression(session, 1));
    }

    @Test
    void nativeRootSidecarPreservesStructuredSquareRoot() {
        var root = new MathRoot(sequence(new MathNamedOperator("sin"), symbol("("), identifier("x"), symbol(")")), java.util.Optional.empty());
        var session = equationSession(sequence(root));
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        var context = new EditorActionContext(session, clipboard, sidecar);

        assertFalse(BuiltInEditorActions.copy().execute(context).documentChanged());
        assertEquals("sqrt(sin ( x ))", clipboard.text);

        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));
        assertTrue(BuiltInEditorActions.paste().execute(context).documentChanged());

        assertEquals(sequence(root, root), equationExpression(session, 1));
    }

    @Test
    void copyingAndPastingStructuralFractionThroughActionsKeepsFractionStacked() {
        var sourceFraction = new MathFraction(identifier("x"), identifier("y"));
        var session = equationSession(sequence(sourceFraction));
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        var context = new EditorActionContext(session, clipboard, sidecar);

        assertTrue(BuiltInEditorActions.copy().isEnabled(context));
        assertFalse(BuiltInEditorActions.copy().execute(context).documentChanged());
        assertEquals("x / y", clipboard.text);

        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));
        assertTrue(BuiltInEditorActions.paste().isEnabled(context));
        assertTrue(BuiltInEditorActions.paste().execute(context).documentChanged());

        assertEquals(sequence(sourceFraction, sourceFraction), equationExpression(session, 1));
    }

    @Test
    void copyingCompleteInteriorFractionSelectionThroughActionsKeepsFractionStacked() {
        var sourceFraction = new MathFraction(sequence(identifier("x"), operator("+"), number("1")), identifier("y"));
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));
        var session = equationSession(sequence(sourceFraction));
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(fractionPath.append(new FractionNumerator()), 0),
                new MathSequencePosition(fractionPath.append(new FractionDenominator()), 1))), java.util.Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        var context = new EditorActionContext(session, clipboard, sidecar);

        assertTrue(BuiltInEditorActions.copy().isEnabled(context));
        BuiltInEditorActions.copy().execute(context);
        assertEquals("(x + 1) / y", clipboard.text);

        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));
        assertTrue(BuiltInEditorActions.paste().execute(context).documentChanged());

        assertEquals(sequence(sourceFraction, sourceFraction), equationExpression(session, 1));
    }

    @Test
    void invalidExternalPasteAfterSidecarMismatchDoesNotMutate() {
        var session = equationSession(sequence(identifier("a")));
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));
        var clipboard = new FakeClipboard();
        clipboard.text = "x^2";
        var sidecar = new ScholarClipboardService();
        sidecar.install("old", new dev.rgcb.scholar.math.clipboard.MathClipboardPayload(sequence(identifier("old"))));
        var context = new EditorActionContext(session, clipboard, sidecar);

        assertFalse(BuiltInEditorActions.paste().isEnabled(context));
        assertFalse(BuiltInEditorActions.paste().execute(context).documentChanged());
        assertEquals(sequence(identifier("a")), equationExpression(session, 1));
        assertTrue(sidecar.snapshot().isEmpty());
        assertFalse(session.canUndo());
    }

    @Test
    void textCopyClearsExistingMathSidecar() {
        var session = session("abcdef");
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        sidecar.install("x", new dev.rgcb.scholar.math.clipboard.MathClipboardPayload(sequence(identifier("x"))));
        select(session, 1, 3);

        BuiltInEditorActions.copy().execute(new EditorActionContext(session, clipboard, sidecar));

        assertEquals("bc", clipboard.text);
        assertTrue(sidecar.snapshot().isEmpty());
    }

    @Test
    void textCutClearsExistingMathSidecarAfterSuccessfulWrite() {
        var session = session("abcdef");
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        sidecar.install("x", new dev.rgcb.scholar.math.clipboard.MathClipboardPayload(sequence(identifier("x"))));
        select(session, 1, 3);

        BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard, sidecar));

        assertEquals("bc", clipboard.text);
        assertEquals("adef", paragraphText(session.current().document()));
        assertTrue(sidecar.snapshot().isEmpty());
    }

    @Test
    void insertFractionActionOnlyAppliesDuringEquationEditing() {
        var textSession = session("abc");
        var blockSession = new EditorSession(document(paragraph(text("A")), new EquationBlock(new MathSequence(List.of()))), 0);
        var equationSession = new EditorSession(document(paragraph(text("A")), new EquationBlock(new MathSequence(List.of()))), 0);
        blockSession.setCurrent(new EditorState(blockSession.current().document(), new BlockSelection(1), java.util.Optional.empty()));
        equationSession.setCurrent(new EditorState(
                equationSession.current().document(),
                new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))),
                java.util.Optional.empty()));
        var action = BuiltInEditorActions.insertFraction();

        assertFalse(action.isEnabled(new EditorActionContext(textSession, new FakeClipboard())));
        assertFalse(action.isEnabled(new EditorActionContext(blockSession, new FakeClipboard())));
        assertTrue(action.isEnabled(new EditorActionContext(equationSession, new FakeClipboard())));
        assertFalse(BuiltInEditorActions.insertEquation().isEnabled(new EditorActionContext(equationSession, new FakeClipboard())));

        var result = action.execute(new EditorActionContext(equationSession, new FakeClipboard()));

        assertTrue(result.documentChanged());
        assertTrue(result.caretShouldBeVisible());
        assertTrue(equationSession.current().isEquationEditingSelection());
    }

    @Test
    void insertRootActionOnlyAppliesDuringEquationEditingAndUsesSharedMutationPath() {
        var textSession = session("abc");
        var blockSession = new EditorSession(document(paragraph(text("A")), new EquationBlock(new MathSequence(List.of()))), 0);
        var equationSession = new EditorSession(document(paragraph(text("A")), new EquationBlock(new MathSequence(List.of()))), 0);
        blockSession.setCurrent(new EditorState(blockSession.current().document(), new BlockSelection(1), java.util.Optional.empty()));
        equationSession.setCurrent(new EditorState(
                equationSession.current().document(),
                new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))),
                java.util.Optional.empty()));
        var action = BuiltInEditorActions.insertRoot();

        assertFalse(action.isEnabled(new EditorActionContext(textSession, new FakeClipboard())));
        assertFalse(action.isEnabled(new EditorActionContext(blockSession, new FakeClipboard())));
        assertTrue(action.isEnabled(new EditorActionContext(equationSession, new FakeClipboard())));

        var result = action.execute(new EditorActionContext(equationSession, new FakeClipboard()));

        assertTrue(result.documentChanged());
        assertEquals(new MathSequence(List.of(new MathRoot(new MathSequence(List.of()), java.util.Optional.empty()))),
                ((EquationBlock) equationSession.current().document().blocks().get(1)).expression());
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(
                MathPath.ROOT.append(new SequenceChild(0)).append(new RootRadicand()), 0))), equationSession.current().selection());
    }

    @Test
    void groupActionsOnlyApplyDuringEquationEditingAndUseSharedMutationPath() {
        var textSession = session("abc");
        var equationSession = new EditorSession(document(paragraph(text("A")), new EquationBlock(sequence())), 0);
        equationSession.setCurrent(new EditorState(
                equationSession.current().document(),
                new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))),
                java.util.Optional.empty()));
        var action = BuiltInEditorActions.insertBracketsGroup();

        assertFalse(action.isEnabled(new EditorActionContext(textSession, new FakeClipboard())));
        assertTrue(action.isEnabled(new EditorActionContext(equationSession, new FakeClipboard())));

        var result = action.execute(new EditorActionContext(equationSession, new FakeClipboard()));

        assertTrue(result.documentChanged());
        assertEquals(sequence(new MathGroup(sequence(), MathDelimiter.BRACKETS)),
                ((EquationBlock) equationSession.current().document().blocks().get(1)).expression());
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(
                MathPath.ROOT.append(new SequenceChild(0)).append(new GroupContent()), 0))), equationSession.current().selection());
    }

    @Test
    void nativeStructuredClipboardPreservesStructuralGroups() {
        var group = new MathGroup(sequence(identifier("x"), operator("+"), number("1")), MathDelimiter.PARENTHESES);
        var session = equationSession(sequence(group));
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));
        var clipboard = new FakeClipboard();
        var sidecar = new ScholarClipboardService();
        var context = new EditorActionContext(session, clipboard, sidecar);

        assertFalse(BuiltInEditorActions.copy().execute(context).documentChanged());
        assertEquals("(x + 1)", clipboard.text);

        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))), java.util.Optional.empty()));
        assertTrue(BuiltInEditorActions.paste().execute(context).documentChanged());

        assertEquals(sequence(group, group), equationExpression(session, 1));
    }

    @Test
    void scriptActionsOnlyApplyDuringEquationEditingAndUseSharedMutationPath() {
        var textSession = session("abc");
        var equationSession = new EditorSession(document(paragraph(text("A")), new EquationBlock(sequence(identifier("x")))), 0);
        equationSession.setCurrent(new EditorState(
                equationSession.current().document(),
                new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 1))),
                java.util.Optional.empty()));
        var action = BuiltInEditorActions.insertSuperscript();

        assertFalse(action.isEnabled(new EditorActionContext(textSession, new FakeClipboard())));
        assertTrue(action.isEnabled(new EditorActionContext(equationSession, new FakeClipboard())));

        var result = action.execute(new EditorActionContext(equationSession, new FakeClipboard()));

        assertTrue(result.documentChanged());
        assertEquals(sequence(new MathScript(identifier("x"), java.util.Optional.empty(), java.util.Optional.of(sequence()))),
                ((EquationBlock) equationSession.current().document().blocks().get(1)).expression());
        assertEquals(new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(
                MathPath.ROOT.append(new SequenceChild(0)).append(new ScriptSuperscript()), 0))), equationSession.current().selection());
    }

    @Test
    void mixedMultiBlockStatesAreReportedToToolbarAndMenuActions() {
        var session = new EditorSession(document(
                paragraph(text("abc", TextMark.BOLD)),
                heading(2, text("def"))), 0);
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 1), new DocumentPosition(1, 2)));
        var context = new EditorActionContext(session, new FakeClipboard());

        assertEquals(ActionSelectionState.MIXED, BuiltInEditorActions.bold().selectionState(context));
        assertEquals(ActionSelectionState.OFF, BuiltInEditorActions.paragraph().selectionState(context));
        assertEquals(ActionSelectionState.OFF, BuiltInEditorActions.heading(2).selectionState(context));
    }

    private static void select(EditorSession session, int anchor, int active) {
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, anchor), new DocumentPosition(0, active)));
    }

    private static EditorSession session(String text) {
        return new EditorSession(document(paragraph(text(text))), 0);
    }

    private static EditorSession equationSession(dev.rgcb.scholar.math.MathExpression expression) {
        var session = new EditorSession(document(paragraph(text("A")), new EquationBlock(expression)), 0);
        session.setCurrent(new EditorState(session.current().document(), new EquationEditingSelection(1, new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0))), java.util.Optional.empty()));
        return session;
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

    private static dev.rgcb.scholar.math.MathSymbol symbol(String value) {
        return new dev.rgcb.scholar.math.MathSymbol(value, dev.rgcb.scholar.math.MathSymbolKind.OTHER);
    }

    private static dev.rgcb.scholar.math.MathExpression equationExpression(EditorSession session, int blockIndex) {
        return ((EquationBlock) session.current().document().blocks().get(blockIndex)).expression();
    }

    private static Text textAt(Document document, int index) {
        return (Text) ((Paragraph) document.blocks().get(0)).content().nodes().get(index);
    }

    private static String paragraphText(Document document) {
        return ((Paragraph) document.blocks().get(0)).content().nodes().stream()
                .map(Text.class::cast)
                .map(Text::content)
                .reduce("", String::concat);
    }

    private static final class FakeClipboard implements ClipboardAdapter {
        private String text = "";
        private boolean writeSucceeds = true;

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean setText(String text) {
            if (!writeSucceeds) {
                return false;
            }
            this.text = text;
            return true;
        }
    }
}
