package dev.rgcb.scholar.mechanical;

import dev.rgcb.scholar.diagram.*;
import java.util.*;

/** Authored semantic mechanical symbol; visible strokes are derived by the renderer. */
public record MechanicalSymbol(DiagramElementId id, DiagramBounds bounds, MechanicalSymbolKind kind, MechanicalOrientation orientation) implements DiagramElement {
    public MechanicalSymbol { Objects.requireNonNull(id, "id"); Objects.requireNonNull(bounds, "bounds"); Objects.requireNonNull(kind, "kind"); Objects.requireNonNull(orientation, "orientation"); }
    public MechanicalSymbol(DiagramElementId id, DiagramBounds bounds, MechanicalSymbolKind kind) { this(id,bounds,kind,MechanicalOrientation.DEG_0); }
    @Override public MechanicalSymbol withBounds(DiagramBounds bounds) { return new MechanicalSymbol(id,bounds,kind,orientation); }
    public MechanicalSymbol withOrientation(MechanicalOrientation orientation) { return new MechanicalSymbol(id,bounds,kind,orientation); }
    @Override public List<DiagramPort> ports() { return List.of(); }
}
