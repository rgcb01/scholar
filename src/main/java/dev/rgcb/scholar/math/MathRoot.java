package dev.rgcb.scholar.math;

import java.util.Objects;
import java.util.Optional;

/**
 * A square root or indexed root.
 */
public record MathRoot(MathExpression radicand, Optional<MathExpression> index) implements MathExpression {
    public MathRoot {
        radicand = Objects.requireNonNull(radicand, "radicand");
        index = Objects.requireNonNull(index, "index");
    }
}
