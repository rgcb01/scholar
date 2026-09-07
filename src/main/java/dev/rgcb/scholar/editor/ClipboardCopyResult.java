package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.clipboard.ScholarClipboardPayload;
import java.util.Objects;
import java.util.Optional;

public record ClipboardCopyResult(String plainText, Optional<ScholarClipboardPayload> payload) {
    public ClipboardCopyResult {
        plainText = Objects.requireNonNull(plainText, "plainText");
        payload = Objects.requireNonNull(payload, "payload");
    }

    public static ClipboardCopyResult plainText(String plainText) {
        return new ClipboardCopyResult(plainText, Optional.empty());
    }

    public static ClipboardCopyResult structured(String plainText, ScholarClipboardPayload payload) {
        return new ClipboardCopyResult(plainText, Optional.of(payload));
    }
}
