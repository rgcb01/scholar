package dev.rgcb.scholar.clipboard;

import java.util.Objects;
import java.util.Optional;

public final class ScholarClipboardService {
    private ScholarClipboardSnapshot snapshot;

    public void install(String plainText, ScholarClipboardPayload payload) {
        snapshot = new ScholarClipboardSnapshot(plainText, payload);
    }

    public Optional<ScholarClipboardPayload> matchingPayload(String currentText) {
        Objects.requireNonNull(currentText, "currentText");
        if (snapshot == null) {
            return Optional.empty();
        }
        if (!snapshot.plainText().equals(currentText)) {
            snapshot = null;
            return Optional.empty();
        }
        return Optional.of(snapshot.payload());
    }

    public Optional<ScholarClipboardSnapshot> snapshot() {
        return Optional.ofNullable(snapshot);
    }

    public void clear() {
        snapshot = null;
    }
}
