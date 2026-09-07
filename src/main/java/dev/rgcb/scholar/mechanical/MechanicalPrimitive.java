package dev.rgcb.scholar.mechanical;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramElement;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramPort;
import java.util.List;
import java.util.Objects;

/**
 * Semantic mechanical primitive authored on the logical diagram canvas.
 *
 * <p>M19C adds a minimal quarter-turn orientation for directional primitives.
 * The original three-argument constructor remains source-compatible and defaults
 * to DEG_0.</p>
 */
public record MechanicalPrimitive(
        DiagramElementId id,
        DiagramBounds bounds,
        MechanicalPrimitiveKind kind,
        MechanicalOrientation orientation
) implements DiagramElement {
    public MechanicalPrimitive(DiagramElementId id, DiagramBounds bounds, MechanicalPrimitiveKind kind) {
        this(id, bounds, kind, MechanicalOrientation.DEG_0);
    }

    public MechanicalPrimitive {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(orientation, "orientation");
    }

    @Override
    public MechanicalPrimitive withBounds(DiagramBounds bounds) {
        return new MechanicalPrimitive(id, Objects.requireNonNull(bounds, "bounds"), kind, orientation);
    }

    public MechanicalPrimitive withOrientation(MechanicalOrientation replacement) {
        return new MechanicalPrimitive(id, bounds, kind, Objects.requireNonNull(replacement, "replacement"));
    }

    public boolean directional() {
        return kind == MechanicalPrimitiveKind.LINE
                || kind == MechanicalPrimitiveKind.CENTERLINE
                || kind == MechanicalPrimitiveKind.ARROW;
    }

    public boolean radial() {
        return kind == MechanicalPrimitiveKind.CIRCLE || kind == MechanicalPrimitiveKind.ARC;
    }

    @Override
    public List<DiagramPort> ports() {
        return List.of();
    }
}
