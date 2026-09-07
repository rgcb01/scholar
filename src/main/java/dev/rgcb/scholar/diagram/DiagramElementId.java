package dev.rgcb.scholar.diagram;

import java.util.Objects;

public record DiagramElementId(String value) {
    public DiagramElementId {
        value = Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Diagram element id must not be blank.");
        }
    }
}
