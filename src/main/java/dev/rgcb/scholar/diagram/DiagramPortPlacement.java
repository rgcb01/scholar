package dev.rgcb.scholar.diagram;

import java.util.Objects;

public record DiagramPortPlacement(DiagramPortSide side, double offset) {
    public DiagramPortPlacement {
        side = Objects.requireNonNull(side, "side");
        if (!Double.isFinite(offset) || offset < 0.0 || offset > 1.0) {
            throw new IllegalArgumentException("Diagram port offset must be finite and in [0, 1].");
        }
    }
}
