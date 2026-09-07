package dev.rgcb.scholar.editor;

public record DocumentPosition(int blockIndex, int characterOffset) {
    public DocumentPosition {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must not be negative.");
        }
        if (characterOffset < 0) {
            throw new IllegalArgumentException("characterOffset must not be negative.");
        }
    }
}
