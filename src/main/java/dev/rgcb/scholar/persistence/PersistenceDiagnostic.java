package dev.rgcb.scholar.persistence;

import java.util.Objects;

public record PersistenceDiagnostic(Severity severity, Code code, String path, String message) {
    public enum Severity { ERROR, WARNING }
    public enum Code {
        MALFORMED_JSON, WRONG_FORMAT, UNSUPPORTED_VERSION, MISSING_FIELD,
        INVALID_VALUE, UNKNOWN_TYPE, VALIDATION_FAILURE, IO_FAILURE, INVALID_NAME, SIZE_LIMIT
    }
    public PersistenceDiagnostic {
        Objects.requireNonNull(severity);
        Objects.requireNonNull(code);
        Objects.requireNonNull(path);
        Objects.requireNonNull(message);
    }
}
