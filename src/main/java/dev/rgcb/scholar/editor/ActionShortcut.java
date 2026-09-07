package dev.rgcb.scholar.editor;

import java.util.Objects;

public record ActionShortcut(String displayText) {
    public ActionShortcut {
        displayText = Objects.requireNonNull(displayText, "displayText");
        if (displayText.isBlank()) {
            throw new IllegalArgumentException("displayText must not be blank.");
        }
    }
}
