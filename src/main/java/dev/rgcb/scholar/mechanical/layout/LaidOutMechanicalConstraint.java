package dev.rgcb.scholar.mechanical.layout;

import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramRect;
import dev.rgcb.scholar.mechanical.MechanicalConstraintKind;
import java.util.Objects;

/** Small derived viewport marker for an M19C semantic relationship. */
public record LaidOutMechanicalConstraint(
        int elementIndex,
        DiagramElementId elementId,
        MechanicalConstraintKind kind,
        LaidOutDiagramRect bounds
) {
    public LaidOutMechanicalConstraint {
        if (elementIndex < 0) throw new IllegalArgumentException("elementIndex must not be negative.");
        Objects.requireNonNull(elementId, "elementId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(bounds, "bounds");
    }
}
