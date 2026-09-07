package dev.rgcb.scholar.electrical.symbol;

import java.util.List;
import java.util.Objects;

public record ElectricalSymbolPolyline(List<NormalizedElectricalPoint> points)
        implements ElectricalSymbolPrimitive {
    public ElectricalSymbolPolyline {
        points = List.copyOf(Objects.requireNonNull(points, "points"));
        if (points.size() < 2) {
            throw new IllegalArgumentException("Electrical symbol polylines need at least two points.");
        }
        points.forEach(point -> Objects.requireNonNull(point, "point"));
    }
}
