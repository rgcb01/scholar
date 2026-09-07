package dev.rgcb.scholar.electrical.layout;

import dev.rgcb.scholar.diagram.layout.LaidOutDiagramPoint;
import java.util.Objects;

public record LaidOutElectricalCircle(LaidOutDiagramPoint center, int radius)
        implements LaidOutElectricalPrimitive {
    public LaidOutElectricalCircle {
        center = Objects.requireNonNull(center, "center");
        if (radius <= 0) {
            throw new IllegalArgumentException("Laid-out electrical circle radius must be positive.");
        }
    }
}
