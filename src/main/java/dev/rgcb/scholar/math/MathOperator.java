package dev.rgcb.scholar.math;

import java.util.Objects;

/**
 * An authored mathematical operator or relation.
 */
public record MathOperator(String symbol, MathOperatorRole role) implements MathExpression {
    public MathOperator {
        symbol = Objects.requireNonNull(symbol, "symbol");
        role = Objects.requireNonNull(role, "role");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("Math operator symbol must not be blank.");
        }
    }
}
