package dev.rgcb.scholar.math;

import java.util.Objects;

/**
 * An explicitly authored named mathematical operator, such as sin, log, or rank.
 */
public record MathNamedOperator(String name) implements MathExpression {
    public MathNamedOperator {
        name = Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Math named operator name must not be blank.");
        }
    }
}
