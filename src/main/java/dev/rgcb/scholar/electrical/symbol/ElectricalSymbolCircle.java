package dev.rgcb.scholar.electrical.symbol;

import java.util.Objects;

/** Radius is normalized against the smaller laid-out component dimension. */
public record ElectricalSymbolCircle(NormalizedElectricalPoint center, double radius) implements ElectricalSymbolPrimitive {
    public ElectricalSymbolCircle {
        center = Objects.requireNonNull(center, "center");
        if (!Double.isFinite(radius) || radius <= 0.0 || radius > 0.5) {
            throw new IllegalArgumentException("Electrical symbol circle radius must be finite and in (0, 0.5].");
        }
    }
}
