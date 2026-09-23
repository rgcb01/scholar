package dev.rgcb.scholar.analysis;

import java.util.Objects;

public record AnalysisDiagnostic(Code code, String message) {
    public enum Code {
        MISSING_DATASET, MISSING_COLUMN, TEXT_COLUMN, NO_OBSERVATIONS, INSUFFICIENT_OBSERVATIONS,
        CONSTANT_X, SINGULAR_FIT, INCOMPATIBLE_DISPLAY_UNIT, UNSUPPORTED_TEMPERATURE_AXIS
    }

    public AnalysisDiagnostic {
        Objects.requireNonNull(code);
        message = Objects.requireNonNull(message);
    }
}
