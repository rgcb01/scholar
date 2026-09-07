package dev.rgcb.scholar.mechanical;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramElement;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramPort;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Semantic relationship between authored mechanical primitives.
 *
 * <p>The marker bounds are only a small interaction footprint. Visual marker
 * placement is derived from the referenced primitive geometry, so relationships
 * follow their objects when the drawing changes.</p>
 */
public record MechanicalConstraint(
        DiagramElementId id,
        DiagramBounds bounds,
        MechanicalConstraintKind kind,
        DiagramElementId subjectId,
        Optional<DiagramElementId> peerId
) implements DiagramElement {
    public MechanicalConstraint {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(subjectId, "subjectId");
        peerId = Objects.requireNonNull(peerId, "peerId");
        if (kind.binary() != peerId.isPresent()) {
            throw new IllegalArgumentException("Constraint arity does not match " + kind);
        }
        if (peerId.filter(subjectId::equals).isPresent()) {
            throw new IllegalArgumentException("A binary mechanical constraint requires two distinct primitives.");
        }
    }

    public static MechanicalConstraint unary(
            DiagramElementId id,
            DiagramBounds bounds,
            MechanicalConstraintKind kind,
            DiagramElementId subjectId
    ) {
        return new MechanicalConstraint(id, bounds, kind, subjectId, Optional.empty());
    }

    public static MechanicalConstraint binary(
            DiagramElementId id,
            DiagramBounds bounds,
            MechanicalConstraintKind kind,
            DiagramElementId subjectId,
            DiagramElementId peerId
    ) {
        return new MechanicalConstraint(id, bounds, kind, subjectId, Optional.of(peerId));
    }

    @Override
    public MechanicalConstraint withBounds(DiagramBounds replacement) {
        return new MechanicalConstraint(id, Objects.requireNonNull(replacement, "replacement"), kind, subjectId, peerId);
    }

    @Override
    public List<DiagramPort> ports() {
        return List.of();
    }

    public boolean references(DiagramElementId elementId) {
        return subjectId.equals(elementId) || peerId.filter(elementId::equals).isPresent();
    }
}
