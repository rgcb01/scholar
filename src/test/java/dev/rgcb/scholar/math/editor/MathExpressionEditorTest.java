package dev.rgcb.scholar.math.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.math.MathExpression;
import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathDelimiter;
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
import dev.rgcb.scholar.math.layout.MathLayoutEngine;
import dev.rgcb.scholar.math.layout.MathTextKind;
import dev.rgcb.scholar.math.layout.MathTextMeasurer;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import java.util.List;
import org.junit.jupiter.api.Test;

class MathExpressionEditorTest {
    private final MathExpressionEditor editor = new MathExpressionEditor();

    @Test
    void validatesCanonicalTokenPositionsStrictlyInsideToken() {
        var expression = sequence(number("123"));
        var tokenPath = MathPath.ROOT.append(new SequenceChild(0));

        editor.validateSelection(expression, caret(new MathSequencePosition(MathPath.ROOT, 0)));
        editor.validateSelection(expression, caret(new MathTokenPosition(tokenPath, 1)));
        editor.validateSelection(expression, caret(new MathTokenPosition(tokenPath, 2)));
        editor.validateSelection(expression, caret(new MathSequencePosition(MathPath.ROOT, 1)));

        assertThrows(IllegalArgumentException.class, () -> new MathTokenPosition(tokenPath, 0));
        assertThrows(IllegalArgumentException.class, () -> editor.validateSelection(expression, caret(new MathTokenPosition(tokenPath, 3))));
    }

