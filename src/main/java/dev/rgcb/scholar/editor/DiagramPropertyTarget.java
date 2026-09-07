package dev.rgcb.scholar.editor;

import java.util.Objects;

public record DiagramPropertyTarget(DiagramProperty property) implements DiagramEditTarget {
    public DiagramPropertyTarget {
        property = Objects.requireNonNull(property, "property");
    }
}
