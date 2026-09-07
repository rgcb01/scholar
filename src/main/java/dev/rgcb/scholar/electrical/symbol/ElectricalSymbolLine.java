package dev.rgcb.scholar.electrical.symbol;

import java.util.Objects;

public record ElectricalSymbolLine(NormalizedElectricalPoint start, NormalizedElectricalPoint end)
        implements ElectricalSymbolPrimitive {
    public ElectricalSymbolLine {
        start = Objects.requireNonNull(start, "start");
        end = Objects.requireNonNull(end, "end");
    }
}
