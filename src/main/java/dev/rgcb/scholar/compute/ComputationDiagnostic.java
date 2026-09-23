package dev.rgcb.scholar.compute;

import java.util.Objects;

public record ComputationDiagnostic(Code code, String message) {
    public enum Code {
        UNKNOWN_VARIABLE, AMBIGUOUS_VARIABLE, INCOMPATIBLE_DIMENSIONS, DIVIDE_BY_ZERO,
        INVALID_POWER, INVALID_EXPRESSION, UNSUPPORTED_OPERATION, INVALID_TEMPERATURE_OPERATION
    }

    public ComputationDiagnostic {
        code = Objects.requireNonNull(code, "code");
        message = Objects.requireNonNull(message, "message");
        if (message.isBlank()) throw new IllegalArgumentException("Diagnostic message must not be blank.");
    }
}
