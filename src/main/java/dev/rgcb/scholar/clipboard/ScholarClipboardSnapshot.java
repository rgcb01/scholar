package dev.rgcb.scholar.clipboard;

import java.util.Objects;

public record ScholarClipboardSnapshot(String plainText, ScholarClipboardPayload payload) {
    public ScholarClipboardSnapshot {
        plainText = Objects.requireNonNull(plainText, "plainText");
        payload = Objects.requireNonNull(payload, "payload");
    }
}
