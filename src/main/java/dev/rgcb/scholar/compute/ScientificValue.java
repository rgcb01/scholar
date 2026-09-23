package dev.rgcb.scholar.compute;

import dev.rgcb.scholar.quantity.QuantityValue;
import java.math.BigDecimal;
import java.util.Objects;

public sealed interface ScientificValue permits ScientificValue.Scalar, ScientificValue.Physical {
    record Scalar(BigDecimal value) implements ScientificValue {
        public Scalar { value = Objects.requireNonNull(value, "value"); }
    }

    record Physical(QuantityValue value) implements ScientificValue {
        public Physical { value = Objects.requireNonNull(value, "value"); }
    }
}
