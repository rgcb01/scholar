package dev.rgcb.scholar.persistence;

import java.util.List;
import java.util.Objects;

public sealed interface PersistenceResult<T> {
    List<PersistenceDiagnostic> diagnostics();

    record Success<T>(T value, List<PersistenceDiagnostic> diagnostics) implements PersistenceResult<T> {
        public Success {
            Objects.requireNonNull(value);
            diagnostics = List.copyOf(diagnostics);
            if (diagnostics.stream().anyMatch(d -> d.severity() == PersistenceDiagnostic.Severity.ERROR)) {
                throw new IllegalArgumentException("Successful persistence cannot contain errors.");
            }
        }
    }

    record Failure<T>(List<PersistenceDiagnostic> diagnostics) implements PersistenceResult<T> {
        public Failure {
            diagnostics = List.copyOf(diagnostics);
            if (diagnostics.stream().noneMatch(d -> d.severity() == PersistenceDiagnostic.Severity.ERROR)) {
                throw new IllegalArgumentException("Failure requires an error.");
            }
        }
    }

    static <T> Failure<T> failure(PersistenceDiagnostic.Code code, String path, String message) {
        return new Failure<>(List.of(new PersistenceDiagnostic(PersistenceDiagnostic.Severity.ERROR, code, path, message)));
    }
}
