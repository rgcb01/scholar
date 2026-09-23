package dev.rgcb.scholar.transfer;

import java.util.Objects;
import java.util.Optional;

/** Operational information only; never inserted into semantic document content. */
public record TransferDiagnostic(
        Severity severity,
        Code code,
        String message,
        Optional<StableIdentityKey> sourceIdentity,
        Optional<String> fragmentLocation
) {
    public enum Severity { INFO, WARNING, ERROR }

    public enum Code {
        EXTERNAL_REFERENCE_DEGRADED,
        MISSING_REQUIRED_RESOURCE,
        UNSUPPORTED_SOURCE_SELECTION,
        UNSUPPORTED_DESTINATION,
        MALFORMED_FRAGMENT,
        INVALID_RESOURCE_CLOSURE,
        INVALID_COMPOSITE_GRAPH,
        IDENTITY_REMAP_FAILURE,
        INSERTION_VALIDATION_FAILURE,
        STALE_DESTINATION,
        SOURCE_DISPLAY_UNAVAILABLE,
        SOURCE_VALIDATION_WARNING,
        UNRESOLVED_EXTERNAL_VARIABLE_DEPENDENCY,
        UNRESOLVED_EXTERNAL_ANALYSIS_DEPENDENCY
    }

    public TransferDiagnostic {
        severity = Objects.requireNonNull(severity, "severity");
        code = Objects.requireNonNull(code, "code");
        message = Objects.requireNonNull(message, "message");
        sourceIdentity = Objects.requireNonNull(sourceIdentity, "sourceIdentity");
        fragmentLocation = Objects.requireNonNull(fragmentLocation, "fragmentLocation");
        if (message.isBlank() || fragmentLocation.filter(String::isBlank).isPresent()) {
            throw new IllegalArgumentException("Diagnostic message/location must not be blank.");
        }
    }
}
