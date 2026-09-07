package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.diagram.DiagramElementId;
import java.util.Objects;

public record DiagramElementTarget(int elementIndex, DiagramElementId elementId) implements DiagramEditTarget {
    public DiagramElementTarget {
        if (elementIndex < 0) {
            throw new IllegalArgumentException("elementIndex must not be negative.");
        }
        elementId = Objects.requireNonNull(elementId, "elementId");
    }
}
