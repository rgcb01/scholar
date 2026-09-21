package dev.rgcb.scholar.plot;

import java.util.Objects;
import java.util.Optional;
import dev.rgcb.scholar.quantity.UnitExpression;

public record AxisDefinition(String label, Optional<AxisRange> explicitRange, AxisScale scale,
                             Optional<UnitExpression> displayUnit) {
    public AxisDefinition(String label, Optional<AxisRange> explicitRange, AxisScale scale) {
        this(label, explicitRange, scale, Optional.empty());
    }
    public AxisDefinition {
        label = Objects.requireNonNull(label, "label");
        explicitRange = Objects.requireNonNull(explicitRange, "explicitRange");
        scale = Objects.requireNonNull(scale, "scale");
        displayUnit = Objects.requireNonNull(displayUnit, "displayUnit");
    }

    public static AxisDefinition linear(String label) {
        return new AxisDefinition(label, Optional.empty(), AxisScale.LINEAR);
    }

    public AxisDefinition withDisplayUnit(UnitExpression unit) {
        return new AxisDefinition(label, explicitRange, scale, Optional.of(unit));
    }
}
