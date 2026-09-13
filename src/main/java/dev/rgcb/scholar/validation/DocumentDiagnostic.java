package dev.rgcb.scholar.validation;

import java.util.Objects;
import java.util.Optional;

public record DocumentDiagnostic(
        DocumentDiagnosticSeverity severity,
        DocumentDiagnosticCode code,
        String message,
        Optional<Integer> blockIndex,
        Optional<String> stableId,
        Optional<String> context
) {
    public DocumentDiagnostic {
        severity = Objects.requireNonNull(severity, "severity");
        code = Objects.requireNonNull(code, "code");
        message = Objects.requireNonNull(message, "message");
        blockIndex = Objects.requireNonNull(blockIndex, "blockIndex");
        stableId = Objects.requireNonNull(stableId, "stableId");
        context = Objects.requireNonNull(context, "context");
    }

    public static DocumentDiagnostic error(DocumentDiagnosticCode code, String message) {
        return new DocumentDiagnostic(DocumentDiagnosticSeverity.ERROR, code, message, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static DocumentDiagnostic error(DocumentDiagnosticCode code, String message, int blockIndex, String context) {
        return new DocumentDiagnostic(DocumentDiagnosticSeverity.ERROR, code, message, Optional.of(blockIndex), Optional.empty(), Optional.of(context));
    }

    public static DocumentDiagnostic error(DocumentDiagnosticCode code, String message, String stableId, String context) {
        return new DocumentDiagnostic(DocumentDiagnosticSeverity.ERROR, code, message, Optional.empty(), Optional.of(stableId), Optional.of(context));
    }

    public static DocumentDiagnostic error(DocumentDiagnosticCode code, String message, int blockIndex, String stableId, String context) {
        return new DocumentDiagnostic(DocumentDiagnosticSeverity.ERROR, code, message, Optional.of(blockIndex), Optional.of(stableId), Optional.of(context));
    }

    public static DocumentDiagnostic warning(DocumentDiagnosticCode code, String message, int blockIndex, String stableId, String context) {
        return new DocumentDiagnostic(DocumentDiagnosticSeverity.WARNING, code, message, Optional.of(blockIndex), Optional.of(stableId), Optional.of(context));
    }

    public static DocumentDiagnostic warning(DocumentDiagnosticCode code, String message, int blockIndex, String context) {
        return new DocumentDiagnostic(DocumentDiagnosticSeverity.WARNING, code, message, Optional.of(blockIndex), Optional.empty(), Optional.of(context));
    }

    public static DocumentDiagnostic warning(DocumentDiagnosticCode code, String message, String stableId, String context) {
        return new DocumentDiagnostic(DocumentDiagnosticSeverity.WARNING, code, message, Optional.empty(), Optional.of(stableId), Optional.of(context));
    }
}