    @Test
    void typesLinearExpressionWithNumberCoalescing() {
        MathExpression expression = new MathSequence(List.of());
        MathSelection selection = editor.rootStart();

        var v = editor.insertText(expression, selection, "v");
        var equals = editor.insertText(v.expression(), v.selection(), "=");
        var one = editor.insertText(equals.expression(), equals.selection(), "1");
        var two = editor.insertText(one.expression(), one.selection(), "2");

        assertEquals(sequence(identifier("v"), operator("=", MathOperatorRole.RELATION), number("12")), two.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 3)), two.selection());
    }

    @Test
    void editsNumberInsideTokenAndDeletesCharacters() {
        var expression = sequence(number("123"));
        var tokenPath = MathPath.ROOT.append(new SequenceChild(0));

        var inserted = editor.insertText(expression, caret(new MathTokenPosition(tokenPath, 1)), "4");
        assertEquals(sequence(number("1423")), inserted.expression());

        var backspace = editor.deleteBackward(inserted.expression(), caret(new MathTokenPosition(tokenPath, 2)));
        assertEquals(sequence(number("123")), backspace.expression());

        var delete = editor.deleteForward(expression, caret(new MathTokenPosition(tokenPath, 1)));
        assertEquals(sequence(number("13")), delete.expression());
    }

    @Test
    void removesTokenWhenLastCharacterIsDeletedButKeepsEquationExpressionContainer() {
        var expression = sequence(identifier("x"));

        var deleted = editor.deleteForward(expression, caret(new MathSequencePosition(MathPath.ROOT, 0)));

        assertEquals(new MathSequence(List.of()), deleted.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 0)), deleted.selection());
    }

    @Test
    void handlesDecimalsAndRejectsSecondDecimal() {
        MathExpression expression = new MathSequence(List.of());
        MathSelection selection = editor.rootStart();

        var three = editor.insertText(expression, selection, "3");
        var dot = editor.insertText(three.expression(), three.selection(), ".");
        var one = editor.insertText(dot.expression(), dot.selection(), "1");
        var rejected = editor.insertText(one.expression(), one.selection(), ".");

        assertEquals(sequence(number("3.1")), one.expression());
        assertFalse(rejected.changed());
        assertEquals(one.expression(), rejected.expression());
    }

    @Test
    void directLettersCreateSeparateIdentifierAtoms() {
        var x = editor.insertText(new MathSequence(List.of()), editor.rootStart(), "x");
        var y = editor.insertText(x.expression(), x.selection(), "y");

        assertEquals(sequence(identifier("x"), identifier("y")), y.expression());
    }

    @Test
    void directWordTypingCreatesOneIdentifierAtomPerLetter() {
        MathExpression expression = new MathSequence(List.of());
        MathSelection selection = editor.rootStart();

        for (var character : List.of("v", "e", "l", "o", "c", "i", "t", "y")) {
            var result = editor.insertText(expression, selection, character);
            expression = result.expression();
            selection = result.selection();
        }

        assertEquals(sequence(
                identifier("v"),
                identifier("e"),
                identifier("l"),
                identifier("o"),
                identifier("c"),
                identifier("i"),
                identifier("t"),
                identifier("y")), expression);
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 8)), selection);
    }

    @Test
    void directLettersAtSequenceBoundaryInsertNewAtomsWithoutIdentifierMerging() {
        var expression = sequence(identifier("x"), identifier("y"));

        var result = editor.insertText(expression, caret(new MathSequencePosition(MathPath.ROOT, 1)), "z");

        assertEquals(sequence(identifier("x"), identifier("z"), identifier("y")), result.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 2)), result.selection());
    }

    @Test
    void directTypingMaintainsSeparateNumberAndIdentifierAtoms() {
        var two = editor.insertText(new MathSequence(List.of()), editor.rootStart(), "2");
        var xAfterNumber = editor.insertText(two.expression(), two.selection(), "x");

        var x = editor.insertText(new MathSequence(List.of()), editor.rootStart(), "x");
        var twoAfterIdentifier = editor.insertText(x.expression(), x.selection(), "2");

        assertEquals(sequence(number("2"), identifier("x")), xAfterNumber.expression());
        assertEquals(sequence(identifier("x"), number("2")), twoAfterIdentifier.expression());
    }

    @Test
    void typingInsideExistingMultiCharacterIdentifierStillEditsThatToken() {
        var expression = new MathIdentifier("velocity");

        var result = editor.insertText(
                expression,
                caret(new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 4)),
                "x");

        assertEquals(new MathIdentifier("veloxcity"), result.expression());
        assertEquals(caret(new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 5)), result.selection());
    }

    @Test
    void unsupportedSpaceInputIsNoOp() {
        var expression = sequence(identifier("x"));
        var selection = caret(new MathSequencePosition(MathPath.ROOT, 1));

        assertFalse(editor.insertText(expression, selection, " ").changed());
    }

    @Test
    void caretSuperscriptAndSubscriptWrapPreviousSingleAtomAndEnterSlot() {
        var expression = sequence(identifier("x"));
        var scriptPath = MathPath.ROOT.append(new SequenceChild(0));

        var superscript = editor.insertText(expression, caret(new MathSequencePosition(MathPath.ROOT, 1)), "^");
        var subscript = editor.insertText(expression, caret(new MathSequencePosition(MathPath.ROOT, 1)), "_");

        assertEquals(sequence(new MathScript(identifier("x"), java.util.Optional.empty(), java.util.Optional.of(sequence()))), superscript.expression());
        assertEquals(caret(new MathSequencePosition(scriptPath.append(new ScriptSuperscript()), 0)), superscript.selection());
        assertEquals(sequence(new MathScript(identifier("x"), java.util.Optional.of(sequence()), java.util.Optional.empty())), subscript.expression());
        assertEquals(caret(new MathSequencePosition(scriptPath.append(new ScriptSubscript()), 0)), subscript.selection());
    }

    @Test
    void subscriptAfterRootCreatesAndReentersRootIndexInsteadOfGenericScript() {
        var expression = sequence(new MathRoot(identifier("x"), java.util.Optional.empty()));
        var rootPath = MathPath.ROOT.append(new SequenceChild(0));

        var indexed = editor.insertText(expression, caret(new MathSequencePosition(MathPath.ROOT, 1)), "_");
        var reentered = editor.insertText(indexed.expression(), caret(new MathSequencePosition(MathPath.ROOT, 1)), "_");
        var removed = editor.deleteBackward(indexed.expression(), indexed.selection());

        assertEquals(sequence(new MathRoot(identifier("x"), java.util.Optional.of(sequence()))), indexed.expression());
        assertEquals(caret(new MathSequencePosition(rootPath.append(new RootIndex()), 0)), indexed.selection());
        assertFalse(reentered.changed());
        assertEquals(caret(new MathSequencePosition(rootPath.append(new RootIndex()), 0)), reentered.selection());
        assertEquals(expression, removed.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), removed.selection());
    }

    @Test
    void scriptCommandsRejectSequenceStartIneligibleAtomsAndMultiAtomSelections() {
        var operatorExpression = sequence(operator("+", MathOperatorRole.BINARY));
        var multiAtomSelection = new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 2));
        var expression = sequence(identifier("x"), identifier("y"));

        assertFalse(editor.insertText(expression, caret(new MathSequencePosition(MathPath.ROOT, 0)), "^").changed());
        assertFalse(editor.insertText(operatorExpression, caret(new MathSequencePosition(MathPath.ROOT, 1)), "^").changed());
        assertFalse(editor.insertScriptSlot(expression, multiAtomSelection, ScriptSlot.SUPERSCRIPT).changed());
    }

    @Test
    void selectedSingleAtomCanBecomeScriptBaseButPartialTokenCannot() {
        var expression = sequence(identifier("x"), identifier("y"));
        var selection = new MathRangeSelection(
                new MathSequencePosition(MathPath.ROOT, 0),
                new MathSequencePosition(MathPath.ROOT, 1));
        var partialToken = new MathRangeSelection(
                new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 1),
                new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 2));

        var result = editor.insertScriptSlot(expression, selection, ScriptSlot.SUPERSCRIPT);

        assertEquals(sequence(new MathScript(identifier("x"), java.util.Optional.empty(), java.util.Optional.of(sequence())), identifier("y")), result.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(0)).append(new ScriptSuperscript()), 0)), result.selection());
        assertFalse(editor.insertScriptSlot(sequence(identifier("abc")), partialToken, ScriptSlot.SUPERSCRIPT).changed());
    }

    @Test
    void existingScriptExtendsMissingSlotAndReentersPresentSlotWithoutMutation() {
        var expression = sequence(new MathScript(identifier("x"), java.util.Optional.of(sequence(identifier("i"))), java.util.Optional.empty()));
        var scriptPath = MathPath.ROOT.append(new SequenceChild(0));

        var extended = editor.insertText(expression, caret(new MathSequencePosition(MathPath.ROOT, 1)), "^");
        var reentered = editor.insertText(extended.expression(), caret(new MathSequencePosition(MathPath.ROOT, 1)), "^");

        assertEquals(sequence(new MathScript(identifier("x"), java.util.Optional.of(sequence(identifier("i"))), java.util.Optional.of(sequence()))), extended.expression());
        assertEquals(caret(new MathSequencePosition(scriptPath.append(new ScriptSuperscript()), 0)), extended.selection());
        assertFalse(reentered.changed());
        assertEquals(extended.expression(), reentered.expression());
        assertEquals(caret(new MathSequencePosition(scriptPath.append(new ScriptSuperscript()), 0)), reentered.selection());
    }

    @Test
    void absentScriptSlotPathsAreInvalid() {
        var expression = sequence(new MathScript(identifier("x"), java.util.Optional.empty(), java.util.Optional.of(sequence(number("2")))));
        var missingSubscript = MathPath.ROOT.append(new SequenceChild(0)).append(new ScriptSubscript());

        assertThrows(IllegalArgumentException.class, () -> editor.validateSelection(expression, caret(new MathSequencePosition(missingSubscript, 0))));
    }

    @Test
    void backspaceAtEmptyScriptSlotRemovesSlotAndUnwrapsFinalSlot() {
        var withOnlySuperscript = sequence(new MathScript(identifier("x"), java.util.Optional.empty(), java.util.Optional.of(sequence())));
        var withBoth = sequence(new MathScript(identifier("x"), java.util.Optional.of(sequence(identifier("i"))), java.util.Optional.of(sequence())));
        var scriptPath = MathPath.ROOT.append(new SequenceChild(0));

        var unwrapped = editor.deleteBackward(withOnlySuperscript, caret(new MathSequencePosition(scriptPath.append(new ScriptSuperscript()), 0)));
        var removedSup = editor.deleteBackward(withBoth, caret(new MathSequencePosition(scriptPath.append(new ScriptSuperscript()), 0)));

        assertEquals(sequence(identifier("x")), unwrapped.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), unwrapped.selection());
        assertEquals(sequence(new MathScript(identifier("x"), java.util.Optional.of(sequence(identifier("i"))), java.util.Optional.empty())), removedSup.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), removedSup.selection());
    }

    @Test
    void scriptBaseCanBeEditedInternallyButNotExpandedToMultipleDirectAtoms() {
        var scriptPath = MathPath.ROOT.append(new SequenceChild(0));
        var basePath = scriptPath.append(new ScriptBase());
        var tokenPath = basePath.append(new SequenceChild(0));
        var expression = sequence(new MathScript(identifier("xy"), java.util.Optional.empty(), java.util.Optional.of(number("2"))));

        var editedToken = editor.insertText(expression, caret(new MathTokenPosition(tokenPath, 1)), "z");
        var expandedBase = editor.insertText(expression, caret(new MathSequencePosition(basePath, 1)), "y");
        var deletedBase = editor.deleteBackward(expression, caret(new MathSequencePosition(basePath, 1)));

        assertEquals(sequence(new MathScript(identifier("xzy"), java.util.Optional.empty(), java.util.Optional.of(number("2")))), editedToken.expression());
        assertFalse(expandedBase.changed());
        assertEquals(expression, expandedBase.expression());
        assertFalse(deletedBase.changed());
        assertEquals(expression, deletedBase.expression());
    }

    @Test
    void navigatesLinearlyThroughTokensWithoutDuplicateEdgeStops() {
        var expression = sequence(identifier("v"), operator("=", MathOperatorRole.RELATION), number("12"));
        var p0 = caret(new MathSequencePosition(MathPath.ROOT, 0));
        var p1 = editor.moveRight(expression, p0).selection();
        var p2 = editor.moveRight(expression, p1).selection();
        var p3 = editor.moveRight(expression, p2).selection();
        var p4 = editor.moveRight(expression, p3).selection();

        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), p1);
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 2)), p2);
        assertEquals(caret(new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(2)), 1)), p3);
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 3)), p4);
    }

    @Test
    void navigatesExistingFractionForwardAndBackward() {
        var fractionPath = MathPath.ROOT.append(new SequenceChild(1));
        var expression = sequence(identifier("a"), new MathFraction(identifier("x"), identifier("y")), identifier("b"));

        var beforeFraction = caret(new MathSequencePosition(MathPath.ROOT, 1));
        var numeratorStart = editor.moveRight(expression, beforeFraction).selection();
        var numeratorEnd = editor.moveRight(expression, numeratorStart).selection();
        var denominatorStart = editor.moveRight(expression, numeratorEnd).selection();
        var denominatorEnd = editor.moveRight(expression, denominatorStart).selection();
        var afterFraction = editor.moveRight(expression, denominatorEnd).selection();

        assertEquals(caret(new MathSequencePosition(fractionPath.append(new FractionNumerator()), 0)), numeratorStart);
        assertEquals(caret(new MathSequencePosition(fractionPath.append(new FractionNumerator()), 1)), numeratorEnd);
        assertEquals(caret(new MathSequencePosition(fractionPath.append(new FractionDenominator()), 0)), denominatorStart);
        assertEquals(caret(new MathSequencePosition(fractionPath.append(new FractionDenominator()), 1)), denominatorEnd);
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 2)), afterFraction);
        assertEquals(denominatorEnd, editor.moveLeft(expression, afterFraction).selection());
        assertEquals(denominatorStart, editor.moveLeft(expression, denominatorEnd).selection());
        assertEquals(numeratorEnd, editor.moveLeft(expression, denominatorStart).selection());
        assertEquals(numeratorStart, editor.moveLeft(expression, numeratorEnd).selection());
        assertEquals(beforeFraction, editor.moveLeft(expression, numeratorStart).selection());
    }

    @Test
    void navigatesEmptyFractionSlots() {
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));
        var expression = sequence(new MathFraction(new MathSequence(List.of()), new MathSequence(List.of())));

        var numeratorStart = editor.moveRight(expression, caret(new MathSequencePosition(MathPath.ROOT, 0))).selection();
        var denominatorStart = editor.moveRight(expression, numeratorStart).selection();
        var afterFraction = editor.moveRight(expression, denominatorStart).selection();

        assertEquals(caret(new MathSequencePosition(fractionPath.append(new FractionNumerator()), 0)), numeratorStart);
        assertEquals(caret(new MathSequencePosition(fractionPath.append(new FractionDenominator()), 0)), denominatorStart);
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), afterFraction);
    }

    @Test
    void navigatesScriptBaseSubscriptSuperscriptAndParentEdges() {
        var scriptPath = MathPath.ROOT.append(new SequenceChild(0));
        var expression = sequence(new MathScript(identifier("x"), java.util.Optional.of(sequence(identifier("i"))), java.util.Optional.of(sequence(number("2")))));

        var before = caret(new MathSequencePosition(MathPath.ROOT, 0));
        var baseStart = editor.moveRight(expression, before).selection();
        var baseEnd = editor.moveRight(expression, baseStart).selection();
        var subStart = editor.moveRight(expression, baseEnd).selection();
        var subEnd = editor.moveRight(expression, subStart).selection();
        var supStart = editor.moveRight(expression, subEnd).selection();
        var supEnd = editor.moveRight(expression, supStart).selection();
        var after = editor.moveRight(expression, supEnd).selection();

        assertEquals(caret(new MathSequencePosition(scriptPath.append(new ScriptBase()), 0)), baseStart);
        assertEquals(caret(new MathSequencePosition(scriptPath.append(new ScriptBase()), 1)), baseEnd);
        assertEquals(caret(new MathSequencePosition(scriptPath.append(new ScriptSubscript()), 0)), subStart);
        assertEquals(caret(new MathSequencePosition(scriptPath.append(new ScriptSubscript()), 1)), subEnd);
        assertEquals(caret(new MathSequencePosition(scriptPath.append(new ScriptSuperscript()), 0)), supStart);
        assertEquals(caret(new MathSequencePosition(scriptPath.append(new ScriptSuperscript()), 1)), supEnd);
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), after);
        assertEquals(supEnd, editor.moveLeft(expression, after).selection());
        assertEquals(supStart, editor.moveLeft(expression, supEnd).selection());
        assertEquals(subEnd, editor.moveLeft(expression, supStart).selection());
        assertEquals(subStart, editor.moveLeft(expression, subEnd).selection());
        assertEquals(baseEnd, editor.moveLeft(expression, subStart).selection());
        assertEquals(baseStart, editor.moveLeft(expression, baseEnd).selection());
        assertEquals(before, editor.moveLeft(expression, baseStart).selection());
    }

    @Test
    void structuralDeleteDoesNotRemoveFractionWithoutNodeSelection() {
        var expression = sequence(identifier("a"), new MathFraction(identifier("x"), identifier("y")), identifier("b"));

        assertFalse(editor.deleteForward(expression, caret(new MathSequencePosition(MathPath.ROOT, 1))).changed());
        assertFalse(editor.deleteBackward(expression, caret(new MathSequencePosition(MathPath.ROOT, 2))).changed());
    }

    @Test
    void parentAdjacentDeleteEntersScriptInsteadOfDeletingIt() {
        var scriptPath = MathPath.ROOT.append(new SequenceChild(0));
        var expression = sequence(new MathScript(identifier("x"), java.util.Optional.empty(), java.util.Optional.of(number("2"))));

        var forward = editor.deleteForward(expression, caret(new MathSequencePosition(MathPath.ROOT, 0)));
        var backward = editor.deleteBackward(expression, caret(new MathSequencePosition(MathPath.ROOT, 1)));

        assertFalse(forward.changed());
        assertEquals(caret(new MathSequencePosition(scriptPath.append(new ScriptBase()), 0)), forward.selection());
        assertFalse(backward.changed());
        assertEquals(caret(new MathSequencePosition(scriptPath.append(new ScriptSuperscript()), 1)), backward.selection());
    }

    @Test
    void wholeScriptSelectionDeletesAsOneParentSequenceAtom() {
        var script = new MathScript(identifier("x"), java.util.Optional.empty(), java.util.Optional.of(number("2")));
        var expression = sequence(identifier("a"), script, identifier("b"));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 1), new MathSequencePosition(MathPath.ROOT, 2));

        var deleted = editor.deleteForward(expression, selection);

        assertEquals(sequence(identifier("a"), identifier("b")), deleted.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), deleted.selection());
    }

    @Test
    void virtualRootAndFractionSlotsMaterializeOnlyWhenEdited() {
        var root = number("12");
        var entered = editor.moveRight(root, editor.rootStart());
        assertEquals(root, entered.expression());

        var insertedBefore = editor.insertText(root, editor.rootStart(), "1");
        assertEquals(sequence(number("1"), number("12")), insertedBefore.expression());

        var fraction = new MathFraction(identifier("x"), identifier("y"));
        var fractionExpression = sequence(fraction);
        var numeratorStart = caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(0)).append(new FractionNumerator()), 0));
        var numeratorInserted = editor.insertText(fractionExpression, numeratorStart, "a");
        var updatedFraction = assertInstanceOf(MathFraction.class, ((MathSequence) numeratorInserted.expression()).expressions().get(0));
        assertEquals(sequence(identifier("a"), identifier("x")), updatedFraction.numerator());
    }

    @Test
    void unicodeIdentifiersUseLogicalCharacterBoundaries() {
        var expression = sequence(identifier("café"), identifier("θ"), identifier("λ"), identifier("e\u0301"));
        var tokenPath = MathPath.ROOT.append(new SequenceChild(0));

        var moved = editor.moveRight(expression, caret(new MathTokenPosition(tokenPath, 2)));
        var deleted = editor.deleteBackward(expression, caret(new MathTokenPosition(tokenPath, 3)));

        assertEquals(caret(new MathTokenPosition(tokenPath, 3)), moved.selection());
        assertEquals(sequence(identifier("caé"), identifier("θ"), identifier("λ"), identifier("e\u0301")), deleted.expression());
    }

    @Test
    void insertsFractionAtEmptyRootAndMovesCaretToNumerator() {
        var result = editor.insertFraction(new MathSequence(List.of()), editor.rootStart());
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));

        assertEquals(sequence(emptyFraction()), result.expression());
        assertEquals(caret(new MathSequencePosition(fractionPath.append(new FractionNumerator()), 0)), result.selection());
        assertTrue(result.changed());
    }

    @Test
    void insertsFractionBeforeAfterAndBetweenAtoms() {
        var before = editor.insertFraction(sequence(identifier("x")), caret(new MathSequencePosition(MathPath.ROOT, 0)));
        var after = editor.insertFraction(sequence(identifier("x")), caret(new MathSequencePosition(MathPath.ROOT, 1)));
        var middle = editor.insertFraction(
                sequence(identifier("a"), operator("+", MathOperatorRole.BINARY), identifier("b")),
                caret(new MathSequencePosition(MathPath.ROOT, 2)));

        assertEquals(sequence(emptyFraction(), identifier("x")), before.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(0)).append(new FractionNumerator()), 0)), before.selection());
        assertEquals(sequence(identifier("x"), emptyFraction()), after.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(1)).append(new FractionNumerator()), 0)), after.selection());
        assertEquals(sequence(identifier("a"), operator("+", MathOperatorRole.BINARY), emptyFraction(), identifier("b")), middle.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(2)).append(new FractionNumerator()), 0)), middle.selection());
    }

    @Test
    void insertsFractionInsideNumberBySplittingValidLogicalFragments() {
        var expression = new MathNumber("123");

        var result = editor.insertFraction(expression, caret(new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 1)));

        assertEquals(sequence(number("1"), emptyFraction(), number("23")), result.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(1)).append(new FractionNumerator()), 0)), result.selection());
    }

    @Test
    void documentsDecimalSplitSemanticsAroundDecimalPoint() {
        var expression = new MathNumber("3.14");
        var tokenPath = MathPath.ROOT.append(new SequenceChild(0));

        var beforeDecimal = editor.insertFraction(expression, caret(new MathTokenPosition(tokenPath, 1)));
        var afterDecimal = editor.insertFraction(expression, caret(new MathTokenPosition(tokenPath, 2)));
        var middleDecimal = editor.insertFraction(expression, caret(new MathTokenPosition(tokenPath, 3)));

        assertEquals(sequence(number("3"), emptyFraction(), number(".14")), beforeDecimal.expression());
        assertEquals(sequence(number("3."), emptyFraction(), number("14")), afterDecimal.expression());
        assertEquals(sequence(number("3.1"), emptyFraction(), number("4")), middleDecimal.expression());
    }

    @Test
    void insertsFractionInsideIdentifierBySplittingLogicalFragments() {
        var expression = new MathIdentifier("velocity");

        var result = editor.insertFraction(expression, caret(new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 3)));

        assertEquals(sequence(identifier("vel"), emptyFraction(), identifier("ocity")), result.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(1)).append(new FractionNumerator()), 0)), result.selection());
    }

    @Test
    void insertsFractionInsideUnicodeIdentifierAtLogicalBoundary() {
        var expression = new MathIdentifier("café");

        var result = editor.insertFraction(expression, caret(new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 3)));

        assertEquals(sequence(identifier("caf"), emptyFraction(), identifier("é")), result.expression());
    }

    @Test
    void materializesVirtualRootOnlyWhenFractionInsertionNeedsMultipleChildren() {
        var root = new MathIdentifier("x");

        var before = editor.insertFraction(root, caret(new MathSequencePosition(MathPath.ROOT, 0)));
        var after = editor.insertFraction(root, caret(new MathSequencePosition(MathPath.ROOT, 1)));

        assertEquals(sequence(emptyFraction(), identifier("x")), before.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(0)).append(new FractionNumerator()), 0)), before.selection());
        assertEquals(sequence(identifier("x"), emptyFraction()), after.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(1)).append(new FractionNumerator()), 0)), after.selection());
    }

    @Test
    void insertsNestedFractionInNumeratorAndDenominatorSequences() {
        var outer = new MathFraction(sequence(identifier("x"), identifier("y")), sequence(identifier("z")));
        var expression = sequence(outer);
        var outerPath = MathPath.ROOT.append(new SequenceChild(0));

        var numerator = editor.insertFraction(
                expression,
                caret(new MathSequencePosition(outerPath.append(new FractionNumerator()), 1)));
        var denominator = editor.insertFraction(
                expression,
                caret(new MathSequencePosition(outerPath.append(new FractionDenominator()), 1)));

        assertEquals(sequence(new MathFraction(sequence(identifier("x"), emptyFraction(), identifier("y")), sequence(identifier("z")))), numerator.expression());
        assertEquals(caret(new MathSequencePosition(outerPath.append(new FractionNumerator()).append(new SequenceChild(1)).append(new FractionNumerator()), 0)), numerator.selection());
        assertEquals(sequence(new MathFraction(sequence(identifier("x"), identifier("y")), sequence(identifier("z"), emptyFraction()))), denominator.expression());
        assertEquals(caret(new MathSequencePosition(outerPath.append(new FractionDenominator()).append(new SequenceChild(1)).append(new FractionNumerator()), 0)), denominator.selection());
    }

    @Test
    void materializesOnlyVirtualFractionSlotContainingInsertedFraction() {
        var expression = sequence(new MathFraction(identifier("x"), identifier("y")));
        var outerPath = MathPath.ROOT.append(new SequenceChild(0));

        var numerator = editor.insertFraction(
                expression,
                caret(new MathSequencePosition(outerPath.append(new FractionNumerator()), 1)));
        var denominator = editor.insertFraction(
                expression,
                caret(new MathSequencePosition(outerPath.append(new FractionDenominator()), 0)));

        assertEquals(sequence(new MathFraction(sequence(identifier("x"), emptyFraction()), identifier("y"))), numerator.expression());
        assertEquals(sequence(new MathFraction(identifier("x"), sequence(emptyFraction(), identifier("y")))), denominator.expression());
    }

    @Test
    void insertsFractionInsideEmptyFractionSlot() {
        var expression = sequence(new MathFraction(new MathSequence(List.of()), new MathSequence(List.of())));
        var outerPath = MathPath.ROOT.append(new SequenceChild(0));

        var result = editor.insertFraction(
                expression,
                caret(new MathSequencePosition(outerPath.append(new FractionNumerator()), 0)));

        assertEquals(sequence(new MathFraction(sequence(emptyFraction()), new MathSequence(List.of()))), result.expression());
        assertEquals(caret(new MathSequencePosition(outerPath.append(new FractionNumerator()).append(new SequenceChild(0)).append(new FractionNumerator()), 0)), result.selection());
    }

    @Test
    void returnedPathTargetsNewFractionAmongIdenticalFractions() {
        var existing = emptyFraction();
        var expression = sequence(existing);

        var result = editor.insertFraction(expression, caret(new MathSequencePosition(MathPath.ROOT, 1)));

        assertEquals(sequence(existing, emptyFraction()), result.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(1)).append(new FractionNumerator()), 0)), result.selection());
    }

    @Test
    void insertedFractionUsesExistingNavigationAndDeleteSafety() {
        var inserted = editor.insertFraction(new MathSequence(List.of()), editor.rootStart());
        var denominator = editor.moveRight(inserted.expression(), inserted.selection());
        var after = editor.moveRight(inserted.expression(), denominator.selection());

        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(0)).append(new FractionDenominator()), 0)), denominator.selection());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), after.selection());
        assertFalse(editor.deleteBackward(inserted.expression(), after.selection()).changed());
        assertFalse(editor.deleteForward(inserted.expression(), caret(new MathSequencePosition(MathPath.ROOT, 0))).changed());
    }

    @Test
    void insertsRootAtCaretAndMovesCaretIntoEmptyRadicand() {
        var result = editor.insertRoot(new MathSequence(List.of()), editor.rootStart());
        var rootPath = MathPath.ROOT.append(new SequenceChild(0));

        assertEquals(sequence(emptyRoot()), result.expression());
        assertEquals(caret(new MathSequencePosition(rootPath.append(new RootRadicand()), 0)), result.selection());
        assertTrue(result.changed());
    }

    @Test
    void wrapsSelectedMathInRootAndMovesCaretAfterRoot() {
        var expression = sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1"));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 3));

        var result = editor.insertRoot(expression, selection);

        assertEquals(sequence(new MathRoot(sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1")), java.util.Optional.empty())), result.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), result.selection());
    }

    @Test
    void editsAndNavigatesRootRadicandIncludingEmptySlots() {
        var rootPath = MathPath.ROOT.append(new SequenceChild(0));
        var expression = sequence(new MathRoot(new MathSequence(List.of()), java.util.Optional.empty()));

        var beforeRoot = caret(new MathSequencePosition(MathPath.ROOT, 0));
        var radicandStart = editor.moveRight(expression, beforeRoot).selection();
        var afterRoot = editor.moveRight(expression, radicandStart).selection();
        var backIntoRadicand = editor.moveLeft(expression, afterRoot).selection();
        var inserted = editor.insertText(expression, radicandStart, "x");

        assertEquals(caret(new MathSequencePosition(rootPath.append(new RootRadicand()), 0)), radicandStart);
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), afterRoot);
        assertEquals(radicandStart, backIntoRadicand);
        assertEquals(sequence(new MathRoot(sequence(identifier("x")), java.util.Optional.empty())), inserted.expression());
        assertEquals(caret(new MathSequencePosition(rootPath.append(new RootRadicand()), 1)), inserted.selection());
        assertFalse(editor.deleteBackward(expression, radicandStart).changed());
    }

    @Test
    void rootNavigationUsesRadicandStartAndEndForNonEmptyNestedStructures() {
        var rootPath = MathPath.ROOT.append(new SequenceChild(0));
        var expression = sequence(new MathRoot(sequence(identifier("x"), new MathFraction(identifier("a"), identifier("b"))), java.util.Optional.empty()));

        assertEquals(caret(new MathSequencePosition(rootPath.append(new RootRadicand()), 0)),
                editor.moveRight(expression, caret(new MathSequencePosition(MathPath.ROOT, 0))).selection());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)),
                editor.moveRight(expression, caret(new MathSequencePosition(rootPath.append(new RootRadicand()), 2))).selection());
        assertEquals(caret(new MathSequencePosition(rootPath.append(new RootRadicand()), 2)),
                editor.moveLeft(expression, caret(new MathSequencePosition(MathPath.ROOT, 1))).selection());
    }

    @Test
    void wholeRootSelectionDeletesCopiesAndPastesAsStructuralAtom() {
        var root = new MathRoot(sequence(new MathNamedOperator("sin"), identifier("x")), java.util.Optional.empty());
        var expression = sequence(identifier("a"), root, identifier("b"));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 1), new MathSequencePosition(MathPath.ROOT, 2));

        var fragment = editor.extractSelection(expression, selection).orElseThrow();
        var deleted = editor.deleteForward(expression, selection);
        var pasted = editor.pasteFragment(sequence(identifier("z")), caret(new MathSequencePosition(MathPath.ROOT, 1)), fragment);

        assertEquals(sequence(root), fragment);
        assertEquals(sequence(identifier("a"), identifier("b")), deleted.expression());
        assertEquals(sequence(identifier("z"), root), pasted.expression());
    }

    @Test
    void rootHitTestingResolvesRadicandAndDecorativeRegionsWithoutFakeTokenPositions() {
        var rootPath = MathPath.ROOT.append(new SequenceChild(0));
        var expression = sequence(new MathRoot(sequence(identifier("x")), java.util.Optional.empty()));
        var layout = new MathLayoutEngine().layout(expression, new FixedMathTextMeasurer());
        var hitTester = new MathHitTester();

        assertEquals(new MathSequencePosition(rootPath, 0), hitTester.hit(layout, 1, 0, new FixedMathTextMeasurer()));
        assertEquals(new MathSequencePosition(rootPath.append(new RootRadicand()), 0), hitTester.hit(layout, 10, 0, new FixedMathTextMeasurer()));
        assertEquals(new MathSequencePosition(rootPath.append(new RootRadicand()), 1), hitTester.hit(layout, 14, 0, new FixedMathTextMeasurer()));
    }

    @Test
    void scriptHitTestingResolvesBaseAndScriptSlots() {
        var scriptPath = MathPath.ROOT.append(new SequenceChild(0));
        var expression = sequence(new MathScript(identifier("x"), java.util.Optional.of(sequence(identifier("i"))), java.util.Optional.of(sequence(number("2")))));
        var layout = new MathLayoutEngine().layout(expression, new FixedMathTextMeasurer());
        var hitTester = new MathHitTester();

        assertEquals(new MathSequencePosition(scriptPath.append(new ScriptBase()), 0), hitTester.hit(layout, 2, 0, new FixedMathTextMeasurer()));
        assertEquals(new MathSequencePosition(scriptPath.append(new ScriptSubscript()), 0), hitTester.hit(layout, 7, 5, new FixedMathTextMeasurer()));
        assertEquals(new MathSequencePosition(scriptPath.append(new ScriptSuperscript()), 0), hitTester.hit(layout, 7, -5, new FixedMathTextMeasurer()));
    }

    @Test
    void emptyRootRadicandHitTestingUsesEditableSlotPath() {
        var rootPath = MathPath.ROOT.append(new SequenceChild(0));
        var expression = sequence(new MathRoot(new MathSequence(List.of()), java.util.Optional.empty()));
        var layout = new MathLayoutEngine().layout(expression, new FixedMathTextMeasurer());

        assertEquals(new MathSequencePosition(rootPath.append(new RootRadicand()), 0),
                new MathHitTester().hit(layout, 10, 0, new FixedMathTextMeasurer()));
    }

    @Test
    void shiftNavigationCreatesDirectionalRangeAndCanonicalizesCollapsedSelection() {
        var expression = sequence(identifier("x"), identifier("y"));
        var start = caret(new MathSequencePosition(MathPath.ROOT, 0));

        var selected = editor.extendRight(expression, start);
        assertEquals(new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 1)), selected);

        var collapsed = editor.extendLeft(expression, selected);
        assertEquals(start, collapsed);
    }

    @Test
    void plainNavigationCollapsesMathRangeToNormalizedStartOrEnd() {
        var expression = sequence(identifier("x"), identifier("y"));
        var backward = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 2), new MathSequencePosition(MathPath.ROOT, 0));

        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 0)), editor.moveLeft(expression, backward).selection());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 2)), editor.moveRight(expression, backward).selection());
    }

    @Test
    void rangeDeleteReplacesPartialTokenSelectionAndPreservesUnicodeBoundaries() {
        var expression = sequence(identifier("café"), identifier("Δx"));
        var tokenPath = MathPath.ROOT.append(new SequenceChild(0));
        var selection = new MathRangeSelection(new MathTokenPosition(tokenPath, 1), new MathTokenPosition(tokenPath, 3));

        var result = editor.deleteForward(expression, selection);

        assertEquals(sequence(identifier("cé"), identifier("Δx")), result.expression());
        assertEquals(caret(new MathTokenPosition(tokenPath, 1)), result.selection());
    }

    @Test
    void rangeDeleteRemovesMultipleAtomsAndLeavesCaretAtNormalizedStart() {
        var expression = sequence(identifier("a"), operator("+", MathOperatorRole.BINARY), identifier("b"), identifier("c"));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 1), new MathSequencePosition(MathPath.ROOT, 3));

        var result = editor.deleteForward(expression, selection);

        assertEquals(sequence(identifier("a"), identifier("c")), result.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), result.selection());
    }

    @Test
    void wholeFractionRangeDeleteRemovesSelectedFractionButCaretOnlyDeleteStillDoesNot() {
        var fraction = new MathFraction(identifier("x"), identifier("y"));
        var expression = sequence(identifier("a"), fraction, identifier("b"));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 1), new MathSequencePosition(MathPath.ROOT, 2));

        var deleted = editor.deleteForward(expression, selection);

        assertEquals(sequence(identifier("a"), identifier("b")), deleted.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), deleted.selection());
        assertFalse(editor.deleteForward(expression, caret(new MathSequencePosition(MathPath.ROOT, 1))).changed());
    }

    @Test
    void typingOverRangeIsAtomicReplacementAtNormalizedRangeStart() {
        var expression = sequence(identifier("a"), identifier("b"), identifier("c"));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 2), new MathSequencePosition(MathPath.ROOT, 0));

        var result = editor.insertText(expression, selection, "x");

        assertEquals(sequence(identifier("x"), identifier("c")), result.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), result.selection());
    }

    @Test
    void unsupportedTypingOverRangeDoesNotDeleteSelection() {
        var expression = sequence(identifier("a"), identifier("b"));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 2));

        var result = editor.insertText(expression, selection, " ");

        assertFalse(result.changed());
        assertEquals(expression, result.expression());
        assertEquals(selection, result.selection());
    }

    @Test
    void insertFractionOverRangeWrapsSelectedAtomsAsNumeratorAndMovesCaretToDenominator() {
        var expression = sequence(identifier("a"), operator("+", MathOperatorRole.BINARY), identifier("b"));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 3));

        var result = editor.insertFraction(expression, selection);

        assertEquals(sequence(new MathFraction(sequence(identifier("a"), operator("+", MathOperatorRole.BINARY), identifier("b")), new MathSequence(List.of()))), result.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(0)).append(new FractionDenominator()), 0)), result.selection());
    }

    @Test
    void insertFractionOverPartialTokenSplitsTokenAroundWrappedNumerator() {
        var expression = sequence(identifier("velocity"));
        var tokenPath = MathPath.ROOT.append(new SequenceChild(0));
        var selection = new MathRangeSelection(new MathTokenPosition(tokenPath, 3), new MathTokenPosition(tokenPath, 6));

        var result = editor.insertFraction(expression, selection);

        assertEquals(sequence(identifier("vel"), new MathFraction(sequence(identifier("oci")), new MathSequence(List.of())), identifier("ty")), result.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(1)).append(new FractionDenominator()), 0)), result.selection());
    }

    @Test
    void unsupportedCrossFractionInternalRangeIsValidatedButNotMutated() {
        var expression = sequence(new MathFraction(sequence(identifier("a")), sequence(identifier("b"))));
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));
        var selection = new MathRangeSelection(
                new MathSequencePosition(fractionPath.append(new FractionNumerator()), 0),
                new MathSequencePosition(fractionPath.append(new FractionDenominator()), 1));

        editor.validateSelection(expression, selection);
        assertFalse(editor.deleteForward(expression, selection).changed());
        assertFalse(editor.insertFraction(expression, selection).changed());
    }

    @Test
    void extractsCopyableMathSelectionsAsCanonicalSequenceFragments() {
        var expression = sequence(number("12345"), identifier("velocity"), new MathFraction(sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1")), identifier("y")));
        var numberPath = MathPath.ROOT.append(new SequenceChild(0));
        var identifierPath = MathPath.ROOT.append(new SequenceChild(1));

        assertEquals(sequence(number("23")), editor.extractSelection(
                expression,
                new MathRangeSelection(new MathTokenPosition(numberPath, 1), new MathTokenPosition(numberPath, 3))).orElseThrow());
        assertEquals(sequence(identifier("loc")), editor.extractSelection(
                expression,
                new MathRangeSelection(new MathTokenPosition(identifierPath, 2), new MathTokenPosition(identifierPath, 5))).orElseThrow());
        assertEquals(sequence(new MathFraction(sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1")), identifier("y"))), editor.extractSelection(
                expression,
                new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 2), new MathSequencePosition(MathPath.ROOT, 3))).orElseThrow());
    }

    @Test
    void extractsFractionSlotContentWithoutIncludingOuterFraction() {
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));
        var expression = sequence(new MathFraction(sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1")), identifier("y")));

        var fragment = editor.extractSelection(
                expression,
                new MathRangeSelection(
                        new MathSequencePosition(fractionPath.append(new FractionNumerator()), 0),
                        new MathSequencePosition(fractionPath.append(new FractionNumerator()), 3))).orElseThrow();

        assertEquals(sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1")), fragment);
    }

    @Test
    void extractsCompleteFractionInteriorSelectionAsWholeFraction() {
        var fraction = new MathFraction(sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1")), identifier("y"));
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));
        var expression = sequence(fraction);

        var fragment = editor.extractSelection(
                expression,
                new MathRangeSelection(
                        new MathSequencePosition(fractionPath.append(new FractionNumerator()), 0),
                        new MathSequencePosition(fractionPath.append(new FractionDenominator()), 1))).orElseThrow();

        assertEquals(sequence(fraction), fragment);
    }

    @Test
    void partialCrossSlotFractionInteriorSelectionRemainsUnextractable() {
        var fraction = new MathFraction(sequence(identifier("x"), identifier("y")), sequence(identifier("z")));
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));
        var expression = sequence(fraction);

        assertTrue(editor.extractSelection(
                expression,
                new MathRangeSelection(
                        new MathSequencePosition(fractionPath.append(new FractionNumerator()), 1),
                        new MathSequencePosition(fractionPath.append(new FractionDenominator()), 1))).isEmpty());
    }

    @Test
    void crossSlotRangesAreNotExtractableClipboardFragments() {
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));
        var expression = sequence(new MathFraction(sequence(identifier("a"), identifier("b")), sequence(identifier("c"))));

        assertTrue(editor.extractSelection(
                expression,
                new MathRangeSelection(
                        new MathSequencePosition(fractionPath.append(new FractionNumerator()), 1),
                        new MathSequencePosition(fractionPath.append(new FractionDenominator()), 1))).isEmpty());
    }

    @Test
    void pasteFragmentInsertsAtCaretsAndSplitsTokensWithoutNormalizing() {
        var fragment = sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1"));
        var empty = editor.pasteFragment(new MathSequence(List.of()), editor.rootStart(), fragment);
        var insideNumber = editor.pasteFragment(
                sequence(number("1234")),
                caret(new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 2)),
                fragment);
        var insideIdentifier = editor.pasteFragment(
                sequence(identifier("velocity")),
                caret(new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 3)),
                fragment);

        assertEquals(fragment, empty.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 3)), empty.selection());
        assertEquals(sequence(number("12"), identifier("x"), operator("+", MathOperatorRole.BINARY), number("1"), number("34")), insideNumber.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 4)), insideNumber.selection());
        assertEquals(sequence(identifier("vel"), identifier("x"), operator("+", MathOperatorRole.BINARY), number("1"), identifier("ocity")), insideIdentifier.expression());
    }

    @Test
    void pasteFragmentIntoFractionSlotsPreservesOuterFraction() {
        var expression = sequence(new MathFraction(identifier("n"), identifier("d")));
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));
        var fragment = sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1"));

        var numerator = editor.pasteFragment(
                expression,
                caret(new MathSequencePosition(fractionPath.append(new FractionNumerator()), 1)),
                fragment);
        var denominator = editor.pasteFragment(
                expression,
                caret(new MathSequencePosition(fractionPath.append(new FractionDenominator()), 0)),
                fragment);

        assertEquals(sequence(new MathFraction(sequence(identifier("n"), identifier("x"), operator("+", MathOperatorRole.BINARY), number("1")), identifier("d"))), numerator.expression());
        assertEquals(caret(new MathSequencePosition(fractionPath.append(new FractionNumerator()), 4)), numerator.selection());
        assertEquals(sequence(new MathFraction(identifier("n"), sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1"), identifier("d")))), denominator.expression());
        assertEquals(caret(new MathSequencePosition(fractionPath.append(new FractionDenominator()), 3)), denominator.selection());
    }

    @Test
    void pasteFragmentReplacesSupportedRangesAndRejectsUnsupportedCrossSlotRanges() {
        var expression = sequence(identifier("a"), new MathFraction(identifier("n"), identifier("d")), identifier("b"));
        var fragment = sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1"));
        var fractionPath = MathPath.ROOT.append(new SequenceChild(1));

        var overFraction = editor.pasteFragment(
                expression,
                new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 1), new MathSequencePosition(MathPath.ROOT, 2)),
                fragment);
        var crossSlot = editor.pasteFragment(
                expression,
                new MathRangeSelection(
                        new MathSequencePosition(fractionPath.append(new FractionNumerator()), 0),
                        new MathSequencePosition(fractionPath.append(new FractionDenominator()), 1)),
                fragment);

        assertEquals(sequence(identifier("a"), identifier("x"), operator("+", MathOperatorRole.BINARY), number("1"), identifier("b")), overFraction.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 4)), overFraction.selection());
        assertFalse(crossSlot.changed());
        assertEquals(expression, crossSlot.expression());
    }

    @Test
    void selectionGeometryHighlightsPartialTokensAndWholeFractionsFromLayoutMapping() {
        var expression = sequence(identifier("abcd"), new MathFraction(identifier("x"), identifier("y")));
        var layout = new MathLayoutEngine().layout(expression, new FixedMathTextMeasurer());
        var resolver = new MathSelectionGeometryResolver();
        var tokenPath = MathPath.ROOT.append(new SequenceChild(0));

        var tokenRects = resolver.resolve(
                expression,
                new MathRangeSelection(new MathTokenPosition(tokenPath, 1), new MathTokenPosition(tokenPath, 3)),
                layout,
                new FixedMathTextMeasurer());
        var fractionRects = resolver.resolve(
                expression,
                new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 1), new MathSequencePosition(MathPath.ROOT, 2)),
                layout,
                new FixedMathTextMeasurer());

        assertEquals(List.of(new MathSelectionRect(5, -7, 10, 10)), tokenRects);
        assertEquals(1, fractionRects.size());
        assertEquals(layout.root().children().get(1).box().width(), fractionRects.get(0).width());
        assertTrue(fractionRects.get(0).height() >= layout.root().children().get(1).box().ascent());
    }

    @Test
    void convertsEligibleSelectionToNamedOperatorAndMathText() {
        var expression = sequence(identifier("s"), identifier("i"), identifier("n"));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 3));

        var named = editor.convertSelectionToSemanticToken(expression, selection, SemanticMathTokenKind.NAMED_OPERATOR, "sin");
        var text = editor.convertSelectionToSemanticToken(expression, selection, SemanticMathTokenKind.MATH_TEXT, "for all");

        assertEquals(sequence(new MathNamedOperator("sin")), named.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), named.selection());
        assertEquals(sequence(new MathText("for all")), text.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), text.selection());
    }

    @Test
    void semanticConversionSupportsNumbersAndArbitraryOperatorNamesWithoutWhitelist() {
        var expression = sequence(identifier("r"), identifier("a"), identifier("n"), identifier("k"), number("2"));
        var selection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 5));

        assertEquals("rank2", editor.semanticTokenContent(expression, selection).orElseThrow());
        var converted = editor.convertSelectionToSemanticToken(expression, selection, SemanticMathTokenKind.NAMED_OPERATOR, "trace");

        assertEquals(sequence(new MathNamedOperator("trace")), converted.expression());
    }

    @Test
    void semanticConversionRejectsOperatorsDelimitersFractionsCrossSlotAndInvalidContent() {
        var operatorSelection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 3));
        var fractionSelection = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 1), new MathSequencePosition(MathPath.ROOT, 2));
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));
        var crossSlotSelection = new MathRangeSelection(
                new MathSequencePosition(fractionPath.append(new FractionNumerator()), 0),
                new MathSequencePosition(fractionPath.append(new FractionDenominator()), 1));

        assertTrue(editor.semanticTokenContent(sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), identifier("y")), operatorSelection).isEmpty());
        assertTrue(editor.semanticTokenContent(sequence(identifier("x"), new MathFraction(identifier("a"), identifier("b"))), fractionSelection).isEmpty());
        assertTrue(editor.semanticTokenContent(sequence(new MathFraction(identifier("a"), identifier("b"))), crossSlotSelection).isEmpty());
        assertFalse(editor.canConvertSelectionToSemanticToken(
                sequence(identifier("i"), identifier("f")),
                new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 2)),
                SemanticMathTokenKind.MATH_TEXT,
                " if"));
        assertFalse(editor.canConvertSelectionToSemanticToken(
                sequence(identifier("s")),
                new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 1)),
                SemanticMathTokenKind.NAMED_OPERATOR,
                " "));
    }

    @Test
    void editsExistingSemanticTokenByWholeTokenSelectionAndCanChangeType() {
        var expression = sequence(new MathNamedOperator("sin"), new MathText("if"));

        var namedToText = editor.convertSelectionToSemanticToken(
                expression,
                new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 1)),
                SemanticMathTokenKind.MATH_TEXT,
                "if");
        var textToNamed = editor.convertSelectionToSemanticToken(
                expression,
                new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 1), new MathSequencePosition(MathPath.ROOT, 2)),
                SemanticMathTokenKind.NAMED_OPERATOR,
                "rank");

        assertEquals(sequence(new MathText("if"), new MathText("if")), namedToText.expression());
        assertEquals(sequence(new MathNamedOperator("sin"), new MathNamedOperator("rank")), textToNamed.expression());
    }

    @Test
    void semanticTokensAreAtomicForHitTestingNavigationSelectionAndDeletion() {
        var expression = sequence(identifier("x"), new MathNamedOperator("sin"), new MathText("for all"), identifier("y"));
        var layout = new MathLayoutEngine().layout(expression, new FixedMathTextMeasurer());
        var hitTester = new MathHitTester();

        assertEquals(new MathSequencePosition(MathPath.ROOT, 1), hitTester.hit(layout, 8, 0, new FixedMathTextMeasurer()));
        assertEquals(new MathSequencePosition(MathPath.ROOT, 2), hitTester.hit(layout, 20, 0, new FixedMathTextMeasurer()));
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 2)), editor.moveRight(expression, caret(new MathSequencePosition(MathPath.ROOT, 1))).selection());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), editor.moveLeft(expression, caret(new MathSequencePosition(MathPath.ROOT, 2))).selection());
        assertEquals(new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 1), new MathSequencePosition(MathPath.ROOT, 2)),
                editor.extendRight(expression, caret(new MathSequencePosition(MathPath.ROOT, 1))));
        assertEquals(sequence(identifier("x"), new MathText("for all"), identifier("y")),
                editor.deleteForward(expression, caret(new MathSequencePosition(MathPath.ROOT, 1))).expression());
        assertEquals(sequence(identifier("x"), new MathText("for all"), identifier("y")),
                editor.deleteBackward(expression, caret(new MathSequencePosition(MathPath.ROOT, 2))).expression());
    }

    @Test
    void insertsEmptyGroupsForAllDelimiterKindsAndPlacesCaretInsideContent() {
        var selection = caret(new MathSequencePosition(MathPath.ROOT, 0));

        var parentheses = editor.insertGroup(sequence(), selection, MathDelimiter.PARENTHESES);
        var brackets = editor.insertGroup(sequence(), selection, MathDelimiter.BRACKETS);
        var braces = editor.insertGroup(sequence(), selection, MathDelimiter.BRACES);

        assertEquals(sequence(new MathGroup(sequence(), MathDelimiter.PARENTHESES)), parentheses.expression());
        assertEquals(sequence(new MathGroup(sequence(), MathDelimiter.BRACKETS)), brackets.expression());
        assertEquals(sequence(new MathGroup(sequence(), MathDelimiter.BRACES)), braces.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(0)).append(new GroupContent()), 0)), parentheses.selection());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(0)).append(new GroupContent()), 0)), brackets.selection());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(0)).append(new GroupContent()), 0)), braces.selection());
    }

    @Test
    void groupContentPathSupportsEditingReplacementAndNesting() {
        var groupPath = MathPath.ROOT.append(new SequenceChild(0)).append(new GroupContent());
        var expression = sequence(new MathGroup(sequence(new MathGroup(sequence(identifier("x")), MathDelimiter.BRACKETS)), MathDelimiter.PARENTHESES));
        var nestedPath = groupPath.append(new SequenceChild(0)).append(new GroupContent());

        var inserted = editor.insertText(expression, caret(new MathSequencePosition(nestedPath, 1)), "+");
        var afterNestedContent = editor.moveRight(inserted.expression(), caret(new MathSequencePosition(nestedPath, 2)));

        assertEquals(sequence(new MathGroup(sequence(new MathGroup(sequence(identifier("x"), operator("+", MathOperatorRole.BINARY)), MathDelimiter.BRACKETS)), MathDelimiter.PARENTHESES)), inserted.expression());
        assertEquals(caret(new MathSequencePosition(groupPath, 1)), afterNestedContent.selection());
    }

    @Test
    void wrapsCompleteSingleMultiAndExistingGroupSelections() {
        var expression = sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1"));
        var range = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 3));
        var single = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 1));
        var existingGroup = sequence(new MathGroup(sequence(identifier("x")), MathDelimiter.PARENTHESES));
        var existingGroupRange = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 1));

        var grouped = editor.insertGroup(expression, range, MathDelimiter.PARENTHESES);
        var groupedSingle = editor.insertGroup(expression, single, MathDelimiter.BRACKETS);
        var nested = editor.insertGroup(existingGroup, existingGroupRange, MathDelimiter.BRACES);

        assertEquals(sequence(new MathGroup(sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1")), MathDelimiter.PARENTHESES)), grouped.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)), grouped.selection());
        assertEquals(sequence(new MathGroup(sequence(identifier("x")), MathDelimiter.BRACKETS), operator("+", MathOperatorRole.BINARY), number("1")), groupedSingle.expression());
        assertEquals(sequence(new MathGroup(sequence(new MathGroup(sequence(identifier("x")), MathDelimiter.PARENTHESES)), MathDelimiter.BRACES)), nested.expression());
    }

    @Test
    void groupWrappingRejectsPartialTokenAndCrossSlotRanges() {
        var partialToken = new MathRangeSelection(
                new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 1),
                new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 2));
        var fraction = sequence(new MathFraction(sequence(identifier("a")), sequence(identifier("b"))));
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));
        var crossSlot = new MathRangeSelection(
                new MathSequencePosition(fractionPath.append(new FractionNumerator()), 0),
                new MathSequencePosition(fractionPath.append(new FractionDenominator()), 1));

        assertFalse(editor.insertGroup(sequence(identifier("abc")), partialToken, MathDelimiter.PARENTHESES).changed());
        assertFalse(editor.insertGroup(fraction, crossSlot, MathDelimiter.PARENTHESES).changed());
    }

    @Test
    void groupNavigationAndParentAdjacentDeletionEnterContent() {
        var expression = sequence(new MathGroup(sequence(identifier("x"), number("1")), MathDelimiter.PARENTHESES));
        var groupPath = MathPath.ROOT.append(new SequenceChild(0));
        var contentPath = groupPath.append(new GroupContent());

        assertEquals(caret(new MathSequencePosition(contentPath, 0)),
                editor.moveRight(expression, caret(new MathSequencePosition(MathPath.ROOT, 0))).selection());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 0)),
                editor.moveLeft(expression, caret(new MathSequencePosition(contentPath, 0))).selection());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 1)),
                editor.moveRight(expression, caret(new MathSequencePosition(contentPath, 2))).selection());
        assertEquals(caret(new MathSequencePosition(contentPath, 2)),
                editor.moveLeft(expression, caret(new MathSequencePosition(MathPath.ROOT, 1))).selection());
        assertEquals(caret(new MathSequencePosition(contentPath, 2)),
                editor.deleteBackward(expression, caret(new MathSequencePosition(MathPath.ROOT, 1))).selection());
        assertEquals(caret(new MathSequencePosition(contentPath, 0)),
                editor.deleteForward(expression, caret(new MathSequencePosition(MathPath.ROOT, 0))).selection());
    }

    @Test
    void emptyGroupBackspaceRemovesTemplateButContentDeletionLeavesGroupEmpty() {
        var groupPath = MathPath.ROOT.append(new SequenceChild(0));
        var contentPath = groupPath.append(new GroupContent());
        var emptyGroup = sequence(new MathGroup(sequence(), MathDelimiter.PARENTHESES), identifier("y"));
        var nonEmptyGroup = sequence(new MathGroup(sequence(identifier("x")), MathDelimiter.PARENTHESES), identifier("y"));

        var removed = editor.deleteBackward(emptyGroup, caret(new MathSequencePosition(contentPath, 0)));
        var emptied = editor.deleteSelection(nonEmptyGroup, new MathRangeSelection(
                new MathSequencePosition(contentPath, 0),
                new MathSequencePosition(contentPath, 1)));

        assertEquals(sequence(identifier("y")), removed.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT, 0)), removed.selection());
        assertEquals(sequence(new MathGroup(sequence(), MathDelimiter.PARENTHESES), identifier("y")), emptied.expression());
        assertEquals(caret(new MathSequencePosition(contentPath, 0)), emptied.selection());
    }

    @Test
    void wholeGroupSelectionDeletesGroupAndGroupIsScriptableSingleAtom() {
        var grouped = sequence(new MathGroup(sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1")), MathDelimiter.PARENTHESES), identifier("y"));
        var wholeGroup = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 1));

        var deleted = editor.deleteSelection(grouped, wholeGroup);
        var scripted = editor.insertScriptSlot(grouped, caret(new MathSequencePosition(MathPath.ROOT, 1)), ScriptSlot.SUPERSCRIPT);
        var rawMultiAtom = editor.insertScriptSlot(
                sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1")),
                new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 3)),
                ScriptSlot.SUPERSCRIPT);

        assertEquals(sequence(identifier("y")), deleted.expression());
        assertEquals(sequence(new MathScript(new MathGroup(sequence(identifier("x"), operator("+", MathOperatorRole.BINARY), number("1")), MathDelimiter.PARENTHESES), java.util.Optional.empty(), java.util.Optional.of(sequence())), identifier("y")), scripted.expression());
        assertEquals(caret(new MathSequencePosition(MathPath.ROOT.append(new SequenceChild(0)).append(new ScriptSuperscript()), 0)), scripted.selection());
        assertFalse(rawMultiAtom.changed());
    }

    private static MathCaretSelection caret(MathPosition position) {
        return new MathCaretSelection(position);
    }

    private static MathSequence sequence(MathExpression... expressions) {
        return new MathSequence(List.of(expressions));
    }

    private static MathNumber number(String value) {
        return new MathNumber(value);
    }

    private static MathIdentifier identifier(String value) {
        return new MathIdentifier(value);
    }

    private static MathFraction emptyFraction() {
        return new MathFraction(new MathSequence(List.of()), new MathSequence(List.of()));
    }

    private static MathRoot emptyRoot() {
        return new MathRoot(new MathSequence(List.of()), java.util.Optional.empty());
    }

    private static MathOperator operator(String symbol, MathOperatorRole role) {
        return new MathOperator(symbol, role);
    }

    private static final class FixedMathTextMeasurer implements MathTextMeasurer {
        @Override
        public MathTextMetrics measureText(String content, MathTextKind kind) {
            return new MathTextMetrics(content.length() * 5, 7, 3);
        }
    }
}
