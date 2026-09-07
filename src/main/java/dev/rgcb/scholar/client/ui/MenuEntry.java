package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.EditorAction;
import java.util.Objects;
import java.util.Optional;

public record MenuEntry(MenuEntryKind kind, Optional<EditorAction> action) {
    public MenuEntry {
        kind = Objects.requireNonNull(kind, "kind");
        action = Objects.requireNonNull(action, "action");
        if (kind == MenuEntryKind.ACTION && action.isEmpty()) {
            throw new IllegalArgumentException("Action menu entries require an action.");
        }
        if (kind == MenuEntryKind.SEPARATOR && action.isPresent()) {
            throw new IllegalArgumentException("Separator menu entries must not have an action.");
        }
    }

    public static MenuEntry action(EditorAction action) {
        return new MenuEntry(MenuEntryKind.ACTION, Optional.of(action));
    }

    public static MenuEntry separator() {
        return new MenuEntry(MenuEntryKind.SEPARATOR, Optional.empty());
    }
}
