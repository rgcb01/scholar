package dev.rgcb.scholar.mechanical.layout;

import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramLabel;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramRect;
import dev.rgcb.scholar.mechanical.MechanicalDimensionKind;
import java.util.Objects;

/** Derived viewport-space geometry for one M19B mechanical dimension/callout. */
public record LaidOutMechanicalDimension(
        int elementIndex,
        DiagramElementId elementId,
        MechanicalDimensionKind kind,
        LaidOutDiagramRect bounds,
        LaidOutDiagramLabel label
) {
    public LaidOutMechanicalDimension {
        if (elementIndex < 0) throw new IllegalArgumentException("elementIndex must not be negative.");
        Objects.requireNonNull(elementId, "elementId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(label, "label");
    }
}
