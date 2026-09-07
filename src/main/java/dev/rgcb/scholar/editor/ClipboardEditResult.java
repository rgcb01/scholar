package dev.rgcb.scholar.editor;

import java.util.Objects;

public record ClipboardEditResult(String clipboardText, EditResult editResult) {
    public ClipboardEditResult {
        clipboardText = Objects.requireNonNull(clipboardText, "clipboardText");
        editResult = Objects.requireNonNull(editResult, "editResult");
    }
}
