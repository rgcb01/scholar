package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.ActionShortcut;
import dev.rgcb.scholar.editor.BuiltInEditorActionCatalog;
import dev.rgcb.scholar.editor.EditorActionId;
import java.util.Objects;
import java.util.Optional;

/** Total shortcut policy for application-shell actions implemented by the client screen. */
public final class ScholarScreenActionShortcuts {
    private ScholarScreenActionShortcuts() {
    }

    public static Optional<ActionShortcut> forAction(EditorActionId id) {
        Objects.requireNonNull(id, "id");
        return BuiltInEditorActionCatalog.require(id).shortcut();
    }
}
