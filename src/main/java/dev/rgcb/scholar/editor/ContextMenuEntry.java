package dev.rgcb.scholar.editor;

import java.util.Objects;
import java.util.Optional;

public record ContextMenuEntry(ContextMenuEntryKind kind, Optional<EditorAction> action) {
    public ContextMenuEntry {
        kind = Objects.requireNonNull(kind, "kind");
        action = Objects.requireNonNull(action, "action");
        if (kind == ContextMenuEntryKind.ACTION && action.isEmpty()) {
            throw new IllegalArgumentException("Action context menu entries require an action.");
        }
        if (kind == ContextMenuEntryKind.SEPARATOR && action.isPresent()) {
            throw new IllegalArgumentException("Separator context menu entries must not have an action.");
        }
    }

    public static ContextMenuEntry action(EditorAction action) {
        return new ContextMenuEntry(ContextMenuEntryKind.ACTION, Optional.of(action));
    }

    public static ContextMenuEntry separator() {
        return new ContextMenuEntry(ContextMenuEntryKind.SEPARATOR, Optional.empty());
    }
}
