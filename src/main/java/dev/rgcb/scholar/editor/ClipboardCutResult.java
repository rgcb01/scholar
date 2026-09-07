package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.clipboard.ScholarClipboardPayload;
import java.util.Objects;
import java.util.Optional;

public record ClipboardCutResult(String plainText, Optional<ScholarClipboardPayload> payload, EditResult editResult) {
    public ClipboardCutResult {
        plainText = Objects.requireNonNull(plainText, "plainText");
        payload = Objects.requireNonNull(payload, "payload");
        editResult = Objects.requireNonNull(editResult, "editResult");
    }

    public static ClipboardCutResult plainText(String plainText, EditResult editResult) {
        return new ClipboardCutResult(plainText, Optional.empty(), editResult);
    }

    public static ClipboardCutResult structured(String plainText, ScholarClipboardPayload payload, EditResult editResult) {
        return new ClipboardCutResult(plainText, Optional.of(payload), editResult);
    }
}
