package dev.rgcb.scholar.validation;

import java.util.List;
import java.util.Objects;

public record DocumentValidationResult(List<DocumentDiagnostic> diagnostics) {
    public DocumentValidationResult {
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean isValid() {
        return diagnostics.stream().noneMatch(diagnostic -> diagnostic.severity() == DocumentDiagnosticSeverity.ERROR);
    }

    public List<DocumentDiagnostic> errors() {
        return diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == DocumentDiagnosticSeverity.ERROR)
                .toList();
    }

    public List<DocumentDiagnostic> warnings() {
        return diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == DocumentDiagnosticSeverity.WARNING)
                .toList();
    }
}
