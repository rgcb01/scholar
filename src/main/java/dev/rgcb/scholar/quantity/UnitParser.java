package dev.rgcb.scholar.quantity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class UnitParser {
    private final UnitRegistry registry;
    public UnitParser() { this(UnitRegistry.builtIn()); }
    public UnitParser(UnitRegistry registry) { this.registry = Objects.requireNonNull(registry); }

    public UnitParseResult parse(String input) {
        try {
            var parser = new Parser(Objects.requireNonNull(input, "input").trim().replace('μ', 'µ'));
            var factors = parser.expression(1, false);
            parser.skipWhitespace();
            if (!parser.end()) throw parser.error("unexpected input");
            var expression = new UnitExpression(combine(factors));
            validateAffine(expression);
            return new UnitParseResult.Success(expression);
        } catch (ParseFailure failure) {
            return new UnitParseResult.Failure(failure.getMessage(), failure.offset);
        } catch (IllegalArgumentException failure) {
            return new UnitParseResult.Failure(failure.getMessage(), 0);
        }
    }

    public UnitExpression parseRequired(String input) {
        return switch (parse(input)) {
            case UnitParseResult.Success success -> success.expression();
            case UnitParseResult.Failure failure -> throw new IllegalArgumentException(failure.message() + " at " + failure.offset());
        };
    }

    private void validateAffine(UnitExpression expression) {
        if (expression.factors().stream().anyMatch(f -> registry.unit(f).affine())
                && registry.simpleUnit(expression).isEmpty()) {
            throw new IllegalArgumentException("affine units may not be prefixed, powered, or compounded");
        }
    }

    private static List<UnitFactor> combine(List<UnitFactor> input) {
        var result = new ArrayList<UnitFactor>();
        for (var factor : input) {
            var index = -1;
            for (var i = 0; i < result.size(); i++) {
                var existing = result.get(i);
                if (existing.unitId().equals(factor.unitId()) && existing.prefix().equals(factor.prefix())) { index = i; break; }
            }
            if (index < 0) result.add(factor);
            else {
                var existing = result.remove(index);
                var exponent = existing.exponent() + factor.exponent();
                if (exponent != 0) result.add(index, new UnitFactor(existing.unitId(), existing.prefix(), exponent));
            }
        }
        if (result.isEmpty()) throw new IllegalArgumentException("dimensionless unit expressions must use an explicit unit");
        return List.copyOf(result);
    }

    private final class Parser {
        private final String text; private int offset;
        private Parser(String text) { this.text = text; }
        private List<UnitFactor> expression(int sign, boolean parenthesized) {
            var result = new ArrayList<UnitFactor>(); var nextSign = sign; var consumed = false;
            while (true) {
                skipWhitespace();
                if (end() || parenthesized && peek() == ')') break;
                if (consumed) {
                    var separator = peek();
                    if (separator != '*' && separator != '·' && separator != '/') throw error("expected *, ·, or /");
                    offset++; nextSign = separator == '/' ? -sign : sign; skipWhitespace();
                }
                if (!end() && peek() == '(') {
                    offset++; result.addAll(expression(nextSign, true)); skipWhitespace();
                    if (end() || peek() != ')') throw error("missing )"); offset++;
                } else {
                    var start = offset;
                    while (!end() && !Character.isWhitespace(peek()) && "*·/^()".indexOf(peek()) < 0 && !isSuperscript(peek())) offset++;
                    if (start == offset) throw error("expected unit symbol");
                    var symbol = text.substring(start, offset);
                    var exponent = exponent();
                    try { result.add(registry.factorForSymbol(symbol, nextSign * exponent)); }
                    catch (IllegalArgumentException failure) { throw new ParseFailure(failure.getMessage(), start); }
                }
                consumed = true;
            }
            if (!consumed) throw error("expected unit expression");
            return result;
        }
        private int exponent() {
            if (end()) return 1;
            if (peek() == '^') {
                offset++; var start = offset; if (!end() && peek() == '-') offset++;
                while (!end() && Character.isDigit(peek())) offset++;
                if (start == offset || text.substring(start, offset).equals("-")) throw error("expected integer exponent");
                return Integer.parseInt(text.substring(start, offset));
            }
            if (isSuperscript(peek())) {
                var value = 0;
                while (!end() && isSuperscript(peek())) value = value * 10 + superscriptValue(text.charAt(offset++));
                return value;
            }
            return 1;
        }
        private void skipWhitespace() { while (!end() && Character.isWhitespace(peek())) offset++; }
        private boolean end() { return offset >= text.length(); }
        private char peek() { return text.charAt(offset); }
        private ParseFailure error(String message) { return new ParseFailure(message, offset); }
    }

    private static boolean isSuperscript(char value) { return "⁰¹²³⁴⁵⁶⁷⁸⁹".indexOf(value) >= 0; }
    private static int superscriptValue(char value) { return "⁰¹²³⁴⁵⁶⁷⁸⁹".indexOf(value); }
    private static final class ParseFailure extends RuntimeException {
        private final int offset; private ParseFailure(String message, int offset) { super(message); this.offset = offset; }
    }
}
