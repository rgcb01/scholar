package dev.rgcb.scholar.mechanical.layout;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.layout.*;
import dev.rgcb.scholar.mechanical.MechanicalAnnotationKind;
import java.util.Objects;
public record LaidOutMechanicalAnnotation(int elementIndex, DiagramElementId elementId, MechanicalAnnotationKind kind, LaidOutDiagramRect bounds, LaidOutDiagramLabel label) {
 public LaidOutMechanicalAnnotation { if(elementIndex<0)throw new IllegalArgumentException(); Objects.requireNonNull(elementId);Objects.requireNonNull(kind);Objects.requireNonNull(bounds);Objects.requireNonNull(label); }
}
