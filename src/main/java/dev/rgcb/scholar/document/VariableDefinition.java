package dev.rgcb.scholar.document;

import dev.rgcb.scholar.compute.ScientificValue;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** An explicitly authored scientific value with document-stable identity. */
public record VariableDefinition(String id, String name, ScientificValue value,
                                 Optional<String> label) implements ComputationTransferBlock {
    public VariableDefinition {
        id = Objects.requireNonNull(id, "id");
        name = Objects.requireNonNull(name, "name");
        value = Objects.requireNonNull(value, "value");
        label = Objects.requireNonNull(label, "label");
        if (id.isBlank() || !name.matches("[\\p{L}_][\\p{L}\\p{N}_]*")) {
            throw new IllegalArgumentException("Variable requires a stable ID and scientific identifier name.");
        }
    }

    public VariableDefinition(String id, String name, ScientificValue value) {
        this(id, name, value, Optional.empty());
    }

    public VariableDefinition withName(String replacement) { return new VariableDefinition(id, replacement, value, label); }
    public VariableDefinition withValue(ScientificValue replacement) { return new VariableDefinition(id, name, replacement, label); }
    @Override public Optional<String> definedVariableId() { return Optional.of(id); }
    @Override public List<VariableDependencyReference> variableDependencies() { return List.of(); }
    @Override public VariableDefinition withVariableTransferIds(Optional<String> definedId,
                                                                List<VariableDependencyReference> dependencies) {
        if (!dependencies.isEmpty()) throw new IllegalArgumentException("Value variable has no expression dependencies.");
        return new VariableDefinition(definedId.orElseThrow(), name, value, label);
    }
}
