package dev.rgcb.scholar.client.ui;

import java.util.Optional;

/** Owns modal popup exclusivity without owning document mutation or field values. */
public final class EditorDialogCoordinator {
    public enum Dialog {
        SEMANTIC_TOKEN,
        CROSS_REFERENCE,
        PLOT_VALUE,
        DIAGRAM_LABEL,
        DIAGRAM_CANVAS,
        ELECTRICAL_COMPONENT
    }

    private Dialog active;

    public void open(Dialog dialog) {
        active = java.util.Objects.requireNonNull(dialog, "dialog");
    }

    public boolean close(Dialog dialog) {
        if (active != dialog) return false;
        active = null;
        return true;
    }

    public void closeAll() {
        active = null;
    }

    public boolean isOpen(Dialog dialog) {
        return active == dialog;
    }

    public boolean anyOpen() {
        return active != null;
    }

    public Optional<Dialog> active() {
        return Optional.ofNullable(active);
    }
}
