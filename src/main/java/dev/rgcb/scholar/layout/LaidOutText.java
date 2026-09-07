package dev.rgcb.scholar.layout;

import java.util.Objects;

public record LaidOutText(
        String text,
        TextStyle style,
        int x,
        int y,
        int width,
        int sourceBlockIndex,
        int sourceStart,
        int sourceEnd
) {
    public LaidOutText(String text, TextStyle style, int x, int y, int width) {
        this(text, style, x, y, width, -1, 0, 0);
    }

    public LaidOutText {
        text = Objects.requireNonNull(text, "text");
        style = Objects.requireNonNull(style, "style");
        if (sourceBlockIndex < -1) {
            throw new IllegalArgumentException("sourceBlockIndex must be -1 or greater.");
        }
        if (sourceStart < 0 || sourceEnd < sourceStart) {
            throw new IllegalArgumentException("source range must be non-negative and ordered.");
        }
    }
}
