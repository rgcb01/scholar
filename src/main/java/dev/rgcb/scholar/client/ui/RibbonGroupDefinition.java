package dev.rgcb.scholar.client.ui;

import java.util.List;
import java.util.Objects;

public record RibbonGroupDefinition(String label, List<RibbonCommandPresentation> commands) {
    public RibbonGroupDefinition {
        label = Objects.requireNonNull(label, "label");
        commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
    }
}
