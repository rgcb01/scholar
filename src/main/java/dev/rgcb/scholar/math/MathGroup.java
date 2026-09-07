package dev.rgcb.scholar.math;

import java.util.Objects;

/**
 * An expression grouped by an explicit authored delimiter pair.
 */
public record MathGroup(MathExpression content, MathDelimiter delimiter) implements MathExpression {
    public MathGroup {
        content = Objects.requireNonNull(content, "content");
        delimiter = Objects.requireNonNull(delimiter, "delimiter");
    }
}
