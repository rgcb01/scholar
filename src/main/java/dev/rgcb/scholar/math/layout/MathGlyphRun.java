package dev.rgcb.scholar.math.layout;

import java.util.Objects;

public record MathGlyphRun(
        String content,
        MathTextKind kind,
        int x,
        int baselineOffset,
        int ascent,
        double scale
) implements MathPrimitive {
    public MathGlyphRun(String content, MathTextKind kind, int x, int baselineOffset, int ascent) {
        this(content, kind, x, baselineOffset, ascent, 1.0);
    }

    public MathGlyphRun {
        content = Objects.requireNonNull(content, "content");
        kind = Objects.requireNonNull(kind, "kind");
        if (content.isEmpty()) {
            throw new IllegalArgumentException("content must not be empty.");
        }
        if (ascent < 0) {
            throw new IllegalArgumentException("ascent must not be negative.");
        }
        if (scale <= 0.0) {
            throw new IllegalArgumentException("scale must be positive.");
        }
    }
}
