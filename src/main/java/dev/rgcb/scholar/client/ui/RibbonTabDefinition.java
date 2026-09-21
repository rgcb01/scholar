package dev.rgcb.scholar.client.ui;

import java.util.List;
import java.util.Objects;

public record RibbonTabDefinition(String label, List<RibbonGroupDefinition> groups) {
    public RibbonTabDefinition {
        label = Objects.requireNonNull(label, "label");
        groups = List.copyOf(Objects.requireNonNull(groups, "groups"));
    }
}
