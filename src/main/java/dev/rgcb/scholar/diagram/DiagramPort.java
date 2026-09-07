package dev.rgcb.scholar.diagram;

import java.util.Objects;

public record DiagramPort(DiagramPortId id, String label, DiagramPortPlacement placement) {
    public DiagramPort {
        id = Objects.requireNonNull(id, "id");
        label = Objects.requireNonNull(label, "label");
        placement = Objects.requireNonNull(placement, "placement");
    }
}
