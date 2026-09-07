package dev.rgcb.scholar.math.editor;

import dev.rgcb.scholar.math.MathExpression;
import java.util.Objects;

public record MathEditResult(MathExpression expression, MathSelection selection, boolean changed) {
    public MathEditResult {
        expression = Objects.requireNonNull(expression, "expression");
        selection = Objects.requireNonNull(selection, "selection");
    }
}
