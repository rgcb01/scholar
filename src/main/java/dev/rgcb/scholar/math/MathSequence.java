package dev.rgcb.scholar.math;

import java.util.List;

/**
 * An ordered expression made from smaller mathematical expressions.
 */
public record MathSequence(List<MathExpression> expressions) implements MathExpression {
    public MathSequence {
        expressions = List.copyOf(expressions);
    }
}
