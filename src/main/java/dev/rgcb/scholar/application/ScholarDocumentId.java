package dev.rgcb.scholar.application;

import dev.rgcb.scholar.persistence.FileDocumentStorage;
import java.util.Objects;
import java.util.UUID;

/** Stable application-library identity, independent from IDs inside the document AST. */
public record ScholarDocumentId(String value) {
    public ScholarDocumentId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!FileDocumentStorage.validName(value)) {
            throw new IllegalArgumentException("Invalid Scholar document identity.");
        }
    }

    public static ScholarDocumentId create() {
        return new ScholarDocumentId("doc-" + UUID.randomUUID());
    }
}
