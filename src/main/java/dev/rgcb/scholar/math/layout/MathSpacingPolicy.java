package dev.rgcb.scholar.math.layout;

import dev.rgcb.scholar.math.MathExpression;
import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathGroup;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNamedOperator;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathOperatorRole;
import dev.rgcb.scholar.math.MathRoot;
import dev.rgcb.scholar.math.MathScript;
import dev.rgcb.scholar.math.MathSymbol;
import dev.rgcb.scholar.math.MathText;
import java.util.Objects;

final class MathSpacingPolicy {
    private static final int BINARY_OPERATOR_SPACING = 4;
    private static final int RELATION_OPERATOR_SPACING = 6;

    int spacingBetween(MathExpression left, MathExpression right, MathTextMeasurer measurer) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(measurer, "measurer");

        var leftAtom = classify(left);
        var rightAtom = classify(right);
        if (rightAtom == Atom.CLOSE_DELIMITER || leftAtom == Atom.OPEN_DELIMITER) {
            return 0;
        }
        if (leftAtom == Atom.NAMED_OPERATOR && rightAtom == Atom.OPEN_DELIMITER) {
            return 0;
        }
        if (leftAtom == Atom.TEXT || rightAtom == Atom.TEXT) {
            return wordSpacing(measurer);
        }
        if (leftAtom == Atom.NAMED_OPERATOR || rightAtom == Atom.NAMED_OPERATOR) {
            return smallSpacing(measurer);
        }

        var leftRole = operatorRole(left);
        var rightRole = operatorRole(right);
        if (leftRole == MathOperatorRole.RELATION || rightRole == MathOperatorRole.RELATION) {
            return RELATION_OPERATOR_SPACING;
        }
        if (leftRole == MathOperatorRole.BINARY || rightRole == MathOperatorRole.BINARY) {
            return BINARY_OPERATOR_SPACING;
        }
        return 0;
    }

    private static int wordSpacing(MathTextMeasurer measurer) {
        return Math.max(1, measurer.measureText(" ", MathTextKind.TEXT).width());
    }

    private static int smallSpacing(MathTextMeasurer measurer) {
        return Math.max(1, wordSpacing(measurer) / 2);
    }

    private static MathOperatorRole operatorRole(MathExpression expression) {
        return expression instanceof MathOperator operator ? operator.role() : null;
    }

    private static Atom classify(MathExpression expression) {
        if (expression instanceof MathNamedOperator) {
            return Atom.NAMED_OPERATOR;
        }
        if (expression instanceof MathText) {
            return Atom.TEXT;
        }
        if (expression instanceof MathIdentifier) {
            return Atom.IDENTIFIER;
        }
        if (expression instanceof MathNumber) {
            return Atom.NUMBER;
        }
        if (expression instanceof MathOperator) {
            return Atom.OPERATOR;
        }
        if (expression instanceof MathSymbol symbol) {
            return switch (symbol.symbol()) {
                case "(", "[", "{" -> Atom.OPEN_DELIMITER;
                case ")", "]", "}" -> Atom.CLOSE_DELIMITER;
                default -> Atom.SYMBOL;
            };
        }
        if (expression instanceof MathGroup group
                && group.delimiter() == dev.rgcb.scholar.math.MathDelimiter.PARENTHESES) {
            return Atom.OPEN_DELIMITER;
        }
        if (expression instanceof MathFraction || expression instanceof MathScript
                || expression instanceof MathRoot || expression instanceof MathGroup) {
            return Atom.STRUCTURED;
        }
        return Atom.STRUCTURED;
    }

    private enum Atom {
        IDENTIFIER,
        NUMBER,
        NAMED_OPERATOR,
        TEXT,
        OPERATOR,
        SYMBOL,
        OPEN_DELIMITER,
        CLOSE_DELIMITER,
        STRUCTURED
    }
}
