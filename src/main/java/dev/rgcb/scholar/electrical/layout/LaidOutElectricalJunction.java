package dev.rgcb.scholar.electrical.layout;

import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramLabel;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramPort;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Positioned junction dot and its interaction geometry. */
public record LaidOutElectricalJunction(
        int elementIndex,
        DiagramElementId elementId,
        int x,
        int y,
        int width,
        int height,
        Optional<LaidOutDiagramLabel> netLabel,
        List<LaidOutDiagramPort> ports
) {
    public LaidOutElectricalJunction {
        if (elementIndex < 0) {
            throw new IllegalArgumentException("elementIndex must not be negative.");
        }
        elementId = Objects.requireNonNull(elementId, "elementId");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Junction dimensions must be positive.");
        }
        netLabel = Objects.requireNonNull(netLabel, "netLabel");
        ports = List.copyOf(Objects.requireNonNull(ports, "ports"));
    }

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }
}
