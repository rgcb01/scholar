package dev.rgcb.scholar.quantity;

import java.math.BigDecimal;
import java.util.Objects;

public record Quantity(BigDecimal value, UnitExpression unit, QuantitySemantics semantics) implements QuantityValue, Comparable<Quantity> {
    public Quantity {
        value = Objects.requireNonNull(value, "value");
        unit = Objects.requireNonNull(unit, "unit");
        semantics = Objects.requireNonNull(semantics, "semantics");
        semantics.validate(unit);
    }
    public Quantity(BigDecimal value, UnitExpression unit) { this(value, unit, QuantitySemantics.defaultFor(unit)); }
    public Quantity(String value, UnitExpression unit) { this(new BigDecimal(value), unit); }
    public Quantity(String value, UnitExpression unit, QuantitySemantics semantics) { this(new BigDecimal(value), unit, semantics); }
    public Quantity nominal() { return this; }
    public boolean compatibleWith(Quantity other) {
        return semantics == other.semantics && unit.compatibleWith(other.unit, UnitRegistry.builtIn());
    }
    public Quantity convertTo(UnitExpression target) { return new UnitConverter().convert(this, target); }
    public int compareTo(Quantity other) {
        if (!compatibleWith(other)) throw new IllegalArgumentException("incompatible quantity dimensions");
        return value.compareTo(other.convertTo(unit).value);
    }
}
