package dev.rgcb.scholar.electrical;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record ElectricalComponentDefinition(
        ElectricalComponentKind kind,
        String defaultReferencePrefix,
        List<ElectricalTerminalDefinition> terminals
) {
    public ElectricalComponentDefinition {
        kind = Objects.requireNonNull(kind, "kind");
        defaultReferencePrefix = Objects.requireNonNull(defaultReferencePrefix, "defaultReferencePrefix");
        if (defaultReferencePrefix.isBlank()) {
            throw new IllegalArgumentException("Electrical reference prefix must not be blank.");
        }
        terminals = List.copyOf(Objects.requireNonNull(terminals, "terminals"));
        if (terminals.isEmpty()) {
            throw new IllegalArgumentException("Electrical components must expose at least one terminal.");
        }
        var ids = new HashSet<>();
        for (var terminal : terminals) {
            Objects.requireNonNull(terminal, "terminal");
            if (!ids.add(terminal.id())) {
                throw new IllegalArgumentException("Electrical terminal ids must be unique within a component kind: "
                        + terminal.id().value());
            }
        }
    }
}
