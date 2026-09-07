package dev.rgcb.scholar.math;

import java.util.Objects;

/**
 * A two-dimensional fraction with required numerator and denominator.
 */
public record MathFraction(MathExpression numerator, MathExpression denominator) implements MathExpression {
    public MathFraction {
        numerator = Objects.requireNonNull(numerator, "numerator");
        denominator = Objects.requireNonNull(denominator, "denominator");
    }
}
