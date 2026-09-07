package dev.rgcb.scholar.math.editor;

import java.util.Objects;

public record MathRange(MathPosition start, MathPosition end) {
    public MathRange {
        start = Objects.requireNonNull(start, "start");
        end = Objects.requireNonNull(end, "end");
        if (start.equals(end)) {
            throw new IllegalArgumentException("MathRange must not be collapsed.");
        }
    }
}
