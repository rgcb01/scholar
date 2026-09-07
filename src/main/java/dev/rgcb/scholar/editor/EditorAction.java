package dev.rgcb.scholar.editor;

import java.util.Optional;

public interface EditorAction {
    EditorActionId id();

    String label();

    default String tooltip() {
        return label();
    }

    Optional<ActionShortcut> shortcut();

    boolean isEnabled(EditorActionContext context);

    default ActionSelectionState selectionState(EditorActionContext context) {
        return ActionSelectionState.NOT_APPLICABLE;
    }

    EditorActionResult execute(EditorActionContext context);
}
