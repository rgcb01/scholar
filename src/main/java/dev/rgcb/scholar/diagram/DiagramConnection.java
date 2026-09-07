package dev.rgcb.scholar.diagram;

import java.util.Objects;

public record DiagramConnection(DiagramEndpoint source, DiagramEndpoint target, String label) {
    public DiagramConnection {
        source = Objects.requireNonNull(source, "source");
        target = Objects.requireNonNull(target, "target");
        label = Objects.requireNonNull(label, "label");
        if (source.equals(target)) {
            throw new IllegalArgumentException("Diagram connection cannot connect an endpoint to itself.");
        }
    }
}
