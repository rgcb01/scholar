package dev.rgcb.scholar.document;

import dev.rgcb.scholar.math.MathExpression;
import java.util.Objects;
import java.util.Optional;

/**
 * A display equation represented by a Scholar Math expression.
 */
public record EquationBlock(MathExpression expression, Optional<String> id) implements BlockNode {
    public EquationBlock(MathExpression expression) {
        this(expression, Optional.empty());
    }

    public EquationBlock(String id, MathExpression expression) {
        this(expression, Optional.of(id));
    }

    public EquationBlock {
        expression = Objects.requireNonNull(expression, "expression");
        id = normalizeId(id);
    }

    public EquationBlock withExpression(MathExpression replacement) {
        return new EquationBlock(replacement, id);
    }

    public EquationBlock withId(String replacement) {
        return new EquationBlock(expression, Optional.of(replacement));
    }

    private static Optional<String> normalizeId(Optional<String> id) {
        return Objects.requireNonNull(id, "id")
                .map(String::trim)
                .filter(value -> !value.isEmpty());
    }
}
