package dev.rgcb.scholar.editor;

import java.util.Objects;

public record PlotEditingSelection(int blockIndex, PlotEditTarget target) implements EditorSelection {
    public PlotEditingSelection {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must not be negative.");
        }
        target = Objects.requireNonNull(target, "target");
    }
}
