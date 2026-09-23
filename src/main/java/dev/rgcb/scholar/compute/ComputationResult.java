package dev.rgcb.scholar.compute;

import java.util.List;
import java.util.Optional;

public record ComputationResult(Optional<ScientificValue> value, List<ComputationDiagnostic> diagnostics) {
    public ComputationResult {
        value = java.util.Objects.requireNonNull(value);
        diagnostics = List.copyOf(diagnostics);
        if (value.isPresent() == !diagnostics.isEmpty()) {
            throw new IllegalArgumentException("A computed result has either a value or diagnostics.");
        }
    }

    public static ComputationResult success(ScientificValue value) {
        return new ComputationResult(Optional.of(value), List.of());
    }

    public static ComputationResult failure(ComputationDiagnostic.Code code, String message) {
        return new ComputationResult(Optional.empty(), List.of(new ComputationDiagnostic(code, message)));
    }
}
