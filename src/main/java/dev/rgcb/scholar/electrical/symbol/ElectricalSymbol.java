package dev.rgcb.scholar.electrical.symbol;

import java.util.List;
import java.util.Objects;

public record ElectricalSymbol(List<ElectricalSymbolPrimitive> primitives) {
    public ElectricalSymbol {
        primitives = List.copyOf(Objects.requireNonNull(primitives, "primitives"));
        if (primitives.isEmpty()) {
            throw new IllegalArgumentException("Electrical symbols must contain derived geometry.");
        }
        primitives.forEach(primitive -> Objects.requireNonNull(primitive, "primitive"));
    }
}
