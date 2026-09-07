package dev.rgcb.scholar.math;

import java.util.Objects;

/**
 * Authored numeric text in a mathematical expression.
 */
public record MathNumber(String content) implements MathExpression {
    public MathNumber {
        content = Objects.requireNonNull(content, "content");
        if (content.isBlank()) {
            throw new IllegalArgumentException("Math number content must not be blank.");
        }
    }
}
