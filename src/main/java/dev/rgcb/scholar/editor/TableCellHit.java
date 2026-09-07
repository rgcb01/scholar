package dev.rgcb.scholar.editor;

import java.util.Objects;

public record TableCellHit(TableCellCoordinate cell, int characterOffset) {
    public TableCellHit {
        cell = Objects.requireNonNull(cell, "cell");
        if (characterOffset < 0) {
            throw new IllegalArgumentException("characterOffset must not be negative.");
        }
    }

    public TableCellTextSelection caretSelection() {
        return TableCellTextSelection.caret(cell, characterOffset);
    }
}
