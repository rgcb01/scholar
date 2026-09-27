package dev.rgcb.scholar.application;

import java.util.Objects;
import java.util.Optional;

/** Presentation-independent summary of validated recoverable work. */
public record RecoveryCandidate(
        RecoveryId recoveryId,
        Optional<ScholarDocumentId> documentId,
        String displayName,
        long capturedAtEpochMillis,
        Optional<Long> confirmedModifiedAtEpochMillis,
        long revision,
        SourceState sourceState
) {
    public enum SourceState { EXISTING_DOCUMENT, MISSING_DOCUMENT, UNTITLED }

    public RecoveryCandidate {
        recoveryId = Objects.requireNonNull(recoveryId, "recoveryId");
        documentId = Objects.requireNonNull(documentId, "documentId");
        displayName = ScholarDocumentNames.normalize(displayName);
        confirmedModifiedAtEpochMillis = Objects.requireNonNull(confirmedModifiedAtEpochMillis,
                "confirmedModifiedAtEpochMillis");
        sourceState = Objects.requireNonNull(sourceState, "sourceState");
    }

    RecoveryCandidate withSourceState(SourceState state) {
        return new RecoveryCandidate(recoveryId, documentId, displayName, capturedAtEpochMillis,
                confirmedModifiedAtEpochMillis, revision, state);
    }
}
