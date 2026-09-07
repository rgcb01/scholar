package dev.rgcb.scholar.editor;

public record BlockSelection(int blockIndex) implements EditorSelection {
    public BlockSelection {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must not be negative.");
        }
    }
}
