package dev.rgcb.scholar.electrical.layout;

import dev.rgcb.scholar.diagram.layout.LaidOutDiagramPoint;
import java.util.Objects;

public record LaidOutElectricalLine(LaidOutDiagramPoint start, LaidOutDiagramPoint end)
        implements LaidOutElectricalPrimitive {
    public LaidOutElectricalLine {
        start = Objects.requireNonNull(start, "start");
        end = Objects.requireNonNull(end, "end");
    }
}
