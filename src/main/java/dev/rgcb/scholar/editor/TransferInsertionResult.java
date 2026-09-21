package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.transfer.TransferDiagnostic;
import java.util.List;
import java.util.Objects;

public sealed interface TransferInsertionResult {
    List<TransferDiagnostic> diagnostics();
    record Success(EditResult edit, List<TransferDiagnostic> diagnostics) implements TransferInsertionResult {
        public Success { edit = Objects.requireNonNull(edit); diagnostics = List.copyOf(diagnostics); }
    }
    record Failure(List<TransferDiagnostic> diagnostics) implements TransferInsertionResult {
        public Failure {
            diagnostics = List.copyOf(diagnostics);
            if (diagnostics.stream().noneMatch(d -> d.severity() == TransferDiagnostic.Severity.ERROR)) {
                throw new IllegalArgumentException("Failure requires an error.");
            }
        }
    }
}
