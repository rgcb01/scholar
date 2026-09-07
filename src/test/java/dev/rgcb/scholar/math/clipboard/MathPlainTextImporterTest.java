package dev.rgcb.scholar.math.clipboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import dev.rgcb.scholar.math.MathExpression;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathOperatorRole;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.MathSymbol;
import dev.rgcb.scholar.math.MathSymbolKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class MathPlainTextImporterTest {
    private final MathPlainTextImporter importer = new MathPlainTextImporter();

    @Test
    void importsOrdinaryLettersAsIndividualIdentifierAtoms() {
        assertImports("x", sequence(identifier("x")));
        assertImports("velocity", sequence(
                identifier("v"),
                identifier("e"),
                identifier("l"),
                identifier("o"),
                identifier("c"),
                identifier("i"),
                identifier("t"),
                identifier("y")));
        assertImports("xy", sequence(identifier("x"), identifier("y")));
        assertImports("Δx", sequence(identifier("Δ"), identifier("x")));
        assertImports("θ + λ", sequence(identifier("θ"), operator("+"), identifier("λ")));
        assertImports("café", sequence(identifier("c"), identifier("a"), identifier("f"), identifier("é")));
        assertImports("e\u0301", sequence(identifier("e\u0301")));
    }

    @Test
    void importsLinearNotationWithoutImplicitStructure() {
        assertImports("2x", sequence(number("2"), identifier("x")));
        assertImports("x+1", sequence(identifier("x"), operator("+"), number("1")));
        assertImports("x + 1", sequence(identifier("x"), operator("+"), number("1")));
        assertImports("x/y", sequence(identifier("x"), operator("/"), identifier("y")));
        assertImports("(x+1)/y", sequence(
                symbol("("),
                identifier("x"),
                operator("+"),
                number("1"),
                symbol(")"),
                operator("/"),
                identifier("y")));
        assertImports("sin(x)", sequence(identifier("s"), identifier("i"), identifier("n"), symbol("("), identifier("x"), symbol(")")));
    }

    @Test
    void importsSimpleNumbersAndSignedFormsAsOperatorsPlusNumbers() {
        assertImports("3.14", sequence(number("3.14")));
        assertImports(".14", sequence(number(".14")));
        assertImports("3.", sequence(number("3.")));
        assertImports("-3", sequence(operator("-"), number("3")));
        assertImports("+3", sequence(operator("+"), number("3")));
    }

    @Test
    void whitespaceSeparatesTokensAndIsNotPersisted() {
        assertImports("  x  ", sequence(identifier("x")));
        assertImports("x   +   1", sequence(identifier("x"), operator("+"), number("1")));
        assertImports("mass velocity", sequence(
                identifier("m"),
                identifier("a"),
                identifier("s"),
                identifier("s"),
                identifier("v"),
                identifier("e"),
                identifier("l"),
                identifier("o"),
                identifier("c"),
                identifier("i"),
                identifier("t"),
                identifier("y")));
        assertImports("12 34", sequence(number("12"), number("34")));
        assertImports("x\t+\n1", sequence(identifier("x"), operator("+"), number("1")));
    }

    @Test
    void normalizesOnlyApprovedUnicodeOperators() {
        assertImports("Δx − θ", sequence(identifier("Δ"), identifier("x"), operator("-"), identifier("θ")));
        assertImports("a×b", sequence(identifier("a"), operator("*"), identifier("b")));
        assertImports("a÷b", sequence(identifier("a"), operator("/"), identifier("b")));
    }

    @Test
    void rejectsUnsupportedOrDeferredSyntaxAtomically() {
        assertFails("", MathImportError.EMPTY);
        assertFails("   \n\t", MathImportError.EMPTY);
        assertFails("x^2", MathImportError.UNSUPPORTED_CHARACTER);
        assertFails("x_1", MathImportError.UNSUPPORTED_CHARACTER);
        assertFails("x@y", MathImportError.UNSUPPORTED_CHARACTER);
        assertFails("3,14", MathImportError.UNSUPPORTED_CHARACTER);
        assertFails("1e6", MathImportError.UNSUPPORTED_SYNTAX);
        assertFails("1E-3", MathImportError.UNSUPPORTED_SYNTAX);
        assertFails("6.022e23", MathImportError.UNSUPPORTED_SYNTAX);
        assertFails("😀", MathImportError.UNSUPPORTED_CHARACTER);
    }

    @Test
    void enforcesInputAndTokenBoundsDeterministically() {
        assertImports(" ".repeat(MathPlainTextImporter.MAX_INPUT_LENGTH - 1) + "x", sequence(identifier("x")));
        assertFails(" ".repeat(MathPlainTextImporter.MAX_INPUT_LENGTH) + "x", MathImportError.TOO_LARGE);

        assertImports(tokens(MathPlainTextImporter.MAX_TOKENS), sequence(repeatedIdentifiers(MathPlainTextImporter.MAX_TOKENS)));
        assertFails(tokens(MathPlainTextImporter.MAX_TOKENS + 1), MathImportError.TOO_MANY_TOKENS);
    }

    private void assertImports(String text, MathSequence expected) {
        var success = assertInstanceOf(MathPlainTextImportResult.Success.class, importer.importText(text));
        assertEquals(expected, success.fragment());
    }

    private void assertFails(String text, MathImportError expected) {
        var failure = assertInstanceOf(MathPlainTextImportResult.Failure.class, importer.importText(text));
        assertEquals(expected, failure.error());
    }

    private static String tokens(int count) {
        return "x ".repeat(count).stripTrailing();
    }

    private static MathExpression[] repeatedIdentifiers(int count) {
        var expressions = new MathExpression[count];
        for (var index = 0; index < count; index++) {
            expressions[index] = identifier("x");
        }
        return expressions;
    }

    private static MathSequence sequence(MathExpression... expressions) {
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

    private static MathSymbol symbol(String symbol) {
        return new MathSymbol(symbol, MathSymbolKind.OTHER);
    }
}
