package dev.rgcb.scholar.diagram.layout;

import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import java.util.Objects;
import java.util.Optional;

public record LaidOutDiagramPort(
        int elementIndex,
        int portIndex,
        DiagramElementId elementId,
        DiagramPortId portId,
        DiagramPortSide side,
        int centerX,
        int centerY,
        LaidOutDiagramRect hitBounds,
        Optional<LaidOutDiagramLabel> label
) {
    public LaidOutDiagramPort {
        if (elementIndex < 0 || portIndex < 0) {
            throw new IllegalArgumentException("Diagram port indices must not be negative.");
        }
        elementId = Objects.requireNonNull(elementId, "elementId");
        portId = Objects.requireNonNull(portId, "portId");
        side = Objects.requireNonNull(side, "side");
        hitBounds = Objects.requireNonNull(hitBounds, "hitBounds");
        label = Objects.requireNonNull(label, "label");
    }
}
