package dev.rgcb.scholar.editor;

public record DocumentInsertionPoint(int blockIndex) {
    public DocumentInsertionPoint {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must not be negative.");
        }
    }
}
