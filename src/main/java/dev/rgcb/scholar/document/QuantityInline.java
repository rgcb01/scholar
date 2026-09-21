package dev.rgcb.scholar.document;

import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.quantity.QuantityValue;
import java.util.Objects;

/** Atomic semantic quantity embedded in prose or a caption. */
public record QuantityInline(QuantityValue value, NumberNotation notation) implements InlineNode {
    public QuantityInline { value = Objects.requireNonNull(value); notation = Objects.requireNonNull(notation); }
    public QuantityInline(QuantityValue value) { this(value, NumberNotation.DECIMAL); }
}
