package dev.rgcb.scholar.transfer;

import java.util.List;
import java.util.Objects;

public sealed interface MaterializationResult {
    List<TransferDiagnostic> diagnostics();

    record Success(MaterializedTransfer transfer) implements MaterializationResult {
        public Success { transfer = Objects.requireNonNull(transfer); }
        public List<TransferDiagnostic> diagnostics() { return transfer.plan().diagnostics(); }
    }

    record Failure(List<TransferDiagnostic> diagnostics) implements MaterializationResult {
        public Failure {
            diagnostics = List.copyOf(diagnostics);
            if (diagnostics.stream().noneMatch(d -> d.severity() == TransferDiagnostic.Severity.ERROR)) {
                throw new IllegalArgumentException("Failure requires an error.");
            }
        }
    }
}
