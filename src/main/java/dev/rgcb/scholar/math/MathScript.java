package dev.rgcb.scholar.math;

import java.util.Objects;
import java.util.Optional;

/**
 * A base expression with an optional subscript, superscript, or both.
 */
public record MathScript(
        MathExpression base,
        Optional<MathExpression> subscript,
        Optional<MathExpression> superscript
) implements MathExpression {
    public MathScript {
        base = Objects.requireNonNull(base, "base");
        subscript = Objects.requireNonNull(subscript, "subscript");
        superscript = Objects.requireNonNull(superscript, "superscript");
        if (subscript.isEmpty() && superscript.isEmpty()) {
            throw new IllegalArgumentException("Math script must have a subscript, a superscript, or both.");
        }
    }
}
