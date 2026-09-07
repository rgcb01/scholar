package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramPortId;
import java.util.Objects;

public record DiagramPortTarget(
        int elementIndex,
        int portIndex,
        DiagramElementId elementId,
        DiagramPortId portId
) implements DiagramEditTarget {
    public DiagramPortTarget {
        if (elementIndex < 0 || portIndex < 0) {
            throw new IllegalArgumentException("Diagram port target indices must not be negative.");
        }
        elementId = Objects.requireNonNull(elementId, "elementId");
        portId = Objects.requireNonNull(portId, "portId");
    }
}
