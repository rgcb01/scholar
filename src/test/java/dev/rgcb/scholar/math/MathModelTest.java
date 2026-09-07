package dev.rgcb.scholar.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class MathModelTest {
    @Test
    void representsFirstTargetEquationWithStructuralFraction() {
        var numerator = new MathSequence(List.of(
                new MathSymbol("Δ", MathSymbolKind.GREEK),
                new MathIdentifier("x")));
        var denominator = new MathSequence(List.of(
                new MathSymbol("Δ", MathSymbolKind.GREEK),
                new MathIdentifier("t")));
        var fraction = new MathFraction(numerator, denominator);

        var equation = new MathSequence(List.of(
                new MathIdentifier("v"),
                new MathOperator("=", MathOperatorRole.RELATION),
                fraction));

        assertEquals(new MathIdentifier("v"), equation.expressions().get(0));
        assertEquals(new MathOperator("=", MathOperatorRole.RELATION), equation.expressions().get(1));
        assertEquals(fraction, equation.expressions().get(2));
        assertEquals(numerator, fraction.numerator());
        assertEquals(denominator, fraction.denominator());
    }

    @Test
    void representsSuperscriptSubscriptAndCombinedScripts() {
        var xSquared = new MathScript(
                new MathIdentifier("x"),
                Optional.empty(),
                Optional.of(new MathNumber("2")));
        var vZero = new MathScript(
                new MathIdentifier("v"),
                Optional.of(new MathNumber("0")),
                Optional.empty());
        var xiSquared = new MathScript(
                new MathIdentifier("x"),
                Optional.of(new MathIdentifier("i")),
                Optional.of(new MathNumber("2")));

        assertEquals(Optional.of(new MathNumber("2")), xSquared.superscript());
        assertEquals(Optional.of(new MathNumber("0")), vZero.subscript());
        assertEquals(Optional.of(new MathIdentifier("i")), xiSquared.subscript());
        assertEquals(Optional.of(new MathNumber("2")), xiSquared.superscript());
    }

    @Test
    void rejectsScriptWithoutSubscriptOrSuperscript() {
        assertThrows(IllegalArgumentException.class, () -> new MathScript(
                new MathIdentifier("x"),
                Optional.empty(),
                Optional.empty()));
    }

    @Test
    void representsSquareAndIndexedRoots() {
        var squareRoot = new MathRoot(new MathIdentifier("x"), Optional.empty());
        var indexedRoot = new MathRoot(new MathIdentifier("x"), Optional.of(new MathNumber("3")));

        assertEquals(new MathIdentifier("x"), squareRoot.radicand());
        assertTrue(squareRoot.index().isEmpty());
        assertEquals(Optional.of(new MathNumber("3")), indexedRoot.index());
    }

    @Test
    void representsExplicitGrouping() {
        var content = new MathSequence(List.of(
                new MathIdentifier("x"),
                new MathOperator("+", MathOperatorRole.BINARY),
                new MathIdentifier("y")));

        var group = new MathGroup(content, MathDelimiter.PARENTHESES);

        assertEquals(content, group.content());
        assertEquals(MathDelimiter.PARENTHESES, group.delimiter());
    }

    @Test
    void representsNestedFractions() {
        var inner = new MathFraction(new MathIdentifier("a"), new MathIdentifier("b"));
        var outer = new MathFraction(inner, new MathIdentifier("c"));

        assertEquals(inner, outer.numerator());
        assertEquals(new MathIdentifier("c"), outer.denominator());
    }

    @Test
    void defensivelyCopiesMathSequenceExpressions() {
        var expressions = new ArrayList<MathExpression>();
        expressions.add(new MathIdentifier("x"));

        var sequence = new MathSequence(expressions);
        expressions.add(new MathIdentifier("y"));

        assertEquals(List.of(new MathIdentifier("x")), sequence.expressions());
        assertThrows(UnsupportedOperationException.class, () -> sequence.expressions().add(new MathIdentifier("z")));
    }

    @Test
    void rejectsNullRequiredValuesAndCollectionElements() {
        assertThrows(NullPointerException.class, () -> new MathSequence(null));
        assertThrows(NullPointerException.class, () -> new MathSequence(List.of((MathExpression) null)));
        assertThrows(NullPointerException.class, () -> new MathNumber(null));
        assertThrows(NullPointerException.class, () -> new MathIdentifier(null));
        assertThrows(NullPointerException.class, () -> new MathNamedOperator(null));
        assertThrows(NullPointerException.class, () -> new MathText(null));
        assertThrows(NullPointerException.class, () -> new MathSymbol(null, MathSymbolKind.GREEK));
        assertThrows(NullPointerException.class, () -> new MathSymbol("Delta", null));
        assertThrows(NullPointerException.class, () -> new MathOperator(null, MathOperatorRole.RELATION));
        assertThrows(NullPointerException.class, () -> new MathOperator("=", null));
        assertThrows(NullPointerException.class, () -> new MathFraction(null, new MathIdentifier("x")));
        assertThrows(NullPointerException.class, () -> new MathFraction(new MathIdentifier("x"), null));
        assertThrows(NullPointerException.class, () -> new MathScript(null, Optional.empty(), Optional.of(new MathNumber("2"))));
        assertThrows(NullPointerException.class, () -> new MathScript(new MathIdentifier("x"), null, Optional.empty()));
        assertThrows(NullPointerException.class, () -> new MathScript(new MathIdentifier("x"), Optional.empty(), null));
        assertThrows(NullPointerException.class, () -> new MathRoot(null, Optional.empty()));
        assertThrows(NullPointerException.class, () -> new MathRoot(new MathIdentifier("x"), null));
        assertThrows(NullPointerException.class, () -> new MathGroup(null, MathDelimiter.PARENTHESES));
        assertThrows(NullPointerException.class, () -> new MathGroup(new MathIdentifier("x"), null));
    }

    @Test
    void rejectsBlankTextualValues() {
        assertThrows(IllegalArgumentException.class, () -> new MathNumber(""));
        assertThrows(IllegalArgumentException.class, () -> new MathIdentifier(" "));
        assertThrows(IllegalArgumentException.class, () -> new MathNamedOperator(""));
        assertThrows(IllegalArgumentException.class, () -> new MathText(" "));
        assertThrows(IllegalArgumentException.class, () -> new MathText(" if"));
        assertThrows(IllegalArgumentException.class, () -> new MathText("if "));
        assertThrows(IllegalArgumentException.class, () -> new MathSymbol("", MathSymbolKind.OTHER));
        assertThrows(IllegalArgumentException.class, () -> new MathOperator(" ", MathOperatorRole.OTHER));
    }

    @Test
    void preservesStructuralEqualityForValueNodes() {
        assertEquals(new MathIdentifier("velocity"), new MathIdentifier("velocity"));
        assertEquals(new MathNamedOperator("rank"), new MathNamedOperator("rank"));
        assertEquals(new MathText("for all"), new MathText("for all"));
        assertEquals(new MathSymbol("Δ", MathSymbolKind.GREEK), new MathSymbol("Δ", MathSymbolKind.GREEK));
        assertEquals(new MathOperator("=", MathOperatorRole.RELATION), new MathOperator("=", MathOperatorRole.RELATION));
    }

    @Test
    void representsNamedOperatorsAndMathTextAsExplicitSemanticTokens() {
        var expression = new MathSequence(List.of(
                new MathNamedOperator("sin"),
                new MathIdentifier("x"),
                new MathText("if"),
                new MathText("for all"),
                new MathNamedOperator("∇dot")));

        assertEquals(new MathNamedOperator("sin"), expression.expressions().get(0));
        assertEquals(new MathIdentifier("x"), expression.expressions().get(1));
        assertEquals(new MathText("if"), expression.expressions().get(2));
        assertEquals(new MathText("for all"), expression.expressions().get(3));
        assertEquals(new MathNamedOperator("∇dot"), expression.expressions().get(4));
    }

    @Test
    void scriptsCanUseNamedOperatorsAndMathTextAsBases() {
        var sinSquared = new MathScript(
                new MathNamedOperator("sin"),
                Optional.empty(),
                Optional.of(new MathNumber("2")));
        var textLabel = new MathScript(
                new MathText("if"),
                Optional.of(new MathIdentifier("n")),
                Optional.empty());

        assertEquals(new MathNamedOperator("sin"), sinSquared.base());
        assertEquals(Optional.of(new MathNumber("2")), sinSquared.superscript());
        assertEquals(new MathText("if"), textLabel.base());
        assertEquals(Optional.of(new MathIdentifier("n")), textLabel.subscript());
    }
}
