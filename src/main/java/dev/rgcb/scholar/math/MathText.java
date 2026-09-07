package dev.rgcb.scholar.math;

import java.util.Objects;

/**
 * Intentionally authored textual content embedded inside mathematical notation.
 */
public record MathText(String content) implements MathExpression {
    public MathText {
        content = Objects.requireNonNull(content, "content");
        if (content.isBlank()) {
            throw new IllegalArgumentException("Math text content must not be blank.");
        }
        if (!content.equals(content.strip())) {
            throw new IllegalArgumentException("Math text content must not use leading or trailing whitespace.");
        }
    }
}
