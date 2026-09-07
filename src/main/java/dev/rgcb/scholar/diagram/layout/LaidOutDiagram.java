package dev.rgcb.scholar.diagram.layout;

import dev.rgcb.scholar.electrical.layout.LaidOutElectricalComponent;
import dev.rgcb.scholar.electrical.layout.LaidOutElectricalJunction;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalPrimitive;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalDimension;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalConstraint;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalSymbol;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalAnnotation;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalPartReference;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record LaidOutDiagram(
        int sourceBlockIndex,
        int x,
        int y,
        int width,
        int height,
        int workspaceX,
        int workspaceY,
        int workspaceWidth,
        int workspaceHeight,
        int canvasX,
        int canvasY,
        int canvasWidth,
        int canvasHeight,
        Optional<LaidOutDiagramLabel> title,
        List<LaidOutDiagramNode> nodes,
        List<LaidOutElectricalComponent> electricalComponents,
        List<LaidOutElectricalJunction> electricalJunctions,
        List<LaidOutMechanicalPrimitive> mechanicalPrimitives,
        List<LaidOutMechanicalDimension> mechanicalDimensions,
        List<LaidOutMechanicalConstraint> mechanicalConstraints,
        List<LaidOutMechanicalSymbol> mechanicalSymbols,
        List<LaidOutMechanicalAnnotation> mechanicalAnnotations,
        List<LaidOutMechanicalPartReference> mechanicalPartReferences,
        List<LaidOutDiagramConnection> connections,
        DiagramCoordinateTransform transform,
        DiagramViewport viewport
) {
    public LaidOutDiagram {
        if (sourceBlockIndex < 0) {
            throw new IllegalArgumentException("sourceBlockIndex must not be negative.");
        }
        if (width <= 0 || height <= 0
                || workspaceWidth <= 0 || workspaceHeight <= 0
                || canvasWidth <= 0 || canvasHeight <= 0) {
            throw new IllegalArgumentException("Laid-out diagram dimensions must be positive.");
        }
        title = Objects.requireNonNull(title, "title");
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        electricalComponents = List.copyOf(Objects.requireNonNull(electricalComponents, "electricalComponents"));
        electricalJunctions = List.copyOf(Objects.requireNonNull(electricalJunctions, "electricalJunctions"));
        mechanicalPrimitives = List.copyOf(Objects.requireNonNull(mechanicalPrimitives, "mechanicalPrimitives"));
        mechanicalDimensions = List.copyOf(Objects.requireNonNull(mechanicalDimensions, "mechanicalDimensions"));
        mechanicalConstraints = List.copyOf(Objects.requireNonNull(mechanicalConstraints, "mechanicalConstraints"));
        mechanicalSymbols = List.copyOf(Objects.requireNonNull(mechanicalSymbols, "mechanicalSymbols"));
        mechanicalAnnotations = List.copyOf(Objects.requireNonNull(mechanicalAnnotations, "mechanicalAnnotations"));
        mechanicalPartReferences = List.copyOf(Objects.requireNonNull(mechanicalPartReferences, "mechanicalPartReferences"));
        connections = List.copyOf(Objects.requireNonNull(connections, "connections"));
        transform = Objects.requireNonNull(transform, "transform");
        viewport = Objects.requireNonNull(viewport, "viewport");
    }

    public LaidOutDiagramRect workspaceBounds() {
        return new LaidOutDiagramRect(workspaceX, workspaceY, workspaceWidth, workspaceHeight);
    }

    public LaidOutDiagramRect canvasBounds() {
        return new LaidOutDiagramRect(canvasX, canvasY, canvasWidth, canvasHeight);
    }
}
