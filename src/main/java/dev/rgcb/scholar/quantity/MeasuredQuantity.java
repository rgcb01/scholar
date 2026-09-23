package dev.rgcb.scholar.quantity;

import java.math.BigDecimal;
import java.util.Objects;

public record MeasuredQuantity(Quantity nominal, BigDecimal absoluteUncertainty) implements QuantityValue {
    public MeasuredQuantity {
        nominal = Objects.requireNonNull(nominal, "nominal");
        absoluteUncertainty = Objects.requireNonNull(absoluteUncertainty, "absoluteUncertainty");
        if (absoluteUncertainty.signum() < 0) throw new IllegalArgumentException("uncertainty must not be negative");
    }
    public MeasuredQuantity(String value, String uncertainty, UnitExpression unit) {
        this(new Quantity(value, unit), new BigDecimal(uncertainty));
    }
    public MeasuredQuantity(String value, String uncertainty, UnitExpression unit, QuantitySemantics semantics) {
        this(new Quantity(value, unit, semantics), new BigDecimal(uncertainty));
    }
    public Quantity uncertaintyQuantity() {
        var semantics = nominal.semantics().isTemperature()
                ? QuantitySemantics.TEMPERATURE_DIFFERENCE
                : QuantitySemantics.LINEAR;
        return new Quantity(absoluteUncertainty, nominal.unit(), semantics);
    }
    public MeasuredQuantity convertTo(UnitExpression target) { return new UnitConverter().convert(this, target); }
}
