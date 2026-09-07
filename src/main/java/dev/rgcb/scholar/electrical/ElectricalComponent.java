package dev.rgcb.scholar.electrical;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramElement;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import java.util.List;
import java.util.Objects;

/** Semantic schematic component; symbol strokes and terminal positions are derived. */
public record ElectricalComponent(
        DiagramElementId id,
        DiagramBounds bounds,
        ElectricalComponentKind kind,
        ElectricalOrientation orientation,
        String referenceDesignator,
        String valueLabel
) implements DiagramElement {
    public ElectricalComponent {
        id = Objects.requireNonNull(id, "id");
        bounds = Objects.requireNonNull(bounds, "bounds");
        kind = Objects.requireNonNull(kind, "kind");
        orientation = Objects.requireNonNull(orientation, "orientation");
        referenceDesignator = Objects.requireNonNull(referenceDesignator, "referenceDesignator");
        valueLabel = Objects.requireNonNull(valueLabel, "valueLabel");
    }

    @Override
    public ElectricalComponent withBounds(DiagramBounds bounds) {
        return new ElectricalComponent(
                id, Objects.requireNonNull(bounds, "bounds"), kind, orientation, referenceDesignator, valueLabel);
    }

    @Override
    public List<DiagramPort> ports() {
        return ElectricalComponentCatalog.definition(kind).terminals().stream()
                .map(terminal -> new DiagramPort(
                        terminal.id(),
                        "",
                        rotate(terminal.canonicalPlacement(), orientation)))
                .toList();
    }

    static DiagramPortPlacement rotate(DiagramPortPlacement placement, ElectricalOrientation orientation) {
        double x;
        double y;
        switch (placement.side()) {
            case LEFT -> {
                x = 0.0;
                y = placement.offset();
            }
            case RIGHT -> {
                x = 1.0;
                y = placement.offset();
            }
            case TOP -> {
                x = placement.offset();
                y = 0.0;
            }
            case BOTTOM -> {
                x = placement.offset();
                y = 1.0;
            }
            default -> throw new IllegalStateException("Unexpected diagram port side: " + placement.side());
        }

        for (var turn = 0; turn < orientation.quarterTurnsClockwise(); turn++) {
            var nextX = 1.0 - y;
            var nextY = x;
            x = nextX;
            y = nextY;
        }

        var epsilon = 1.0e-12;
        if (Math.abs(x) <= epsilon) {
            return new DiagramPortPlacement(DiagramPortSide.LEFT, clampUnit(y));
        }
        if (Math.abs(x - 1.0) <= epsilon) {
            return new DiagramPortPlacement(DiagramPortSide.RIGHT, clampUnit(y));
        }
        if (Math.abs(y) <= epsilon) {
            return new DiagramPortPlacement(DiagramPortSide.TOP, clampUnit(x));
        }
        if (Math.abs(y - 1.0) <= epsilon) {
            return new DiagramPortPlacement(DiagramPortSide.BOTTOM, clampUnit(x));
        }
        throw new IllegalStateException("Rotated electrical terminal did not remain on the component perimeter.");
    }

    private static double clampUnit(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
