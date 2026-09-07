package dev.rgcb.scholar.diagram;

import java.util.Objects;

public record DiagramEndpoint(DiagramElementId elementId, DiagramPortId portId) {
    public DiagramEndpoint {
        elementId = Objects.requireNonNull(elementId, "elementId");
        portId = Objects.requireNonNull(portId, "portId");
    }
}
