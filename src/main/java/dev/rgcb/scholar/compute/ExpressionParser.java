package dev.rgcb.scholar.compute;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.VariableDefinition;
import dev.rgcb.scholar.document.VariableDependencyReference;
import dev.rgcb.scholar.quantity.Quantity;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Controlled authoring parser. Resolved operands immediately bind to stable IDs. */
public final class ExpressionParser {
    public record ParseResult(Expression expression, List<ComputationDiagnostic> diagnostics) {
        public ParseResult { diagnostics = List.copyOf(diagnostics); }
    }

    public ParseResult parse(String source, Document document) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(document);
        var parser = new Parser(source, document);
        try {
            var expression = parser.additive();
            parser.spaces();
            if (!parser.end()) throw parser.error("Unexpected input");
            return new ParseResult(expression, parser.diagnostics);
        } catch (ParseFailure failure) {
            return new ParseResult(new Expression.Invalid(source, failure.code),
                    List.of(new ComputationDiagnostic(failure.code,
                            failure.getMessage() + " at " + failure.offset)));
        }
    }

    private static final class Parser {
        private final String source;
        private final Document document;
        private final List<ComputationDiagnostic> diagnostics = new ArrayList<>();
        private final UnitParser units = new UnitParser();
        private int offset;

        private Parser(String source, Document document) { this.source = source; this.document = document; }

        private Expression additive() {
            var left = multiplicative();
            while (true) {
                spaces();
                if (eat('+')) left = new Expression.Binary(Expression.Operator.ADD, left, multiplicative());
                else if (eat('-')) left = new Expression.Binary(Expression.Operator.SUBTRACT, left, multiplicative());
                else return left;
            }
        }

        private Expression multiplicative() {
            var left = unary();
            while (true) {
                spaces();
                if (eat('*') || eat('×')) left = new Expression.Binary(Expression.Operator.MULTIPLY, left, unary());
                else if (eat('/')) left = new Expression.Binary(Expression.Operator.DIVIDE, left, unary());
                else return left;
            }
        }

        private Expression unary() {
            spaces();
            if (eat('+')) return new Expression.Unary(Expression.UnaryOperator.PLUS, unary());
            if (eat('-')) return new Expression.Unary(Expression.UnaryOperator.MINUS, unary());
            return power();
        }

        private Expression power() {
            var base = primary();
            spaces();
            if (!eat('^')) return base;
            spaces();
            var sign = eat('-') ? -1 : 1;
            if (sign == 1) eat('+');
            var start = offset;
            while (!end() && Character.isDigit(peek())) offset++;
            if (start == offset) throw powerError("Power requires an integer exponent");
            if (!end() && peek() == '.') throw powerError("Power requires an integer exponent");
            try {
                return new Expression.Power(base, Math.multiplyExact(sign, Integer.parseInt(source.substring(start, offset))));
            } catch (NumberFormatException | ArithmeticException exception) {
                throw powerError("Power is outside the supported integer range");
            }
        }

        private Expression primary() {
            spaces();
            if (eat('(')) {
                var inner = additive();
                spaces();
                if (!eat(')')) throw error("Missing closing parenthesis");
                return new Expression.Group(inner);
            }
            var difference = eat('Δ');
            if (!end() && (Character.isDigit(peek()) || peek() == '.')) return numeric(difference);
            if (difference) throw error("Temperature difference requires a numeric quantity");
            if (!end() && (Character.isLetter(peek()) || peek() == '_')) return identifier();
            throw error("Expected a value or variable");
        }

        private Expression numeric(boolean difference) {
            var start = offset;
            while (!end() && Character.isDigit(peek())) offset++;
            if (eat('.')) while (!end() && Character.isDigit(peek())) offset++;
            if (source.substring(start, offset).equals(".")) throw error("Invalid decimal number");
            if (!end() && (peek() == 'e' || peek() == 'E')) {
                var exponentStart = offset++;
                if (!end() && (peek() == '+' || peek() == '-')) offset++;
                var digits = offset;
                while (!end() && Character.isDigit(peek())) offset++;
                if (digits == offset) offset = exponentStart;
            }
            final BigDecimal number;
            try { number = new BigDecimal(source.substring(start, offset)); }
            catch (NumberFormatException failure) { throw error("Invalid decimal number"); }
            var unitStart = offset;
            spaces();
            if (end() || !isUnitStart(peek())) {
                offset = unitStart;
                if (difference) throw error("Temperature difference requires a unit");
                return new Expression.NumberLiteral(number);
            }
            var candidateStart = offset;
            while (!end()) {
                var c = peek();
                if (Character.isWhitespace(c) || c == '+' || c == '-' || c == '*' || c == '×' || c == ')') break;
                if (c == '/' && (offset + 1 == source.length() || !isUnitStart(source.charAt(offset + 1)))) break;
                offset++;
            }
            var symbol = source.substring(candidateStart, offset);
            try {
                var unit = units.parseRequired(symbol);
                var semantics = difference ? QuantitySemantics.TEMPERATURE_DIFFERENCE : QuantitySemantics.defaultFor(unit);
                return new Expression.ValueLiteral(new ScientificValue.Physical(new Quantity(number, unit, semantics)));
            } catch (IllegalArgumentException failure) {
                throw error("Invalid quantity unit: " + symbol);
            }
        }

        private Expression identifier() {
            var start = offset++;
            while (!end() && (Character.isLetterOrDigit(peek()) || peek() == '_')) offset++;
            var name = source.substring(start, offset);
            var matches = document.blocks().stream().filter(VariableDefinition.class::isInstance)
                    .map(VariableDefinition.class::cast).filter(variable -> variable.name().equals(name)).toList();
            if (matches.size() == 1) return new Expression.Variable(
                    new VariableDependencyReference(matches.getFirst().id()), name);
            diagnostics.add(new ComputationDiagnostic(matches.isEmpty()
                    ? ComputationDiagnostic.Code.UNKNOWN_VARIABLE : ComputationDiagnostic.Code.AMBIGUOUS_VARIABLE,
                    matches.isEmpty() ? "Unknown variable: " + name : "Ambiguous variable: " + name));
            return new Expression.UnresolvedName(name);
        }

        private static boolean isUnitStart(char c) {
            return Character.isLetter(c) || c == '°' || c == 'µ' || c == 'μ';
        }
        private void spaces() { while (!end() && Character.isWhitespace(peek())) offset++; }
        private boolean eat(char c) { if (!end() && source.charAt(offset) == c) { offset++; return true; } return false; }
        private char peek() { return source.charAt(offset); }
        private boolean end() { return offset >= source.length(); }
        private ParseFailure error(String message) { return new ParseFailure(message, offset); }
        private ParseFailure powerError(String message) {
            return new ParseFailure(message, offset, ComputationDiagnostic.Code.INVALID_POWER);
        }
    }

    private static final class ParseFailure extends RuntimeException {
        private final int offset;
        private final ComputationDiagnostic.Code code;
        private ParseFailure(String message, int offset) {
            this(message, offset, ComputationDiagnostic.Code.INVALID_EXPRESSION);
        }
        private ParseFailure(String message, int offset, ComputationDiagnostic.Code code) {
            super(message);
            this.offset = offset;
            this.code = code;
        }
    }
}
