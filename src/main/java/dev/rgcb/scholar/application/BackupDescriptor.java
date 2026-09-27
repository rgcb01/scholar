package dev.rgcb.scholar.application;

import java.util.Objects;

/** Stable metadata for one previous confirmed canonical version. */
public record BackupDescriptor(ScholarDocumentId documentId, String storageName, long savedAtEpochMillis) {
    public BackupDescriptor {
        documentId = Objects.requireNonNull(documentId, "documentId");
        storageName = Objects.requireNonNull(storageName, "storageName");
        if (savedAtEpochMillis < 0) throw new IllegalArgumentException("Backup time must not be negative.");
    }
}
