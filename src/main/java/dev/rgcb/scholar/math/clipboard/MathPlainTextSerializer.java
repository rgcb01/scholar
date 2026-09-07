package dev.rgcb.scholar.math.clipboard;

import dev.rgcb.scholar.math.MathExpression;
import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathGroup;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNamedOperator;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathRoot;
import dev.rgcb.scholar.math.MathScript;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.MathSymbol;
import dev.rgcb.scholar.math.MathText;
import java.util.Objects;
import java.util.Optional;

public final class MathPlainTextSerializer {
    public Optional<String> serialize(MathExpression expression) {
        Objects.requireNonNull(expression, "expression");
        return serializeExpression(expression, Context.ROOT);
    }

    private Optional<String> serializeExpression(MathExpression expression, Context context) {
        if (expression instanceof MathSequence sequence) {
            return serializeSequence(sequence, context);
        }
        if (expression instanceof MathNumber number) {
            return Optional.of(number.content());
        }
        if (expression instanceof MathIdentifier identifier) {
            return Optional.of(identifier.name());
        }
        if (expression instanceof MathNamedOperator namedOperator) {
            return Optional.of(namedOperator.name());
        }
        if (expression instanceof MathText text) {
            return Optional.of(text.content());
        }
        if (expression instanceof MathOperator operator) {
            return Optional.of(operator.symbol());
        }
        if (expression instanceof MathSymbol symbol) {
            return Optional.of(symbol.symbol());
        }
        if (expression instanceof MathFraction fraction) {
            return serializeFraction(fraction, context);
        }
        if (expression instanceof MathRoot root) {
            return serializeRoot(root);
        }
        if (expression instanceof MathScript script) {
            return serializeScript(script);
        }
        if (expression instanceof MathGroup group) {
            return serializeGroup(group);
        }
        return Optional.empty();
    }

    private Optional<String> serializeSequence(MathSequence sequence, Context context) {
        var output = new StringBuilder();
        for (var expression : sequence.expressions()) {
            var serialized = serializeExpression(expression, Context.SEQUENCE);
            if (serialized.isEmpty()) {
                return Optional.empty();
            }
            var text = serialized.orElseThrow();
            if (expression instanceof MathOperator) {
                trimTrailingSpace(output);
                if (!output.isEmpty()) {
                    output.append(' ');
                }
                output.append(text).append(' ');
            } else {
                if (!output.isEmpty() && output.charAt(output.length() - 1) != ' ') {
                    output.append(' ');
                }
                output.append(text);
            }
        }
        var text = output.toString().stripTrailing();
        if (context != Context.ROOT && needsSequenceParentheses(sequence)) {
            return Optional.of("(" + text + ")");
        }
        return Optional.of(text);
    }

    private Optional<String> serializeFraction(MathFraction fraction, Context context) {
        var numerator = serializeFractionSide(fraction.numerator());
        var denominator = serializeFractionSide(fraction.denominator());
        if (numerator.isEmpty() || denominator.isEmpty()) {
            return Optional.empty();
        }
        var text = numerator.orElseThrow() + " / " + denominator.orElseThrow();
        return context == Context.FRACTION_SIDE ? Optional.of("(" + text + ")") : Optional.of(text);
    }

    private Optional<String> serializeFractionSide(MathExpression expression) {
        if (expression instanceof MathSequence sequence) {
            var serialized = serializeSequence(sequence, Context.FRACTION_SIDE);
            if (serialized.isEmpty()) {
                return Optional.empty();
            }
            var text = serialized.orElseThrow();
            return needsSequenceParentheses(sequence) ? Optional.of(text) : Optional.of(stripOuterParenthesesIfSimple(text));
        }
        return serializeExpression(expression, expression instanceof MathFraction ? Context.FRACTION_SIDE : Context.SEQUENCE);
    }

    private Optional<String> serializeRoot(MathRoot root) {
        var radicand = serializeExpression(root.radicand(), Context.SEQUENCE);
        if (radicand.isEmpty()) {
            return Optional.empty();
        }
        if (root.index().isEmpty()) {
            return Optional.of("sqrt(" + radicand.orElseThrow() + ")");
        }
        var index = serializeExpression(root.index().orElseThrow(), Context.SEQUENCE);
        if (index.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("root(" + index.orElseThrow() + ", " + radicand.orElseThrow() + ")");
    }

    private Optional<String> serializeScript(MathScript script) {
        var base = serializeScriptBase(script.base());
        if (base.isEmpty()) {
            return Optional.empty();
        }
        var output = new StringBuilder(base.orElseThrow());
        if (script.subscript().isPresent()) {
            var subscript = serializeScriptSlot(script.subscript().orElseThrow());
            if (subscript.isEmpty()) {
                return Optional.empty();
            }
            output.append('_').append(subscript.orElseThrow());
        }
        if (script.superscript().isPresent()) {
            var superscript = serializeScriptSlot(script.superscript().orElseThrow());
            if (superscript.isEmpty()) {
                return Optional.empty();
            }
            output.append('^').append(superscript.orElseThrow());
        }
        return Optional.of(output.toString());
    }

    private Optional<String> serializeGroup(MathGroup group) {
        var content = serializeExpression(group.content(), Context.ROOT);
        if (content.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(leftDelimiter(group) + content.orElseThrow() + rightDelimiter(group));
    }

    private Optional<String> serializeScriptBase(MathExpression expression) {
        var serialized = serializeExpression(expression, Context.SEQUENCE);
        if (serialized.isEmpty()) {
            return Optional.empty();
        }
        if (expression instanceof MathSequence sequence && needsSequenceParentheses(sequence)) {
            return serialized;
        }
        if (expression instanceof MathFraction) {
            return Optional.of("(" + serialized.orElseThrow() + ")");
        }
        return serialized;
    }

    private Optional<String> serializeScriptSlot(MathExpression expression) {
        if (expression instanceof MathSequence sequence && sequence.expressions().isEmpty()) {
            return Optional.of("()");
        }
        var serialized = serializeExpression(expression, Context.SEQUENCE);
        if (serialized.isEmpty()) {
            return Optional.empty();
        }
        if (expression instanceof MathSequence sequence && needsSequenceParentheses(sequence)) {
            return serialized;
        }
        if (expression instanceof MathFraction) {
            return Optional.of("(" + serialized.orElseThrow() + ")");
        }
        return serialized;
    }

    private static boolean needsSequenceParentheses(MathSequence sequence) {
        return sequence.expressions().stream().anyMatch(MathOperator.class::isInstance);
    }

    private static void trimTrailingSpace(StringBuilder output) {
        while (!output.isEmpty() && output.charAt(output.length() - 1) == ' ') {
            output.setLength(output.length() - 1);
        }
    }

    private static String stripOuterParenthesesIfSimple(String text) {
        return text;
    }

    private static String leftDelimiter(MathGroup group) {
        return switch (group.delimiter()) {
            case PARENTHESES -> "(";
            case BRACKETS -> "[";
            case BRACES -> "{";
        };
    }

    private static String rightDelimiter(MathGroup group) {
        return switch (group.delimiter()) {
            case PARENTHESES -> ")";
            case BRACKETS -> "]";
            case BRACES -> "}";
        };
    }

    private enum Context {
        ROOT,
        SEQUENCE,
        FRACTION_SIDE
    }
}
