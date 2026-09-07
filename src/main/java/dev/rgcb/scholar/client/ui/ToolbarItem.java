package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.EditorAction;
import java.util.Objects;
import java.util.Optional;

public record ToolbarItem(ToolbarItemKind kind, Optional<EditorAction> action) {
    public ToolbarItem {
        kind = Objects.requireNonNull(kind, "kind");
        action = Objects.requireNonNull(action, "action");
        if (kind == ToolbarItemKind.ACTION && action.isEmpty()) {
            throw new IllegalArgumentException("Action toolbar items require an action.");
        }
        if (kind != ToolbarItemKind.ACTION && action.isPresent()) {
            throw new IllegalArgumentException("Non-action toolbar items must not contain an action.");
        }
    }

    public static ToolbarItem action(EditorAction action) {
        return new ToolbarItem(ToolbarItemKind.ACTION, Optional.of(action));
    }

    public static ToolbarItem separator() {
        return new ToolbarItem(ToolbarItemKind.SEPARATOR, Optional.empty());
    }

    public static ToolbarItem blockStyle() {
        return new ToolbarItem(ToolbarItemKind.BLOCK_STYLE, Optional.empty());
    }

    public static ToolbarItem group() {
        return new ToolbarItem(ToolbarItemKind.GROUP, Optional.empty());
    }

    public static ToolbarItem semanticConvert() {
        return new ToolbarItem(ToolbarItemKind.SEMANTIC_CONVERT, Optional.empty());
    }
}
