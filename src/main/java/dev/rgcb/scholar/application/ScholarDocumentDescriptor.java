package dev.rgcb.scholar.application;

import java.util.Objects;

public record ScholarDocumentDescriptor(
        ScholarDocumentId id,
        String displayName,
        long createdAtEpochMillis,
        long modifiedAtEpochMillis,
        DocumentPreview preview
) {
    public ScholarDocumentDescriptor {
        id = Objects.requireNonNull(id, "id");
        displayName = ScholarDocumentNames.normalize(displayName);
        preview = Objects.requireNonNull(preview, "preview");
        if (createdAtEpochMillis < 0 || modifiedAtEpochMillis < 0) {
            throw new IllegalArgumentException("timestamps must not be negative.");
        }
    }
}
