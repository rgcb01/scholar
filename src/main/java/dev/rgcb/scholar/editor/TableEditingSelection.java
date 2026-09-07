package dev.rgcb.scholar.editor;

import java.util.Objects;

public record TableEditingSelection(int blockIndex, TableCellTextSelection selection) implements EditorSelection {
    public TableEditingSelection {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must not be negative.");
        }
        selection = Objects.requireNonNull(selection, "selection");
    }
}
