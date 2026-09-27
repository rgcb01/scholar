package dev.rgcb.scholar.application;

import dev.rgcb.scholar.document.Document;
import java.util.Objects;
import java.util.Optional;

/** Immutable authored state captured without changing the editor session. */
public record RecoverySnapshot(
        RecoveryId recoveryId,
        Optional<ScholarDocumentId> documentId,
        boolean originallyUntitled,
        String displayName,
        long capturedAtEpochMillis,
        Optional<Long> confirmedModifiedAtEpochMillis,
        long revision,
        Document document
) {
    public RecoverySnapshot {
        recoveryId = Objects.requireNonNull(recoveryId, "recoveryId");
        documentId = Objects.requireNonNull(documentId, "documentId");
        displayName = ScholarDocumentNames.normalize(displayName);
        confirmedModifiedAtEpochMillis = Objects.requireNonNull(confirmedModifiedAtEpochMillis,
                "confirmedModifiedAtEpochMillis");
        document = Objects.requireNonNull(document, "document");
        if (capturedAtEpochMillis < 0 || revision < 0
                || confirmedModifiedAtEpochMillis.filter(value -> value < 0).isPresent()) {
            throw new IllegalArgumentException("Recovery metadata must not be negative.");
        }
    }
}
