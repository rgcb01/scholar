package dev.rgcb.scholar.math;

import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.quantity.QuantityValue;
import java.util.Objects;

/** Explicit atomic unit-bearing value; identifiers remain ordinary variables. */
public record MathQuantity(QuantityValue value, NumberNotation notation) implements MathExpression {
    public MathQuantity { value = Objects.requireNonNull(value); notation = Objects.requireNonNull(notation); }
    public MathQuantity(QuantityValue value) { this(value, NumberNotation.DECIMAL); }
}
