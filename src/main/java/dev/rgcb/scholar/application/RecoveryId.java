package dev.rgcb.scholar.application;

import dev.rgcb.scholar.persistence.FileDocumentStorage;
import java.util.Objects;
import java.util.UUID;

/** Stable identity for one open workspace's recovery stream. */
public record RecoveryId(String value) {
    public RecoveryId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!FileDocumentStorage.validName(value)) throw new IllegalArgumentException("Invalid recovery identity.");
    }

    public static RecoveryId create() {
        return new RecoveryId("recovery-" + UUID.randomUUID());
    }
}
