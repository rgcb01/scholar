package dev.rgcb.scholar.plot;

import java.util.Objects;
import java.util.Optional;

public record AxisDefinition(String label, Optional<AxisRange> explicitRange, AxisScale scale) {
    public AxisDefinition {
        label = Objects.requireNonNull(label, "label");
        explicitRange = Objects.requireNonNull(explicitRange, "explicitRange");
        scale = Objects.requireNonNull(scale, "scale");
    }

    public static AxisDefinition linear(String label) {
        return new AxisDefinition(label, Optional.empty(), AxisScale.LINEAR);
    }
}
