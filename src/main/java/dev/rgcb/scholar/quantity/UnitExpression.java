package dev.rgcb.scholar.quantity;

import java.util.List;
import java.util.Objects;

public record UnitExpression(List<UnitFactor> factors) {
    public UnitExpression {
        factors = List.copyOf(Objects.requireNonNull(factors, "factors"));
        if (factors.isEmpty()) throw new IllegalArgumentException("unit expression requires at least one factor");
    }
    public static UnitExpression of(String unitId) { return new UnitExpression(List.of(new UnitFactor(unitId, 1))); }
    public PhysicalDimension dimension(UnitRegistry registry) { return registry.dimension(this); }
    public String displaySymbol(UnitRegistry registry) { return registry.displaySymbol(this, true); }
    public String asciiSymbol(UnitRegistry registry) { return registry.displaySymbol(this, false); }
    public boolean compatibleWith(UnitExpression other, UnitRegistry registry) {
        return dimension(registry).equals(other.dimension(registry));
    }
}
