package dev.rgcb.scholar.quantity;

import java.util.Objects;
import java.util.Optional;

public record UnitFactor(String unitId, Optional<MetricPrefix> prefix, int exponent) {
    public UnitFactor {
        unitId = Objects.requireNonNull(unitId, "unitId");
        prefix = Objects.requireNonNull(prefix, "prefix");
        if (unitId.isBlank()) throw new IllegalArgumentException("unitId must not be blank");
        if (exponent == 0) throw new IllegalArgumentException("unit exponent must not be zero");
    }
    public UnitFactor(String unitId, int exponent) { this(unitId, Optional.empty(), exponent); }
}
