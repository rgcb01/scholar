package dev.rgcb.scholar.electrical.layout;

import dev.rgcb.scholar.diagram.layout.LaidOutDiagramPoint;
import java.util.List;
import java.util.Objects;

public record LaidOutElectricalPolyline(List<LaidOutDiagramPoint> points)
        implements LaidOutElectricalPrimitive {
    public LaidOutElectricalPolyline {
        points = List.copyOf(Objects.requireNonNull(points, "points"));
        if (points.size() < 2) {
            throw new IllegalArgumentException("Laid-out electrical polylines need at least two points.");
        }
        points.forEach(point -> Objects.requireNonNull(point, "point"));
    }
}
