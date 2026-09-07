package dev.rgcb.scholar.diagram;

import java.util.Objects;

public record DiagramPortId(String value) {
    public DiagramPortId {
        value = Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Diagram port id must not be blank.");
        }
    }
}
