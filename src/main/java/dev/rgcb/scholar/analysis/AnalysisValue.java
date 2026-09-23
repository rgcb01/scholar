package dev.rgcb.scholar.analysis;

import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.quantity.Quantity;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.ScientificNumberFormatter;
import dev.rgcb.scholar.quantity.UnitExpression;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record AnalysisValue(BigDecimal value, Optional<UnitExpression> unit, QuantitySemantics semantics) {
    public AnalysisValue {
        value = Objects.requireNonNull(value);
        unit = Objects.requireNonNull(unit);
        semantics = Objects.requireNonNull(semantics);
        if (unit.isPresent()) semantics.validate(unit.orElseThrow());
        else if (semantics != QuantitySemantics.LINEAR) throw new IllegalArgumentException("Unitless analysis value must be linear.");
    }

    public String format(NumberNotation notation, boolean unicode) {
        var formatter = new ScientificNumberFormatter();
        return unit.map(expression -> formatter.format(new Quantity(value, expression, semantics), notation, unicode))
                .orElseGet(() -> formatter.formatNumber(value, notation, unicode));
    }
}
