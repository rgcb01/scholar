package dev.rgcb.scholar.electrical;

import dev.rgcb.scholar.diagram.DiagramEndpoint;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Derived electrical connectivity group; never persisted as routed geometry. */
public record ElectricalNet(int index, Optional<String> label, Set<DiagramEndpoint> endpoints) {
    public ElectricalNet {
        if (index < 0) {
            throw new IllegalArgumentException("Electrical net index must not be negative.");
        }
        label = Objects.requireNonNull(label, "label");
        endpoints = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(endpoints, "endpoints")));
        if (endpoints.isEmpty()) {
            throw new IllegalArgumentException("Electrical net must contain at least one endpoint.");
        }
    }
}
