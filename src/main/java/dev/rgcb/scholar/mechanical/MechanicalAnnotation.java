package dev.rgcb.scholar.mechanical;
import dev.rgcb.scholar.diagram.*;
import java.util.*;
public record MechanicalAnnotation(DiagramElementId id, DiagramBounds bounds, MechanicalAnnotationKind kind, String text) implements DiagramElement {
 public MechanicalAnnotation { Objects.requireNonNull(id); Objects.requireNonNull(bounds); Objects.requireNonNull(kind); text=Objects.requireNonNull(text); }
 @Override public MechanicalAnnotation withBounds(DiagramBounds b){return new MechanicalAnnotation(id,b,kind,text);}
 public MechanicalAnnotation withText(String t){return new MechanicalAnnotation(id,bounds,kind,t);}
 @Override public List<DiagramPort> ports(){return List.of();}
}
