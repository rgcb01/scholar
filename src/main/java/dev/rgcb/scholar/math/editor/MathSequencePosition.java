package dev.rgcb.scholar.math.editor;

import java.util.Objects;

public record MathSequencePosition(MathPath sequencePath, int childOffset) implements MathPosition {
    public MathSequencePosition {
        sequencePath = Objects.requireNonNull(sequencePath, "sequencePath");
        if (childOffset < 0) {
            throw new IllegalArgumentException("childOffset must not be negative.");
        }
    }
}
