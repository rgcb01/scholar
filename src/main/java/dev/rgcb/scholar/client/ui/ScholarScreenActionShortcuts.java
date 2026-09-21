package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.ActionShortcut;
import dev.rgcb.scholar.editor.EditorActionId;
import java.util.Objects;
import java.util.Optional;

/** Total shortcut policy for application-shell actions implemented by the client screen. */
public final class ScholarScreenActionShortcuts {
    private ScholarScreenActionShortcuts() {
    }

    public static Optional<ActionShortcut> forAction(EditorActionId id) {
        Objects.requireNonNull(id, "id");
        return switch (id) {
            case FILE_NEW -> Optional.of(ActionShortcut.ctrl(ActionShortcut.Key.N));
            case FILE_OPEN -> Optional.of(ActionShortcut.ctrl(ActionShortcut.Key.O));
            case FILE_SAVE -> Optional.of(ActionShortcut.ctrl(ActionShortcut.Key.S));
            case FILE_SAVE_AS -> Optional.of(ActionShortcut.of(
                    new ActionShortcut.Stroke(ActionShortcut.Key.S, true, true)));
            default -> Optional.empty();
        };
    }
}
