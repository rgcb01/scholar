package dev.rgcb.scholar.mechanical.layout;

import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramRect;
import dev.rgcb.scholar.mechanical.MechanicalOrientation;
import dev.rgcb.scholar.mechanical.MechanicalPrimitiveKind;
import java.util.Objects;

/** Derived viewport-space geometry for one M19A/M19C mechanical primitive. */
public record LaidOutMechanicalPrimitive(
        int elementIndex,
        DiagramElementId elementId,
        MechanicalPrimitiveKind kind,
        MechanicalOrientation orientation,
        LaidOutDiagramRect bounds
) {
    public LaidOutMechanicalPrimitive {
        if (elementIndex < 0) {
            throw new IllegalArgumentException("elementIndex must not be negative.");
        }
        Objects.requireNonNull(elementId, "elementId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(orientation, "orientation");
        Objects.requireNonNull(bounds, "bounds");
    }
}
