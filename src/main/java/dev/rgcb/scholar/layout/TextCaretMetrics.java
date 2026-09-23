package dev.rgcb.scholar.layout;

/** Visual caret bounds relative to the logical top of a laid-out text line. */
public record TextCaretMetrics(int topInset, int height) {
    public TextCaretMetrics {
        if (height < 1) throw new IllegalArgumentException("Invalid text caret metrics.");
    }
}
