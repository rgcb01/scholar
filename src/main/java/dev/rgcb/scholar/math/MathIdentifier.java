package dev.rgcb.scholar.math;

import java.util.Objects;

/**
 * An ordinary mathematical name such as a variable, quantity, or function name.
 */
public record MathIdentifier(String name) implements MathExpression {
    public MathIdentifier {
        name = Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Math identifier name must not be blank.");
        }
    }
}
