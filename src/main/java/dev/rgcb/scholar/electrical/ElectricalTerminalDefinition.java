package dev.rgcb.scholar.electrical;

import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import java.util.Objects;

public record ElectricalTerminalDefinition(
        DiagramPortId id,
        ElectricalTerminalRole role,
        DiagramPortPlacement canonicalPlacement
) {
    public ElectricalTerminalDefinition {
        id = Objects.requireNonNull(id, "id");
        role = Objects.requireNonNull(role, "role");
        canonicalPlacement = Objects.requireNonNull(canonicalPlacement, "canonicalPlacement");
    }
}
