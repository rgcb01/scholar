package dev.rgcb.scholar.editor;

import java.util.Objects;

/** Dedicated internal selection for one DiagramBlock. */
public record DiagramEditingSelection(int blockIndex, DiagramEditTarget target) implements EditorSelection {
    public DiagramEditingSelection {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must not be negative.");
        }
        target = Objects.requireNonNull(target, "target");
    }
}
