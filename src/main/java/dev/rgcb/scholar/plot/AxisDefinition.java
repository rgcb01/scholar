package dev.rgcb.scholar.plot;

import java.util.Objects;
import java.util.Optional;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitExpression;

public record AxisDefinition(String label, Optional<AxisRange> explicitRange, AxisScale scale,
                             Optional<UnitExpression> displayUnit, Optional<QuantitySemantics> displayUnitSemantics) {
    public AxisDefinition(String label, Optional<AxisRange> explicitRange, AxisScale scale) {
        this(label, explicitRange, scale, Optional.empty(), Optional.empty());
    }
    public AxisDefinition(String label, Optional<AxisRange> explicitRange, AxisScale scale,
                          Optional<UnitExpression> displayUnit) {
        this(label, explicitRange, scale, displayUnit, displayUnit.map(QuantitySemantics::defaultFor));
    }
    public AxisDefinition {
        label = Objects.requireNonNull(label, "label");
        explicitRange = Objects.requireNonNull(explicitRange, "explicitRange");
        scale = Objects.requireNonNull(scale, "scale");
        displayUnit = Objects.requireNonNull(displayUnit, "displayUnit");
        displayUnitSemantics = Objects.requireNonNull(displayUnitSemantics, "displayUnitSemantics");
        if (displayUnit.isPresent() != displayUnitSemantics.isPresent()) {
            throw new IllegalArgumentException("plot display unit and quantity semantics must be present together");
        }
        if (displayUnit.isPresent()) displayUnitSemantics.orElseThrow().validate(displayUnit.orElseThrow());
    }

    public static AxisDefinition linear(String label) {
        return new AxisDefinition(label, Optional.empty(), AxisScale.LINEAR);
    }

    public AxisDefinition withDisplayUnit(UnitExpression unit) {
        return withDisplayUnit(unit, QuantitySemantics.defaultFor(unit));
    }

    public AxisDefinition withDisplayUnit(UnitExpression unit, QuantitySemantics semantics) {
        return new AxisDefinition(label, explicitRange, scale, Optional.of(unit), Optional.of(semantics));
    }
}
