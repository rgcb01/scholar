package dev.rgcb.scholar.editor;

public record FigureCaptionSelection(int blockIndex, int anchorOffset, int activeOffset) implements EditorSelection {
    public FigureCaptionSelection {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must not be negative.");
        }
        if (anchorOffset < 0 || activeOffset < 0) {
            throw new IllegalArgumentException("caption offsets must not be negative.");
        }
    }

    public static FigureCaptionSelection caret(int blockIndex, int offset) {
        return new FigureCaptionSelection(blockIndex, offset, offset);
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
