package dev.rgcb.scholar.diagram.layout;

import dev.rgcb.scholar.diagram.DiagramElementId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record LaidOutDiagramNode(
        int elementIndex,
        DiagramElementId elementId,
        int x,
        int y,
        int width,
        int height,
        Optional<LaidOutDiagramLabel> label,
        List<LaidOutDiagramPort> ports
) {
    public LaidOutDiagramNode {
        if (elementIndex < 0) {
            throw new IllegalArgumentException("elementIndex must not be negative.");
        }
        elementId = Objects.requireNonNull(elementId, "elementId");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Laid-out diagram node dimensions must be positive.");
        }
        label = Objects.requireNonNull(label, "label");
        ports = List.copyOf(Objects.requireNonNull(ports, "ports"));
    }
}
