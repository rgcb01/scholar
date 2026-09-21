package dev.rgcb.scholar.transfer;

import java.util.List;
import java.util.Objects;

public sealed interface ExtractionResult {
    List<TransferDiagnostic> diagnostics();

    record Success(DocumentFragment fragment, SourceTransferMetadata sourceMetadata,
                   List<TransferDiagnostic> diagnostics) implements ExtractionResult {
        public Success {
            fragment = Objects.requireNonNull(fragment, "fragment");
            sourceMetadata = Objects.requireNonNull(sourceMetadata, "sourceMetadata");
            diagnostics = List.copyOf(diagnostics);
            if (diagnostics.stream().anyMatch(d -> d.severity() == TransferDiagnostic.Severity.ERROR)) {
                throw new IllegalArgumentException("Success cannot contain fatal diagnostics.");
            }
        }
    }

    record Failure(List<TransferDiagnostic> diagnostics) implements ExtractionResult {
        public Failure {
            diagnostics = List.copyOf(diagnostics);
            if (diagnostics.stream().noneMatch(d -> d.severity() == TransferDiagnostic.Severity.ERROR)) {
                throw new IllegalArgumentException("Failure requires a fatal diagnostic.");
            }
        }
    }
}
