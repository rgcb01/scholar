package dev.rgcb.scholar.document;

import dev.rgcb.scholar.math.MathExpression;
import java.util.Objects;

/**
 * A display equation represented by a Scholar Math expression.
 */
public record EquationBlock(MathExpression expression) implements BlockNode {
    public EquationBlock {
        expression = Objects.requireNonNull(expression, "expression");
    }
}
