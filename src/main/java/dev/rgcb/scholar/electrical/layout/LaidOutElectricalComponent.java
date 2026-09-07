package dev.rgcb.scholar.electrical.layout;

import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramLabel;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramPort;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record LaidOutElectricalComponent(
        int elementIndex,
        DiagramElementId elementId,
        ElectricalComponentKind kind,
        int x,
        int y,
        int width,
        int height,
        Optional<LaidOutDiagramLabel> referenceDesignator,
        Optional<LaidOutDiagramLabel> valueLabel,
        List<LaidOutDiagramPort> ports,
        List<LaidOutElectricalPrimitive> primitives
) {
    public LaidOutElectricalComponent {
        if (elementIndex < 0) {
            throw new IllegalArgumentException("elementIndex must not be negative.");
        }
        elementId = Objects.requireNonNull(elementId, "elementId");
        kind = Objects.requireNonNull(kind, "kind");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Laid-out electrical component dimensions must be positive.");
        }
        referenceDesignator = Objects.requireNonNull(referenceDesignator, "referenceDesignator");
        valueLabel = Objects.requireNonNull(valueLabel, "valueLabel");
        ports = List.copyOf(Objects.requireNonNull(ports, "ports"));
        primitives = List.copyOf(Objects.requireNonNull(primitives, "primitives"));
        ports.forEach(port -> Objects.requireNonNull(port, "port"));
        primitives.forEach(primitive -> Objects.requireNonNull(primitive, "primitive"));
    }
}
