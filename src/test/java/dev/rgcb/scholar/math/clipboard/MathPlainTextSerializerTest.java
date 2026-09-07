package dev.rgcb.scholar.math.clipboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import dev.rgcb.scholar.math.MathSymbol;
import dev.rgcb.scholar.math.MathSymbolKind;
import dev.rgcb.scholar.math.MathText;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MathPlainTextSerializerTest {
    private final MathPlainTextSerializer serializer = new MathPlainTextSerializer();

    @Test
    void serializesLinearSequencesWithReadableOperatorSpacing() {
        assertEquals("x + 1", serializer.serialize(sequence(identifier("x"), operator("+"), number("1"))).orElseThrow());
        assertEquals("x = 12.5", serializer.serialize(sequence(identifier("x"), new MathOperator("=", MathOperatorRole.RELATION), number("12.5"))).orElseThrow());
        assertEquals("a / b", serializer.serialize(sequence(identifier("a"), operator("/"), identifier("b"))).orElseThrow());
    }

    @Test
    void serializesFractionsWithDeterministicParentheses() {
        assertEquals("a / b", serializer.serialize(new MathFraction(identifier("a"), identifier("b"))).orElseThrow());
        assertEquals("(x + 1) / y", serializer.serialize(new MathFraction(sequence(identifier("x"), operator("+"), number("1")), identifier("y"))).orElseThrow());
        assertEquals("x / (y + 1)", serializer.serialize(new MathFraction(identifier("x"), sequence(identifier("y"), operator("+"), number("1")))).orElseThrow());
        assertEquals("(a / b) / c", serializer.serialize(new MathFraction(new MathFraction(identifier("a"), identifier("b")), identifier("c"))).orElseThrow());
    }

    @Test
    void serializesSymbolsAndRejectsUnsupportedNodes() {
        assertEquals("θ + Δ", serializer.serialize(sequence(
                new MathSymbol("θ", MathSymbolKind.GREEK),
                operator("+"),
                new MathSymbol("Δ", MathSymbolKind.GREEK))).orElseThrow());
    }

    @Test
    void serializesSquareRootsAndProgrammaticIndexedRoots() {
        assertEquals("sqrt(x)", serializer.serialize(new MathRoot(identifier("x"), Optional.empty())).orElseThrow());
        assertEquals("sqrt((x + 1))", serializer.serialize(new MathRoot(sequence(identifier("x"), operator("+"), number("1")), Optional.empty())).orElseThrow());
        assertEquals("sqrt(a / b)", serializer.serialize(new MathRoot(new MathFraction(identifier("a"), identifier("b")), Optional.empty())).orElseThrow());
        assertEquals("root(3, x)", serializer.serialize(new MathRoot(identifier("x"), Optional.of(number("3")))).orElseThrow());
    }

    @Test
    void serializesNamedOperatorsAndMathTextAsReadableFallbackText() {
        assertEquals("sin", serializer.serialize(new MathNamedOperator("sin")).orElseThrow());
        assertEquals("rank", serializer.serialize(new MathNamedOperator("rank")).orElseThrow());
        assertEquals("if", serializer.serialize(new MathText("if")).orElseThrow());
        assertEquals("for all", serializer.serialize(new MathText("for all")).orElseThrow());
        assertEquals("sin ( x ) if x = 0", serializer.serialize(sequence(
                new MathNamedOperator("sin"),
                new MathSymbol("(", MathSymbolKind.OTHER),
                identifier("x"),
                new MathSymbol(")", MathSymbolKind.OTHER),
                new MathText("if"),
                identifier("x"),
                new MathOperator("=", MathOperatorRole.RELATION),
                number("0"))).orElseThrow());
    }

    @Test
    void serializesSuperscriptsAndSubscriptsWithReadableGrouping() {
        assertEquals("x^2", serializer.serialize(new MathScript(identifier("x"), Optional.empty(), Optional.of(number("2")))).orElseThrow());
        assertEquals("x_i", serializer.serialize(new MathScript(identifier("x"), Optional.of(identifier("i")), Optional.empty())).orElseThrow());
        assertEquals("x_i^2", serializer.serialize(new MathScript(identifier("x"), Optional.of(identifier("i")), Optional.of(number("2")))).orElseThrow());
        assertEquals("x^(a + b)", serializer.serialize(new MathScript(identifier("x"), Optional.empty(), Optional.of(sequence(identifier("a"), operator("+"), identifier("b"))))).orElseThrow());
        assertEquals("x_(i + 1)", serializer.serialize(new MathScript(identifier("x"), Optional.of(sequence(identifier("i"), operator("+"), number("1"))), Optional.empty())).orElseThrow());
        assertEquals("sqrt(x)^2", serializer.serialize(new MathScript(new MathRoot(identifier("x"), Optional.empty()), Optional.empty(), Optional.of(number("2")))).orElseThrow());
        assertEquals("(a / b)^2", serializer.serialize(new MathScript(new MathFraction(identifier("a"), identifier("b")), Optional.empty(), Optional.of(number("2")))).orElseThrow());
    }

    @Test
    void serializesStructuralGroupsWithTheirAuthoredDelimiters() {
        assertEquals("(x + 1)", serializer.serialize(new MathGroup(
                sequence(identifier("x"), operator("+"), number("1")), MathDelimiter.PARENTHESES)).orElseThrow());
        assertEquals("[x]", serializer.serialize(new MathGroup(identifier("x"), MathDelimiter.BRACKETS)).orElseThrow());
        assertEquals("{}", serializer.serialize(new MathGroup(sequence(), MathDelimiter.BRACES)).orElseThrow());
        assertEquals("(x + 1)^2", serializer.serialize(new MathScript(
                new MathGroup(sequence(identifier("x"), operator("+"), number("1")), MathDelimiter.PARENTHESES),
                Optional.empty(),
                Optional.of(number("2")))).orElseThrow());
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
}
