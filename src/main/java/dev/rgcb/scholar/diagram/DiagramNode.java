package dev.rgcb.scholar.diagram;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record DiagramNode(
        DiagramElementId id,
        DiagramBounds bounds,
        String label,
        List<DiagramPort> ports
) implements DiagramElement {
    public DiagramNode {
        id = Objects.requireNonNull(id, "id");
        bounds = Objects.requireNonNull(bounds, "bounds");
        label = Objects.requireNonNull(label, "label");
        ports = List.copyOf(Objects.requireNonNull(ports, "ports"));

        var ids = new HashSet<DiagramPortId>();
        for (var port : ports) {
            Objects.requireNonNull(port, "port");
            if (!ids.add(port.id())) {
                throw new IllegalArgumentException("Diagram port ids must be unique within an element: " + port.id().value());
            }
        }
    }
    @Override
    public DiagramNode withBounds(DiagramBounds bounds) {
        return new DiagramNode(id, Objects.requireNonNull(bounds, "bounds"), label, ports);
    }

}
