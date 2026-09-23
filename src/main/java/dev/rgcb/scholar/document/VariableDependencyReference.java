package dev.rgcb.scholar.document;

import java.util.Objects;

/** A computation operand bound to a stable variable identity. */
public record VariableDependencyReference(String variableId) {
    public VariableDependencyReference {
        variableId = Objects.requireNonNull(variableId, "variableId");
        if (variableId.isBlank()) {
            throw new IllegalArgumentException("Variable dependency ID must not be blank.");
        }
    }
}
