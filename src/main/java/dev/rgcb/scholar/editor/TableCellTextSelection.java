package dev.rgcb.scholar.editor;

import java.util.Objects;

public record TableCellTextSelection(TableCellCoordinate cell, int anchorOffset, int activeOffset) {
    public TableCellTextSelection {
        cell = Objects.requireNonNull(cell, "cell");
        if (anchorOffset < 0) {
            throw new IllegalArgumentException("anchorOffset must not be negative.");
        }
        if (activeOffset < 0) {
            throw new IllegalArgumentException("activeOffset must not be negative.");
        }
    }

    public static TableCellTextSelection caret(TableCellCoordinate cell, int offset) {
        return new TableCellTextSelection(cell, offset, offset);
    }

    public boolean isCaret() {
        return anchorOffset == activeOffset;
    }

    public int startOffset() {
        return Math.min(anchorOffset, activeOffset);
    }

    public int endOffset() {
        return Math.max(anchorOffset, activeOffset);
    }
}
