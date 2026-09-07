package dev.rgcb.scholar.mechanical.layout;

import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramRect;
import dev.rgcb.scholar.mechanical.*;
import java.util.Objects;

public record LaidOutMechanicalSymbol(int elementIndex, DiagramElementId elementId, MechanicalSymbolKind kind, MechanicalOrientation orientation, LaidOutDiagramRect bounds) {
 public LaidOutMechanicalSymbol { if(elementIndex<0) throw new IllegalArgumentException("elementIndex must not be negative."); Objects.requireNonNull(elementId); Objects.requireNonNull(kind); Objects.requireNonNull(orientation); Objects.requireNonNull(bounds); }
}
