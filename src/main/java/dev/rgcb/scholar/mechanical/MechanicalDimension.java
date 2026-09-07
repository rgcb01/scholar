package dev.rgcb.scholar.mechanical;

import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramElement;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramPort;
import java.util.List;
import java.util.Objects;

/**
 * Semantic mechanical measurement.
 *
 * <p>The authored bounds define the measured geometry and annotation envelope.
 * The displayed value is always derived from that geometry; it is deliberately
 * not stored as editable presentation text. M19C can therefore add constraints
 * later without replacing M19B's measurement model.</p>
 */
public record MechanicalDimension(
        DiagramElementId id,
        DiagramBounds bounds,
        MechanicalDimensionKind kind
) implements DiagramElement {
    public MechanicalDimension {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(kind, "kind");
    }

    @Override
    public MechanicalDimension withBounds(DiagramBounds bounds) {
        return new MechanicalDimension(id, Objects.requireNonNull(bounds, "bounds"), kind);
    }

    @Override
    public List<DiagramPort> ports() {
        return List.of();
    }

    /** Derived numeric measurement in logical-canvas units (degrees for ANGLE). */
    public double measuredValue() {
        return switch (kind) {
            case HORIZONTAL -> bounds.width();
            case VERTICAL -> bounds.height();
            case ALIGNED -> Math.hypot(bounds.width(), bounds.height());
            case RADIUS -> Math.min(bounds.width(), bounds.height()) / 2.0;
            case DIAMETER -> Math.min(bounds.width(), bounds.height());
            case ANGLE -> Math.toDegrees(Math.atan2(bounds.height(), bounds.width()));
        };
    }

    public String displayText() {
        var value = format(measuredValue());
        return switch (kind) {
            case RADIUS -> "R " + value;
            case DIAMETER -> "⌀ " + value;
            case ANGLE -> value + "°";
            default -> value;
        };
    }

    private static String format(double value) {
        var rounded = Math.rint(value * 10.0) / 10.0;
        if (Math.abs(rounded - Math.rint(rounded)) < 1.0e-9) {
            return Long.toString(Math.round(rounded));
        }
        return Double.toString(rounded);
    }
}
